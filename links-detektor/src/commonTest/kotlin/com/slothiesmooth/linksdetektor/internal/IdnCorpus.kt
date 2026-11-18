package com.slothiesmooth.linksdetektor.internal

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Shared IDN inputs. `EQUAL_CORPUS` maps an input host to the output `java.net.IDN.toASCII` produces for it.
 */
internal object IdnCorpus {
    private val HIGH_SURROGATE: Char = '\uD800'
    private val LOW_SURROGATE: Char = '\uDC00'

    val EQUAL_CORPUS: ImmutableList<Pair<String, String>> = persistentListOf(
        "example.com" to "example.com",
        "EXAMPLE.COM" to "EXAMPLE.COM",
        "B\u00DCCHER.DE" to "xn--bcher-kva.DE",
        "XN--BCHER-KVA.de" to "XN--BCHER-KVA.de",
        "b\u00FCcher.de" to "xn--bcher-kva.de",
        "\u4F8B\u3048.jp" to "xn--r8jz45g.jp",
        "\u4F8B\u3048\u3002jp" to "xn--r8jz45g.jp",
        "\u4F8B\u3048\uFF0Ejp" to "xn--r8jz45g.jp",
        "\u4F8B\u3048\uFF61jp" to "xn--r8jz45g.jp",
        "stra\u00DFe.de" to "strasse.de",
        "fa\u00DF.de" to "fass.de",
        "\u00DF" to "ss",
        "\u03C2.gr" to "xn--4xa.gr",
        "\u0391\u0392\u0393.gr" to "xn--mxacd.gr",
        "\uFF45\uFF58\uFF41\uFF4D\uFF50\uFF4C\uFF45.com" to "example.com",
        "\u2168.com" to "ix.com",
        "caf\u00E9.fr" to "xn--caf-dma.fr",
        "cafe\u0301.fr" to "xn--caf-dma.fr",
        "\u00E4\u00F6\u00FC.example" to "xn--4ca0bs.example",
        "\u0627\u0644\u0645\u063A\u0631\u0628.ma" to "xn--mgbc0a9azcg.ma",
        "\u0438\u0441\u043F\u044B\u0442\u0430\u043D\u0438\u0435.\u0440\u0444" to "xn--80akhbyknj4f.xn--p1ai",
        "\u0130stanbul.com" to "xn--istanbul-o0e.com",
        "I\u0307stanbul.com" to "xn--istanbul-o0e.com",
        "\u00AD\u00ADab.com" to "ab.com",
        "a\u200Db.com" to "ab.com",
        "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC.ir" to "xn--mgba3gch31f.ir",
        "\u30DF\u30AB\u30B5.jp" to "xn--lckua8j.jp",
        "\uFF76\uFF9E.jp" to "xn--mck.jp",
        "a\u0300\u0301.com" to "xn--0ca40i.com",
        "ex_ample.com" to "ex_ample.com",
        "-a-.com" to "-a-.com",
        "a b.com" to "a b.com",
        "ab--cd.com" to "ab--cd.com",
        "192.168.0.1" to "192.168.0.1",
        "[::1]" to "[::1]",
        "a." to "a.",
        "." to ".",
        "" to "",
        "\u00E9".repeat(30) + "\u4F8B".repeat(10) + ".com" to
            "xn--9caaaaaaaaaaaaaaaaaaaaaaaaaaaaaa44153daaaaaaaaa.com",
        "\u00FC".repeat(56) + ".com" to "xn--td" + "a".repeat(56) + ".com",
    )

    /** Hosts that both `PureIdn` and `java.net.IDN` reject with an `IllegalArgumentException`. */
    val REJECTS: ImmutableList<String> = persistentListOf(
        "a..b",
        ".a",
        "a".repeat(64) + ".com",
        "\u00FC".repeat(60) + ".com",
        // Lone surrogates are appended at run time: a compile-time string constant holding one loses it in klibs.
        "${HIGH_SURROGATE}.com",
        "a${LOW_SURROGATE}b.com",
        "\u00E9$HIGH_SURROGATE",
    )

    /** Inputs where `PureIdn` deliberately accepts what `java.net.IDN` rejects, with the `PureIdn` output. */
    val KNOWN_DIVERGENCES: ImmutableList<Pair<String, String>> = persistentListOf(
        "\uD83D\uDE00.com" to "xn--e28h.com",
        "\u1E9E.de" to "ss.de",
    )
}
