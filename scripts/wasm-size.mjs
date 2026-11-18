#!/usr/bin/env node
// Measures a directory's raw, gzip and brotli sizes. Usage: node scripts/wasm-size.mjs <dir> [--budget-mb N]
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative } from "node:path";
import { fileURLToPath } from "node:url";
import { brotliCompressSync, constants, gzipSync } from "node:zlib";

const MB = 1024 * 1024;
const DEFAULT_BUDGET_MB = 5;

function walk(dir) {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    return statSync(path).isDirectory() ? walk(path) : [path];
  });
}

export function measure(dir) {
  const files = walk(dir).sort().map((file) => {
    const bytes = readFileSync(file);
    return {
      path: relative(dir, file).split("\\").join("/"),
      raw: bytes.length,
      gzip: gzipSync(bytes, { level: 9 }).length,
      brotli: brotliCompressSync(bytes, {
        params: { [constants.BROTLI_PARAM_QUALITY]: 11 },
      }).length,
    };
  });
  const total = files.reduce(
    (sum, f) => ({ raw: sum.raw + f.raw, gzip: sum.gzip + f.gzip, brotli: sum.brotli + f.brotli }),
    { raw: 0, gzip: 0, brotli: 0 },
  );
  return { files, total };
}

function main(args) {
  const dir = args.find((arg) => !arg.startsWith("--"));
  const budgetIndex = args.indexOf("--budget-mb");
  const budgetMb = budgetIndex >= 0 ? Number(args[budgetIndex + 1]) : DEFAULT_BUDGET_MB;
  if (!dir || Number.isNaN(budgetMb)) {
    console.error("usage: node scripts/wasm-size.mjs <dir> [--budget-mb N]");
    return 2;
  }
  const { files, total } = measure(dir);
  const line = (name, s) =>
    `${name.padEnd(48)} raw ${String(s.raw).padStart(10)}  gzip ${String(s.gzip).padStart(10)}  brotli ${String(s.brotli).padStart(10)}`;
  files.forEach((f) => console.log(line(f.path, f)));
  console.log(line("TOTAL", total));
  const mb = (total.brotli / MB).toFixed(2);
  if (total.brotli > budgetMb * MB) {
    console.error(`brotli total ${mb} MB exceeds the ${budgetMb} MB budget`);
    return 1;
  }
  console.log(`brotli total ${mb} MB is within the ${budgetMb} MB budget`);
  return 0;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  process.exitCode = main(process.argv.slice(2));
}
