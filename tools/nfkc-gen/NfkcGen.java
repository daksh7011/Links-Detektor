import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates NfkcData.kt from the pinned Unicode 15.0.0 data files in ./ucd.
 *
 * Usage (from the repository root, JDK 17+, no Gradle):
 *   java tools/nfkc-gen/NfkcGen.java
 *
 * Integers are written as little-endian base-32 varints, one character per 5 bits plus a continuation bit,
 * using the base64 alphabet, so the tables are plain ASCII string constants.
 */
public final class NfkcGen {
    private static final String ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final int CHUNK = 6000;
    private static final int LINE = 96;

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "tools/nfkc-gen");
        Path out = Path.of(args.length > 1 ? args[1]
            : "links-detektor/src/commonMain/kotlin/com/slothiesmooth/linksdetektor/internal/nfkc/NfkcData.kt");

        TreeMap<Integer, Integer> ccc = new TreeMap<>();
        TreeMap<Integer, int[]> mapping = new TreeMap<>();
        TreeMap<Integer, int[]> canonical = new TreeMap<>();
        for (String line : Files.readAllLines(root.resolve("ucd/UnicodeData.txt"), StandardCharsets.UTF_8)) {
            if (line.isEmpty()) continue;
            String[] f = line.split(";", -1);
            int cp = Integer.parseInt(f[0], 16);
            int cls = Integer.parseInt(f[3]);
            if (cls != 0) ccc.put(cp, cls);
            String d = f[5];
            if (d.isEmpty()) continue;
            boolean compat = d.startsWith("<");
            if (compat) d = d.substring(d.indexOf('>') + 1).trim();
            String[] parts = d.split(" ");
            int[] cps = new int[parts.length];
            for (int i = 0; i < parts.length; i++) cps[i] = Integer.parseInt(parts[i], 16);
            mapping.put(cp, cps);
            if (!compat) canonical.put(cp, cps);
        }

        boolean[] excluded = new boolean[0x110000];
        for (String line : Files.readAllLines(root.resolve("ucd/DerivedNormalizationProps.txt"), StandardCharsets.UTF_8)) {
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash);
            String[] f = line.split(";");
            if (f.length < 2 || !f[1].trim().equals("Full_Composition_Exclusion")) continue;
            String range = f[0].trim();
            int dots = range.indexOf("..");
            int lo = Integer.parseInt(dots < 0 ? range : range.substring(0, dots), 16);
            int hi = dots < 0 ? lo : Integer.parseInt(range.substring(dots + 2), 16);
            for (int c = lo; c <= hi; c++) excluded[c] = true;
        }

        // Combining class ranges: (gap since previous end, length - 1, class).
        IntStream cccStream = new IntStream();
        int prevEnd = 0;
        int rangeCount = 0;
        Integer start = null;
        int last = -1;
        int cur = 0;
        List<int[]> ranges = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : ccc.entrySet()) {
            int cp = e.getKey();
            int cls = e.getValue();
            if (start != null && cp == last + 1 && cls == cur) {
                last = cp;
            } else {
                if (start != null) ranges.add(new int[] {start, last, cur});
                start = cp;
                last = cp;
                cur = cls;
            }
        }
        if (start != null) ranges.add(new int[] {start, last, cur});
        for (int[] r : ranges) {
            cccStream.add(r[0] - prevEnd);
            cccStream.add(r[1] - r[0]);
            cccStream.add(r[2]);
            prevEnd = r[1];
            rangeCount++;
        }

        // Full compatibility decomposition (recursively expanded): (gap since previous key, length, code points).
        IntStream decompStream = new IntStream();
        int prevKey = 0;
        for (Integer cp : mapping.keySet()) {
            List<Integer> expanded = new ArrayList<>();
            expand(cp, mapping, expanded);
            decompStream.add(cp - prevKey);
            decompStream.add(expanded.size());
            for (int c : expanded) decompStream.add(c);
            prevKey = cp;
        }

        // Primary composites: pairs from two-code-point canonical decompositions that are not excluded,
        // sorted by (first, second): (first gap, second, composite).
        TreeMap<Long, Integer> pairs = new TreeMap<>();
        for (Map.Entry<Integer, int[]> e : canonical.entrySet()) {
            int[] d = e.getValue();
            if (d.length != 2 || excluded[e.getKey()]) continue;
            pairs.put(((long) d[0] << 21) | d[1], e.getKey());
        }
        IntStream compStream = new IntStream();
        int prevFirst = 0;
        for (Map.Entry<Long, Integer> e : pairs.entrySet()) {
            int first = (int) (e.getKey() >> 21);
            int second = (int) (e.getKey() & 0x1FFFFF);
            compStream.add(first - prevFirst);
            compStream.add(second);
            compStream.add(e.getValue());
            prevFirst = first;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("// Generated by tools/nfkc-gen from Unicode 15.0.0 — do not edit.\n");
        sb.append("package com.slothiesmooth.linksdetektor.internal.nfkc\n\n");
        sb.append("/**\n * Unicode 15.0.0 normalization data. Every table is a sequence of little-endian base-32 varints written\n");
        sb.append(" * one character per 5 bits (plus a continuation bit) in the base64 alphabet, split into chunks.\n */\n");
        sb.append("internal object NfkcData {\n");
        emit(sb, "CCC_RANGES", "Combining classes as (gap since previous range end, length - 1, class) triples.",
            cccStream.encode());
        emit(sb, "DECOMPOSITIONS",
            "Full compatibility decompositions as (gap since previous key, length, code points) entries.",
            decompStream.encode());
        emit(sb, "COMPOSITIONS",
            "Canonical composition pairs as (first gap, second, composite) triples sorted by (first, second).",
            compStream.encode());
        sb.append("}\n");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        System.out.println("ccc ranges=" + rangeCount + " decompositions=" + mapping.size() + " pairs=" + pairs.size()
            + " bytes=" + sb.length());
    }

    private static void expand(int cp, TreeMap<Integer, int[]> mapping, List<Integer> out) {
        int[] m = mapping.get(cp);
        if (m == null) {
            out.add(cp);
            return;
        }
        for (int c : m) expand(c, mapping, out);
    }

    private static void emit(StringBuilder sb, String name, String doc, String data) {
        sb.append("\n    /** ").append(doc).append(" */\n");
        sb.append("    val ").append(name).append(": Array<String> = arrayOf(\n");
        for (int i = 0; i < data.length(); i += CHUNK) {
            String chunk = data.substring(i, Math.min(data.length(), i + CHUNK));
            for (int j = 0; j < chunk.length(); j += LINE) {
                String piece = chunk.substring(j, Math.min(chunk.length(), j + LINE));
                boolean first = j == 0;
                boolean lastPiece = j + LINE >= chunk.length();
                sb.append(first ? "        \"" : "            \"").append(piece).append('"');
                sb.append(lastPiece ? ",\n" : " +\n");
            }
        }
        sb.append("    )\n");
    }

    private static final class IntStream {
        private final StringBuilder sb = new StringBuilder();

        void add(int value) {
            int v = value;
            while (v >= 32) {
                sb.append(ALPHABET.charAt(32 | (v & 31)));
                v >>>= 5;
            }
            sb.append(ALPHABET.charAt(v));
        }

        String encode() {
            return sb.toString();
        }
    }
}
