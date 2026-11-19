# Links-Detektor Library KMP Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the JVM-only `:links-detektor` library into a pure Kotlin Multiplatform library (jvm, android, iOS, macOS, wasmJs, js, linux, mingw) and cut the first publishable `3.0.0-alpha1`.

**Architecture:** Behaviour is pinned first with golden tests captured from today's JVM implementation, then every JVM-only API is swapped for pure Kotlin while the module is still `kotlin("jvm")`. Only then does the build flip to KMP through a `build-logic` convention plugin (`ld.published.library`), with three tiny platform seams (`nfkc`, `idnToAscii`, `MalformedUrlException`). Publishing, docs and CI are rebuilt last.

**Tech Stack:** Kotlin 2.4.20, Gradle 9.9.0, AGP 9.3.3 (`com.android.kotlin.multiplatform.library`), kotlinx-collections-immutable 0.5.2, generated Unicode 15.0.0 NFKC tables (`TableNfkc`, linux/mingw), detekt 1.23.8, vanniktech maven-publish 0.37.0, Dokka 2.3.0, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-11-18-kmp-migration-design.md` (phases 1–5 only; "Owner decisions" and "House rule" sections win over everything above them).

## Global Constraints

- Coordinates stay `com.slothiesmooth:links-detektor`; package stays `com.slothiesmooth.linksdetektor`.
- Version line: `3.0.0-alpha1`, `-alpha2`, … → `-beta1` … → `-rc1` … → `3.0.0`. No dot before the number.
- Library JVM bytecode **17** (toolchain 21 is fine). Library Android `minSdk` **23**, `compileSdk` 37.
- Targets: `jvm`, `android`, `iosArm64`, `iosSimulatorArm64`, `macosArm64`, `wasmJs`, `js`, `linuxX64`, `linuxArm64`, `mingwX64`. Not `iosX64`, `macosX64`, tvOS, watchOS.
- Dependencies of the published library: `kotlinx-collections-immutable` as `api` everywhere. Nothing else (doist normalize was dropped 2026-11-19 — see spec).
- Immutable collections everywhere: no `List`/`Set`/`Map` (or mutable variants) in any signature, property or field; no `mutableListOf`/`arrayListOf`/`mutableSetOf`/`hashSetOf`/`mutableMapOf`/`hashMapOf`/`java.util.*` collections. Use `ImmutableList`/`PersistentList` etc. and `.builder()`→`.build()`. `StringBuilder` is allowed. Tests assert on immutable types too.
- No `!!` anywhere (repo guideline + detekt `UnsafeCallOnNullableType`); no new `@Suppress`.
- `explicitApi()` on the library; every public declaration keeps KDoc.
- JVM/Android keep `java.net.IDN` (no behaviour change for current users); all other targets use `PureIdn`.
- Commit messages: one subject line, at most 1–3 short body lines. **No `Co-Authored-By` trailer, no AI attribution.**
- PRs target `develop`. **Do not merge to `master` before Task 13** — `master.yml` publishes on push.
- Do not delete or gitignore `docs/superpowers/`.
- Gradle runs use JDK 21 (`java -version` → 21).

## Review Focus

1. **Stray `%` in the input** (`"see http://exa%mple.com/x ok"`): today the detector silently drops the `%`, so `originalUrl` is not a substring of the input. `Url.range` must still cover the whole source span (`4..<25`). Pinned in Task 3.
2. **IPv6 literal with more than 8 groups** (`"[1:2:3:4:5:6:7:8:9]"`): today throws `ArrayIndexOutOfBoundsException` on JVM and would silently corrupt on JS. Must return "not an IP" on every target. Pinned in Task 5.
3. **Turkish default locale on JVM** (`"TITLE.com"`): must normalise to `title.com`, not `tıtle…`. Pinned in Task 5.
4. **Hosts with code points unassigned in Unicode 3.2 (emoji, `ẞ`)**: JVM rejects (host `null`), `PureIdn` encodes (`xn--e28h.com`). Both behaviours pinned explicitly so the divergence is a decision, not an accident. Task 6.
5. **Pathological backtracking input**: today `detect()` throws the JVM-only `NegativeArraySizeException`; after the migration it throws `IllegalStateException` on every target with the same message. Pinned in Task 5.

## File Structure (end state)

```
build-logic/
  settings.gradle.kts  build.gradle.kts
  src/main/kotlin/ld.detekt.gradle.kts
  src/main/kotlin/ld.published.library.gradle.kts
links-detektor/
  build.gradle.kts                      (plugin + POM only)
  api/                                  (ABI dumps, committed)
  src/commonMain/kotlin/com/slothiesmooth/linksdetektor/
    LinksDetektor.kt  LinksDetektorOptions.kt  Url.kt  NormalizedUrl.kt  MalformedUrlException.kt (expect)
    internal/ (existing files) + IpFormatter.kt  PureIdn.kt  Punycode.kt  Platform.kt (expect nfkc, idnToAscii)
  src/jvmAndAndroidMain/…/  MalformedUrlException.jvmAndAndroid.kt  internal/Platform.jvmAndAndroid.kt
  src/nonJvmMain/…/         MalformedUrlException.nonJvm.kt  internal/Idn.nonJvm.kt
  src/appleMain/…/internal/Nfkc.apple.kt
  src/webMain/…/internal/Nfkc.web.kt
  src/nativeNonAppleMain/…/internal/Nfkc.nativeNonApple.kt
  src/commonTest/kotlin/…   (all existing + golden tests)
  src/jvmTest/kotlin/…      (IdnDifferentialTest, JvmIdnHostTest, TurkishLocaleTest)
```

Deleted: `sample/`, `links-detektor/src/main/resources/`, committed Dokka output under `docs/` (everything except `docs/superpowers/`).

---

### Task 1: Tests to kotlin.test, drop mockk and junit-params

**Files:**
- Modify: `links-detektor/src/test/kotlin/com/slothiesmooth/linksdetektor/LinksDetektorTest.kt`
- Modify: `links-detektor/src/test/kotlin/com/slothiesmooth/linksdetektor/internal/CharExtensionsTest.kt`
- Modify: `links-detektor/src/test/kotlin/com/slothiesmooth/linksdetektor/internal/UrlUtilTest.kt`
- Modify: `links-detektor/build.gradle.kts:47-51`, `gradle/libs.versions.toml:16-18,31-35`, `.junie/guidelines.md` (the "JUnit and Mockk" line → "kotlin.test")

**Interfaces:**
- Produces: three test classes that import only `kotlin.test.*` (no `org.junit`, `io.mockk`, `java.*`), ready to move to `commonTest` unchanged.

- [ ] **Step 1: Swap test dependencies.** In `links-detektor/build.gradle.kts` replace the four test lines with `testImplementation(kotlin("test"))` (keep `useJUnitPlatform()`). Remove `junit`, `mockk` versions and `junit-api`, `junit-engine`, `junit-params`, `mockk` libraries from the catalog.

- [ ] **Step 2: Convert the three files.** Rules:
  - Imports become `kotlin.test.Test`, `assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull`, `assertFailsWith`.
  - Each `@ParameterizedTest` becomes one `@Test` looping over a `persistentListOf(...)` table, asserting with a message: `assertEquals(expected, actual, "case: $input")` / `assertTrue(c.isHex(), "case: $c")`. `@CsvSource` rows become `Pair`s; `@NullAndEmptySource` adds `null` and `""` to the table; `provideOptionCombinations` becomes a private `val` table of `Triple<LinksDetektorOptions, String, ImmutableList<String>>`.
  - Delete `detect calls appropriate internal methods` (`LinksDetektorTest.kt:876-893`, vacuous mockk test).
  - `LinksDetektorTest.kt:1102-1104`: replace `System.currentTimeMillis()` with `kotlin.time.TimeSource.Monotonic.markNow()` / `elapsedNow()`.
  - `LinksDetektorTest.kt:1134,1159,1163`: keep `java.net.MalformedURLException` for now (Task 9 retargets it); it is the only `java.*` import allowed to remain.
  - `UrlUtilTest.kt:332`: replace `String.format("%%%02X", i)` with `"%" + i.toString(16).uppercase().padStart(2, '0')`.
  - Test names stay as they are.

- [ ] **Step 3: Run.** `./gradlew :links-detektor:test`
  Expected: BUILD SUCCESSFUL, **172 tests**, 0 failed (139 plain + 34 collapsed parameterized − 1 deleted).
  `grep -rn -E "org\.junit|io\.mockk|java\.util" links-detektor/src/test` → no output.

- [ ] **Step 4: Commit.** `git commit -m "Move tests to kotlin.test, drop mockk and junit-params"`

---

### Task 2: Golden tests from the current JVM implementation

All values below were captured by running the unmodified code at `314f638` on JDK 21. These are characterization tests: they must pass **immediately**; a failure means the test was transcribed wrongly, not that the code is wrong.

**Files:**
- Create: `links-detektor/src/test/kotlin/com/slothiesmooth/linksdetektor/internal/HostNormalizerGoldenTest.kt`
- Create: `links-detektor/src/test/kotlin/com/slothiesmooth/linksdetektor/internal/PathNormalizerGoldenTest.kt`
- Create: `links-detektor/src/test/kotlin/com/slothiesmooth/linksdetektor/internal/UrlUtilEncodeGoldenTest.kt`
- Create: `links-detektor/src/test/kotlin/com/slothiesmooth/linksdetektor/UrlCreateTest.kt`
- Create: `links-detektor/src/test/kotlin/com/slothiesmooth/linksdetektor/NormalizeGoldenTest.kt`
- Create: `links-detektor/src/test/kotlin/com/slothiesmooth/linksdetektor/internal/JvmIdnHostTest.kt` (JVM-only expectations; moves to `jvmTest` in Task 8)

**Interfaces:**
- Produces: `internal fun ByteArray?.hex(): String?` test helper (lowercase, 2 digits per byte, `null` for null) in `HostNormalizerGoldenTest.kt`, reused by `NormalizeGoldenTest`.

Write every non-ASCII character as a `\uXXXX` escape exactly as shown.

- [ ] **Step 1: `HostNormalizerGoldenTest`.** One `@Test fun hostGoldens()` over a table of `(input: String?, normalizedHost: String?, bytesHex: String?)`, asserting both `HostNormalizer(input).normalizedHost` and `.bytes.hex()` with `"case: $input"`. `M` below = `00000000000000000000ffff` (IPv4-mapped prefix).

  | input | normalizedHost | bytes |
  |---|---|---|
  | `null` | null | null |
  | `""` | null | null |
  | `example.com` | `example.com` | null |
  | `EXAMPLE.Com` | `example.com` | null |
  | `www.Example.COM` | `www.example.com` | null |
  | `TITLE.com` | `title.com` | null |
  | `bücher.de` | `xn--bcher-kva.de` | null |
  | `BÜCHER.de` | `xn--bcher-kva.de` | null |
  | `例え.jp` | `xn--r8jz45g.jp` | null |
  | `例え。jp` | `xn--r8jz45g.jp` | null |
  | `例え．jp` | `xn--r8jz45g.jp` | null |
  | `例え｡jp` | `xn--r8jz45g.jp` | null |
  | `straße.de` | `strasse.de` | null |
  | `xn--bcher-kva.de` | `xn--bcher-kva.de` | null |
  | `αβγ.gr` | `xn--mxacd.gr` | null |
  | `ｅｘａｍｐｌｅ.com` | `example.com` | null |
  | `Ⅸ.com` | `ix.com` | null |
  | `İstanbul.com` | `xn--istanbul-o0e.com` | null |
  | `café.fr` | `xn--caf-dma.fr` | null |
  | `café.fr` | `xn--caf-dma.fr` | null |
  | `äöü.example` | `xn--4ca0bs.example` | null |
  | `المغرب.icom.museum` | `xn--mgbc0a9azcg.icom.museum` | null |
  | `"a".repeat(63) + ".com"` | same as input | null |
  | `"a".repeat(64) + ".com"` | null | null |
  | `"ü".repeat(60) + ".com"` | null | null |
  | `192.168.0.1` | `192.168.0.1` | `M c0a80001` |
  | `127.0.0.1` | `127.0.0.1` | `M 7f000001` |
  | `0.0.0.0` | `0.0.0.0` | `M 00000000` |
  | `255.255.255.255` | `255.255.255.255` | `M ffffffff` |
  | `0xC0.0xA8.0x00.0x01` | `192.168.0.1` | `M c0a80001` |
  | `0xc0.0250.0.1` | `192.168.0.1` | `M c0a80001` |
  | `0300.0250.0.01` | `192.168.0.1` | `M c0a80001` |
  | `3232235521` | `192.168.0.1` | `M c0a80001` |
  | `0xC0A80001` | `192.168.0.1` | `M c0a80001` |
  | `030052000001` | `192.168.0.1` | `M c0a80001` |
  | `4294967295` | `255.255.255.255` | `M ffffffff` |
  | `0` | `0.0.0.0` | `M 00000000` |
  | `1` | `0.0.0.1` | `M 00000001` |
  | `0x` | `0.0.0.0` | `M 00000000` |
  | `00` | `0.0.0.0` | `M 00000000` |
  | `192.168.0%2e1` | `192.168.0.1` | `M c0a80001` |
  | `%31%39%32.168.0.1` | `192.168.0.1` | `M c0a80001` |
  | `256.1.1.1` | `256.1.1.1` | null |
  | `1.2.3` | `1.2.3` | null |
  | `1.2.3.4.5` | `1.2.3.4.5` | null |
  | `192.0x00A80001` | `192.0x00a80001` | null |
  | `4294967296` | `4294967296` | null |
  | `1.2.3.08` | `1.2.3.08` | null |
  | `1.2.3.-4` | `1.2.3.-4` | null |
  | `192.168.0.1.` | `192.168.0.1` | null |
  | `[2001:db8:0:0:0:0:0:1]` | `[2001:db8:0:0:0:0:0:1]` | `20010db8000000000000000000000001` |
  | `[2001:0db8:85a3:0000:0000:8a2e:0370:7334]` | `[2001:db8:85a3:0:0:8a2e:370:7334]` | `20010db885a3000000008a2e03707334` |
  | `[2001:db8::1]` | `[2001:db8:0:0:0:0:0:1]` | `20010db8000000000000000000000001` |
  | `[::1]` | `[0:0:0:0:0:0:0:1]` | `00000000000000000000000000000001` |
  | `[::]` | `[0:0:0:0:0:0:0:0]` | 32 zeros |
  | `[1::]` | `[1:0:0:0:0:0:0:0]` | `00010000000000000000000000000000` |
  | `[2001:DB8::ABCD]` | `[2001:db8:0:0:0:0:0:abcd]` | `20010db800000000000000000000abcd` |
  | `[1:2:3:4:5:6:7:8]` | `[1:2:3:4:5:6:7:8]` | `00010002000300040005000600070008` |
  | `[::ffff:192.168.0.1]` | `192.168.0.1` | `M c0a80001` |
  | `[::ffff:c0a8:1]` | `192.168.0.1` | `M c0a80001` |
  | `[0:0:0:0:0:ffff:c0a8:1]` | `192.168.0.1` | `M c0a80001` |
  | `[::192.168.0.1]` | `[0:0:0:0:0:0:c0a8:1]` | `000000000000000000000000c0a80001` |
  | `[64:ff9b::192.0.2.33]` | `[64:ff9b:0:0:0:0:c000:221]` | `0064ff9b0000000000000000c0000221` |
  | `[1::2::3]` | `[1:0:0:0:0:0:0:3]` | `00010000000000000000000000000003` |
  | `[fe80::1%25eth0]` | `[fe80::1%25eth0]` | null |
  | `[fe80::1%eth0]` | `[fe80::1%25eth0]` | null |
  | `[1:2]` | `[1:2]` | null |
  | `[::g]` | `[::g]` | null |
  | `[12345::1]` | `[12345::1]` | null |
  | `[]` | `[]` | null |
  | `exa%6Dple.com` | `example.com` | null |
  | `ex%41mple.com` | `exAmple.com` | null |
  | `example%2Ecom` | `example.com` | null |
  | `exam%20ple.com` | `exam%20ple.com` | null |
  | `exa%25mple.com` | `exa%25mple.com` | null |
  | `exa%mple.com` | `exa%25mple.com` | null |
  | `exa#mple.com` | `exa%23mple.com` | null |
  | `ex ample.com` | `ex%20ample.com` | null |
  | `exa\u0001mple.com` | `exa%01mple.com` | null |
  | `example.com.` | `example.com` | null |
  | `.` | `""` | null |
  | `.example.com` | null | null |
  | `..example..com..` | null | null |
  | `a...b` | null | null |
  | `...` | null | null |
  | `localhost` | `localhost` | null |
  | `ex_ample.com` | `ex_ample.com` | null |
  | `-example-.com` | `-example-.com` | null |

- [ ] **Step 2: `JvmIdnHostTest`** (expectations that hold only where `java.net.IDN` is used): `HostNormalizer("😀.com").normalizedHost` is `null`; `HostNormalizer("ẞ.de").normalizedHost` is `null`.

- [ ] **Step 3: `PathNormalizerGoldenTest`.** One test over `(input: String?, expected: String?)` calling `PathNormalizer().normalizePath(input)`:

  `null→null`, `""→""`, `/→/`, `/a/b/c→/a/b/c`, `/a/./b→/a/b`, `/a/../b→/b`, `/a/b/..→/a/`, `/a/b/../→/a/`, `/a/b/.→/a/b/`, `/a/b/./→/a/b/`, `/..→/`, `/../→/`, `/../../a→/a`, `/.→/`, `/./→/`, `//→/`, `///→/`, `/a//b→/a/b`, `/a///b//→/a/b/`, `/a/b/../../c→/c`, `/a/b/../../../c→/c`, `/a/%2e/b→/a/b`, `/a/%2E%2E/b→/b`, `/a/%2e%2e→/`, `/%2e%2e/%2e%2e/a→/a`, `/a%2fb→/a/b`, `/a/b%2f..%2fc→/a/c`, `/a%2Fb/../c→/a/c`, `"/a b"→/a%20b`, `/a%20b→/a%20b`, `/a%2520b→/a%20b`, `/a%252520b→/a%20b`, `/a%25%32%30b→/a%20b`, `/%→/%25`, `/%2→/%252`, `/%zz→/%25zz`, `/a%→/a%25`, `/100%/x→/100%25/x`, `/a#b→/a%23b`, `/a%23b→/a%23b`, `/café→/caf%E9`, `/例→/%8B`, `/😀→/%3D%00`, `/aĀb→/a%00b`, `"/a\tb"→/a%09b`, `/a%0Ab→/a%0Ab`, `/a~b_c-d.e→` same, `/a/.../b→` same, `/a/..b/c→` same, `/a/.b/c→` same, `/a/b../c→` same, `a/b→a/b`, `a/../b→/b`, `..→..`, `.→.`, `/a/b/c/../../..→/`, `/a/b/c/../../../→/`, `/a/./././b→/a/b`, `/./a→/a`, `/../a/./b/../c//d/→/a/c/d/`, `/%7Euser→/~user`, `/a?b→/a?b`, `/%41%42%43→/ABC`.

- [ ] **Step 4: `UrlUtilEncodeGoldenTest`** (pins the low-byte truncation quirk): `"a b"→a%20b`, `café→caf%E9`, `Ā→%00`, `例→%8B`, `😀→%3D%00`, `#%→%23%25`, `\u007f→%7F`, `~→~`, `\u0000\u001f→%00%1F`.

- [ ] **Step 5: `UrlCreateTest`** (use `assertFailsWith<java.net.MalformedURLException>` for now):
  - `noUrlThrows`: for each of `""`, `"   "`, `"localhost"`, `"hello"`, `"http://"`, `"://"` → message `"We couldn't find any urls in string: $input"`.
  - `moreThanOneUrlThrows`: `"www.a.com,www.b.com"` → message `"We found more than one url in string: www.a.com,www.b.com"`.
  - `createParsesParts`: `"http://user:pass@host.com:99/p?q=1#f"` → scheme `http`, username `user`, password `pass`, host `host.com`, port `99`, path `/p`, query `?q=1`, fragment `#f`, `fullUrl` equal to input.
  - `createSanitisesWhitespace`: `"  http://example.com/a b  "` → `originalUrl == "http://example.com/a%20b"`; `"http://example.com/\tpath"` → `originalUrl == "http://example.com/path"`.
  - `createDefaults`: `"example.com"` → scheme `http`, port `80`, path `/`, `fullUrl == "http://example.com/"`; `"//cdn.example.com/x.js"` → scheme `""`, port `-1`, `fullUrl == "//cdn.example.com/x.js"`.
  - `spacesDoNotSplit`: `"http://example.com http://example.org"` does **not** throw; host is `example.com%20http`.

- [ ] **Step 6: `NormalizeGoldenTest`.** Table `(input, host, path, bytesHex, fullUrl)` through `Url.create(input).normalize()`:

  | input | host | path | bytes | fullUrl |
  |---|---|---|---|---|
  | `http://EXAMPLE.com/a/../b/./c//d?x=1#Frag` | `example.com` | `/b/c/d` | null | `http://example.com/b/c/d?x=1#Frag` |
  | `http://bücher.de/straße` | `xn--bcher-kva.de` | `/stra%DFe` | null | `http://xn--bcher-kva.de/stra%DFe` |
  | `http://3232235521/` | `192.168.0.1` | `/` | `M c0a80001` | `http://192.168.0.1/` |
  | `http://0xC0.0250.0.1:8080/a%2e%2e/b` | `192.168.0.1` | `/a../b` | `M c0a80001` | `http://192.168.0.1:8080/a../b` |
  | `http://[2001:db8::1]:8080/a` | `[2001:db8:0:0:0:0:0:1]` | `/a` | `20010db8000000000000000000000001` | `http://[2001:db8:0:0:0:0:0:1]:8080/a` |
  | `http://[::ffff:192.168.0.1]/` | `192.168.0.1` | `/` | `M c0a80001` | `http://192.168.0.1/` |
  | `https://user:pw@Example.COM:443/%7Efoo/` | `example.com` | `/~foo/` | null | `https://user:pw@example.com/~foo/` |
  | `http://www.example.com./a%20b` | `www.example.com` | `/a%20b` | null | `http://www.example.com/a%20b` |
  | `http://例え。jp/例` | `xn--r8jz45g.jp` | `/%8B` | null | `http://xn--r8jz45g.jp/%8B` |
  | `ftp://Example.com:21/x` | `example.com` | `/x` | null | `ftp://example.com/x` |
  | `http://example.com:80` | `example.com` | `/` | null | `http://example.com/` |
  | `http://exa%6Dple.com/%41` | `example.com` | `/A` | null | `http://example.com/A` |
  | `//Example.com/a/..` | `example.com` | `/` | null | `//example.com/` |
  | `http://example.com/a/b/../../../c` | `example.com` | `/c` | null | `http://example.com/c` |

- [ ] **Step 7: Run.** `./gradlew :links-detektor:test` → BUILD SUCCESSFUL, all pass with zero production changes (`git status` shows only new test files).

- [ ] **Step 8: Commit.** `git commit -m "Pin normalisation and Url.create behaviour with golden tests"`

---

### Task 3: `Url.range` (additive)

**Decision — type and semantics.** `val range: IntRange` on `Url`. `range.first` is the index in the scanned string of the first character the URL was read from (inclusive); `range.last + 1` is the index just past the last one (exclusive end). It is built as `start until endExclusive`.
Why `IntRange` over a value class: it is the stdlib's own convention for "where in the string" (`MatchResult.range` has identical semantics), it works directly with `String.substring(range)` / `slice`, and it adds no new class to the public ABI. Consumers needing an exclusive end write `range.last + 1` (do not use `IntRange.endExclusive`, it is deprecated).
Guarantees: ranges of one `detect()` call are ascending and non-overlapping; `content.substring(url.range)` equals `originalUrl` (after mapping `\n`,`\t`,`\r` to space) **except** when the detector dropped a stray `%`, where the range covers the whole source span including the dropped `%`. For `Url.create(s)` the range is relative to the sanitised string that was scanned (trimmed, spaces → `%20`, whitespace removed), not to `s`. `NormalizedUrl.range` equals the range of the `Url` it came from.

**Files:**
- Modify: `links-detektor/src/main/kotlin/com/slothiesmooth/linksdetektor/internal/UrlMarker.kt:14` (add field)
- Modify: `links-detektor/src/main/kotlin/com/slothiesmooth/linksdetektor/Url.kt:37` (add property + KDoc)
- Modify: `links-detektor/src/main/kotlin/com/slothiesmooth/linksdetektor/LinksDetektor.kt:37,633-648`
- Test: `links-detektor/src/test/kotlin/com/slothiesmooth/linksdetektor/UrlRangeTest.kt`

**Interfaces:**
- Produces: `Url.range: IntRange` (public); `UrlMarker.range: IntRange` (internal `var`, default `IntRange.EMPTY`).

- [ ] **Step 1: Write the failing tests** in `UrlRangeTest`. Helper: `fun ranges(s: String, o: LinksDetektorOptions) = LinksDetektor(s, o).detect().map { it.range }`.

  | input | options | expected ranges |
  |---|---|---|
  | `http://example.com` | Default | `0 until 18` |
  | `Visit http://example.com and https://example.org for more` | Default | `6 until 24`, `29 until 48` |
  | `"x http://a.com\nhttp://b.com/p\ty"` | Default | `2 until 14`, `15 until 29` |
  | `<a href="http://a.com/b">http://c.com</a>` | HTML | `9 until 23`, `25 until 37` |
  | `{"u":"http://a.com/x"}` | JSON | `6 until 20` |
  | `"http://a.com" 'http://b.com'` (quotes are part of the input) | Default | `8 until 13`, `23 until 28` |
  | `a.com a.com a.com` | Default | `0 until 5`, `6 until 11`, `12 until 17` |
  | `go to example.com.` | Default | `6 until 18` |
  | `http://a.com:hello.world` | Default | `0 until 12`, `13 until 24` |
  | `hello:world.com/x` | ALLOW_SINGLE_LEVEL_DOMAIN | `0 until 5`, `6 until 17` |
  | `http://a.com:80a b.com:` | Default | `0 until 15`, `17 until 23` |
  | `x.com""` | Default | `0 until 5` |
  | `x.co.. y` | Default | `0 until 5` |
  | `http://[::1]] x` | Default | `0 until 12` |
  | `ｅｘ。com 例え。jp/x` | Default | `0 until 6`, `7 until 14` |
  | `%%a.com` | Default | `2 until 7` |
  | `see http://exa%mple.com/x ok` | Default | `4 until 25` (originalUrl is `http://example.com/x`) |
  | `ab%zz.com rest` | Default | `0 until 9` (originalUrl is `abzz.com`) |
  | `a.com a%.com` | Default | `0 until 5`, `6 until 12` |

  Plus:
  - `substringMatchesOriginalUrl`: for the rows without a stray `%`, `input.substring(url.range)` with `\n\t\r`→space equals `url.originalUrl`.
  - `rangesAreAscendingAndDisjoint`: for every row and every `LinksDetektorOptions` entry, each `range.first >= previous.last + 1` and no range is empty.
  - `normalizeKeepsRange`: `url.normalize().range == url.range`.
  - `createRangeIsRelativeToSanitisedInput`: `Url.create("  http://example.com/a b  ").range == (0 until 24)`.

- [ ] **Step 2: Run to verify failure.** `./gradlew :links-detektor:test --tests '*UrlRangeTest*'` → compilation error, unresolved reference `range`.

- [ ] **Step 3: Implement.** `LinksDetektor` keeps the constructor argument (`private val content: String`). In `readEnd`, immediately before `currentUrlMarker.createUrl()`, set `currentUrlMarker.range = locate(originalUrl)`. `Url.range` reads `urlMarker.range`. The algorithm is not determined by the tests, so use exactly this (validated against 1,858 detections with zero misplacements):

  ```kotlin
  // The buffer ends at most one consumed-but-unappended character before the reader position.
  // The only characters the detector drops from inside a URL are stray '%'.
  private fun locate(original: String): IntRange {
      val position = minOf(inputTextReader.position, content.length)
      for (trailing in 0..MAX_TRAILING) {           // MAX_TRAILING = 3
          val end = position - trailing
          if (end <= 0) break
          var i = end - 1
          var j = original.length - 1
          if (asRead(content[i]) != original[j]) continue
          while (j >= 0 && i >= 0) {
              if (asRead(content[i]) == original[j]) j-- else if (content[i] != '%') break
              i--
          }
          if (j < 0) return (i + 1) until end
      }
      return IntRange.EMPTY
  }
  ```
  `asRead(c)` maps `\n`, `\t`, `\r` to `' '` (same rule as `InputTextReader.read()`; reuse `isWhiteSpace()`).

- [ ] **Step 4: Run.** `./gradlew :links-detektor:test` → all pass (Task 2 goldens unchanged).

- [ ] **Step 5: Commit.** `git commit -m "Add Url.range with the source span of each detected URL"`

---

### Task 4: Immutable collections and detekt bans

**Files:**
- Modify: `links-detektor/build.gradle.kts` (add `api(libs.kotlinx.collections.immutable)`), `gradle/libs.versions.toml`
- Modify: `LinksDetektor.kt:77,82,110-113,282-333,672-685`, `Url.kt:249-252,334-340,356`, `internal/CharExtensions.kt:104-134`, `internal/DomainNameReader.kt:490`, `internal/HostNormalizer.kt:112,178`
- Modify: `detekt.yml:535-540`
- Modify: all test files that declare `List`/`listOf` locals

**Interfaces:**
- Produces: `fun LinksDetektor.detect(): ImmutableList<Url>`; `internal fun String.splitByDot(): ImmutableList<String>`; `LinksDetektor(content: String, options: ImmutableSet<LinksDetektorOptions>)` primary constructor (Step 8); `internal value class OptionMask`.

- [ ] **Step 1: Write the failing test** in `LinksDetektorTest`:
  ```kotlin
  @Test
  fun `detect returns an immutable list`() {
      val urls: ImmutableList<Url> = LinksDetektor("http://a.com b.org", LinksDetektorOptions.Default).detect()
      assertEquals(2, urls.size)
  }
  ```
  and in `CharExtensionsTest`: `val parts: ImmutableList<String> = "a.b".splitByDot(); assertEquals(persistentListOf("a", "b"), parts)`.

- [ ] **Step 2: Run to verify failure.** `./gradlew :links-detektor:test` → compilation error (unresolved `ImmutableList` / type mismatch).

- [ ] **Step 3: Convert.** Catalog: `kotlinx-collections-immutable = "0.5.2"`, library `org.jetbrains.kotlinx:kotlinx-collections-immutable`.
  - `urlList`: `private val urlList = persistentListOf<Url>().builder()`; `detect()` returns `urlList.build()`.
  - `characterMatchHashMap`: `persistentHashMapOf<Char, Int>().builder()`.
  - `VALID_SCHEMES`: `persistentSetOf("http://", "https://", "ftp://", "ftps://", "http%3a//", "https%3a//", "ftp%3a//", "ftps%3a//")`.
  - `Url.SCHEME_PORT_MAP`: `private val SCHEME_PORT_MAP: ImmutableMap<String, Int> = persistentMapOf("http" to 80, "https" to 443, "ftp" to 21)`; replace `SCHEME_PORT_MAP[scheme]!!` with a single lookup `?: -1`.
  - `Url.populateUsernamePassword`: drop `.toTypedArray()`, use `.split(':').dropLastWhile { it.isEmpty() }.toImmutableList()`.
  - `splitByDot`: builder → `.build()`; empty input returns `persistentListOf()`.
  - Every `val x: List<…>` local in main becomes `ImmutableList<…>` (wrap `String.split` results with `.toImmutableList()`).
  - Tests: `listOf(` → `persistentListOf(`, `List<` → `ImmutableList<`, `emptyList()` → `persistentListOf()`.

- [ ] **Step 4: Turn on the detekt bans** in `detekt.yml`:
  ```yaml
  ForbiddenImport:
    active: true
    excludes: ['**/test/**', '**/*Test/**']
    imports:
      - value: 'java.util.*'
        reason: 'Use kotlinx.collections.immutable; the library is multiplatform.'
  ForbiddenMethodCall:
    active: true
    methods:
      - value: 'kotlin.collections.mutableListOf'
      - value: 'kotlin.collections.arrayListOf'
      - value: 'kotlin.collections.mutableSetOf'
      - value: 'kotlin.collections.hashSetOf'
      - value: 'kotlin.collections.linkedSetOf'
      - value: 'kotlin.collections.mutableMapOf'
      - value: 'kotlin.collections.hashMapOf'
      - value: 'kotlin.collections.linkedMapOf'
  ```
  (each with `reason: 'Immutable collections only (house rule).'`). `java.util.*` is still imported until Task 5 by `UrlUtil.kt:5`, `DomainNameReader.kt:12`, `PathNormalizer.kt:4` (`Stack`) and by `LinksDetektor.kt:15`, `HostNormalizer.kt:10` (`Locale`), so add those five files to a temporary `excludes` entry here and remove it in Task 5.

- [ ] **Step 5: Verify the ban actually fires (spike).** `ForbiddenMethodCall` needs type resolution, which the plain `detekt` task does not have. Temporarily add `val x = mutableListOf<Int>()` to `UrlPart.kt`, run `./gradlew :links-detektor:detektMain`.
  Expected: FAILED with `ForbiddenMethodCall`. Remove the line.
  Fallback if `detektMain` does not flag it (or cannot read the compiled classes): leave the YAML in place and rely on the regex guard `bannedCollections` that Task 7 adds to `ld.detekt`; note the outcome in the commit body.

- [ ] **Step 6: Run.** `./gradlew :links-detektor:test :links-detektor:detekt` → BUILD SUCCESSFUL.
  `grep -rn -E "mutableListOf|HashMap|HashSet|ArrayList|: List<|: Set<|: Map<" links-detektor/src` → no output.

- [ ] **Step 7: Commit.** `git commit -m "Use immutable collections; detect() returns ImmutableList<Url>"`

- [ ] **Step 8: Option-set constructor (owner decision, 2026-11-18).** Callers can combine any options, not only the predefined combined constants.
  - Public API: `LinksDetektor(content: String, options: ImmutableSet<LinksDetektorOptions>)` becomes the primary constructor. The existing `LinksDetektor(content: String, options: LinksDetektorOptions)` stays as a secondary constructor delegating with `persistentSetOf(options)` (source and binary compatible).
  - Internals: add `internal value class OptionMask(val value: Int)` in `internal/OptionMask.kt` with `fun has(flag: LinksDetektorOptions): Boolean = value and flag.value == flag.value` and `companion fun of(options: ImmutableSet<LinksDetektorOptions>): OptionMask` (bitwise OR; empty set → `Default.value`). `LinksDetektor` and `DomainNameReader` hold an `OptionMask` and every `linksDetektorOptions.hasFlag(X)` / `_options.hasFlag(X)` becomes `mask.has(X)`.
  - Failing tests first, in `LinksDetektorOptionSetTest`:
    - `quote plus single quote set matches both` — `LinksDetektor("\"http://a.com\" 'http://b.com'", persistentSetOf(QUOTE_MATCH, SINGLE_QUOTE_MATCH)).detect().map { it.originalUrl }` equals what the predefined combined constant yields for the same input (capture with the old constructor in the test).
    - `single-level-domain combines with bracket match` — a set with `ALLOW_SINGLE_LEVEL_DOMAIN` + `BRACKET_MATCH` detects `(http://localhost)` as `http://localhost` (no constant has this exact value).
    - `single option constructor equals singleton set` — for every `LinksDetektorOptions` entry and every golden input from Task 2, `LinksDetektor(s, o).detect()` equals `LinksDetektor(s, persistentSetOf(o)).detect()` compared by `originalUrl` and `range`.
    - `empty set behaves like Default`.
  - Run `./gradlew :links-detektor:test` → BUILD SUCCESSFUL, all goldens unchanged.
  - Commit: `git commit -m "Accept a set of options in LinksDetektor"`
  - Produces (for the showcase plan): `LinksDetektor(content: String, options: ImmutableSet<LinksDetektorOptions>)`.

---

### Task 5: Pure-Kotlin swaps (everything except IDN) and dead-dependency removal

**Files:**
- Create: `links-detektor/src/main/kotlin/com/slothiesmooth/linksdetektor/internal/IpFormatter.kt`
- Modify: `internal/HostNormalizer.kt`, `internal/PathNormalizer.kt`, `internal/UrlUtil.kt`, `internal/CharExtensions.kt`, `internal/InputTextReader.kt:80,120,164`, `internal/DomainNameReader.kt:12,221,416,419,611`, `LinksDetektor.kt:14-15,179,243-245,251,268,401,465,583,639,650`, `Url.kt` (all `StringUtils` and `!!` sites)
- Modify: `links-detektor/build.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `detekt.yml` (drop Task 4's temporary excludes)
- Delete: `links-detektor/src/main/resources/logback.groovy`, `logbackCompiler.groovy`
- Test: `internal/IpFormatterTest.kt`, `internal/InputTextReaderTest.kt`, `internal/TurkishLocaleTest.kt` (JVM-only, moves to `jvmTest` in Task 8); extend `HostNormalizerGoldenTest`

**Interfaces:**
- Produces: `internal fun formatIp(bytes: ByteArray): String` (16-byte input).

- [ ] **Step 1: Write the failing tests.**
  - `IpFormatterTest` (helper `fun bytes(hex: String): ByteArray`):
    - `00000000000000000000ffffc0a80001` → `192.168.0.1`
    - `00000000000000000000ffffffffffff` → `255.255.255.255`
    - `20010db8000000000000000000000001` → `[2001:db8:0:0:0:0:0:1]`
    - `20010db885a3000000008a2e03707334` → `[2001:db8:85a3:0:0:8a2e:370:7334]`
    - 32 zeros → `[0:0:0:0:0:0:0:0]`
    - `000000000000000000000000c0a80001` → `[0:0:0:0:0:0:c0a8:1]` (IPv4-compatible is **not** unwrapped)
    - `0000000000000000000100ffc0a80001` → `[0:0:0:0:1:ff:c0a8:1]` (prefix not exactly mapped)
  - `HostNormalizerGoldenTest`, new row: `[1:2:3:4:5:6:7:8:9]` → normalizedHost `[1:2:3:4:5:6:7:8:9]`, bytes null (today: throws).
  - `InputTextReaderTest`:
    - `peekCharBeyondEndThrows`: `assertFailsWith<IndexOutOfBoundsException> { InputTextReader("ab").peekChar(3) }`
    - `excessiveBacktrackThrowsIllegalState`: on `InputTextReader("ab")`, call `read()` once then `goBack()`/`read()` in a loop; the 21st `goBack()` throws `IllegalStateException` whose message starts with `"Backtracked max amount of characters. Endless loop detected. Bad Text: '"`.
  - `TurkishLocaleTest`: set `java.util.Locale.setDefault(Locale.forLanguageTag("tr-TR"))`, assert `HostNormalizer("TITLE.com").normalizedHost == "title.com"` and `LinksDetektor("HTTP://TITLE.COM/I", Default).detect().single().normalize().host == "title.com"`; restore the previous default in `finally`.

- [ ] **Step 2: Run to verify failure.** `./gradlew :links-detektor:test` → unresolved `formatIp`; after stubbing it, the 9-group row, both `InputTextReaderTest` cases and `TurkishLocaleTest` fail.

- [ ] **Step 3: Implement `formatIp`.** Must match `InetAddress.getByAddress(bytes).hostAddress` wrapped in `[]` for IPv6:
  ```kotlin
  internal fun formatIp(bytes: ByteArray): String {
      val mapped = (0..9).all { bytes[it].toInt() == 0 } && bytes[10] == 0xff.toByte() && bytes[11] == 0xff.toByte()
      if (mapped) return (12..15).joinToString(".") { (bytes[it].toInt() and 0xff).toString() }
      return (0 until 8).joinToString(":", "[", "]") {
          (((bytes[it * 2].toInt() and 0xff) shl 8) or (bytes[it * 2 + 1].toInt() and 0xff)).toString(16)
      }
  }
  ```
  Rules: no `::` compression, no leading zeros, lowercase hex, only the exact `::ffff:a.b.c.d` form collapses to dotted quad.

- [ ] **Step 4: Apply the swap table.** After this step `grep -rn -E "^import (java|org\.apache)\." links-detektor/src/main` must print only `HostNormalizer.kt: import java.net.IDN` and the two `java.net.MalformedURLException` imports.

  | Today | Replacement |
  |---|---|
  | `InetAddress.getByAddress` / `Inet6Address` / `UnknownHostException` (`HostNormalizer.kt:61-72`) | `host = formatIp(bytes)` |
  | `System.arraycopy(src, a, dst, b, n)` (`HostNormalizer.kt:203,215,218`) | `src.copyInto(dst, b, a, a + n)` |
  | 9-group overflow (`HostNormalizer.kt:198-216`) | return `null` when a section would be written past index 15 |
  | `StringUtils.isEmpty(x)` / `defaultString(x)` | `x.isNullOrEmpty()` / `x.orEmpty()` |
  | `java.util.Stack<Int>` (`PathNormalizer.kt:32`, `UrlUtil.kt:24`) | `persistentListOf<Int>().builder()`; push = `add`, pop = `removeAt(lastIndex)`, peek = `last()` |
  | `StringBuilder(path)` with nullable arg (`PathNormalizer.kt:31`) | `StringBuilder(path.orEmpty())` |
  | `.lowercase(Locale.getDefault())` (4 sites) | `.lowercase()` |
  | `String.format("%%%02X", c.code.toByte())` (`UrlUtil.kt:84`) | `"%" + (c.code and 0xFF).toString(16).uppercase().padStart(2, '0')` |
  | `sb.delete(a, b)` (11 sites) | `sb.deleteRange(a, b)` |
  | `sb.replace(0, len, s)` (`DomainNameReader.kt:221`) | `sb.setRange(0, len, s)` |
  | `Math.max(a, b)` (`InputTextReader.kt:120`) | `maxOf(a, b)` |
  | `throw ArrayIndexOutOfBoundsException()` (`InputTextReader.kt:80`) | `throw IndexOutOfBoundsException()` |
  | `throw NegativeArraySizeException(msg)` (`InputTextReader.kt:164`) | `throw IllegalStateException(msg)`; update the three `@throws` KDoc lines and add `@throws IllegalStateException` to `LinksDetektor.detect()` KDoc |
  | `":".toRegex()` splits | `split(':')` |
  | `@Suppress("NAME_SHADOWING")` (`LinksDetektor.kt:243`), shadowing in `PathNormalizer.kt:13`, `InputTextReader.kt:154` | rename the local instead |
  | `!!` (8 sites in `Url.kt`) | `?.let`, `.orEmpty()`, early `return`; `originalUrl` stays `String?` |

  Keep `@Suppress("HttpUrlsUsage")` only if the IDE inspection still needs it; otherwise delete.

- [ ] **Step 5: Remove dead build pieces.** `links-detektor/build.gradle.kts`: drop the shadow plugin, `libs.kotlin.stdlib`, groovy, jansi, logback, logback-groovy, logging, commonsMath3, commonsLang3. Root `build.gradle.kts`: drop shadow and git-hooks. Catalog: drop the matching versions/libraries. `git rm -r links-detektor/src/main/resources`.

- [ ] **Step 6: Run.** `./gradlew :links-detektor:test :links-detektor:detekt` → BUILD SUCCESSFUL; all Task 2 goldens still pass untouched.

- [ ] **Step 7: Commit.** `git commit -m "Replace JVM-only APIs with pure Kotlin and drop unused dependencies"` with body `Backtrack guard now throws IllegalStateException; host lowercasing is locale-independent.`

---

### Task 6: IDN seam, `PureIdn`, differential test

**Files:**
- Create: `internal/Platform.kt` (seam functions, plain `internal fun` for now), `internal/Punycode.kt`, `internal/PureIdn.kt`
- Modify: `internal/HostNormalizer.kt:50-56`
- Test: `internal/PunycodeTest.kt`, `internal/PureIdnTest.kt`, `internal/IdnDifferentialTest.kt` (JVM-only, moves to `jvmTest` in Task 8)

**Interfaces:**
- Produces:
  - `internal fun nfkc(input: String): String`
  - `internal fun idnToAscii(host: String): String` — throws `IllegalArgumentException` when the host cannot be converted
  - `internal object Punycode { fun encode(label: String): String }` — output without the `xn--` prefix; throws `IllegalArgumentException` on overflow
  - `internal object PureIdn { fun toAscii(host: String): String }` — throws `IllegalArgumentException`
  In this task `nfkc` = `java.text.Normalizer.normalize(input, Normalizer.Form.NFKC)` and `idnToAscii` = `java.net.IDN.toASCII(host)`; Task 9 turns both into `expect`.

- [ ] **Step 1: Write the failing tests.**
  - `PunycodeTest`: `bücher→bcher-kva`, `例え→r8jz45g`, `αβγ→mxacd`, `café→caf-dma`, `münchen→mnchen-3ya`, `한국→3e0b707e`, `ไทย→o3cw4h`, `испытание→80akhbyknj4f`, `рф→p1ai`, `😀→e28h` (surrogate pair is one code point), `üüü→tdaaa`.
  - `PureIdnTest.matchesJavaIdn` — table `EQUAL_CORPUS` (input → expected; these are `java.net.IDN.toASCII` outputs):

    | input | expected |
    |---|---|
    | `example.com` | `example.com` |
    | `EXAMPLE.COM` | `EXAMPLE.COM` (ASCII labels are returned verbatim) |
    | `BÜCHER.DE` | `xn--bcher-kva.DE` |
    | `XN--BCHER-KVA.de` | `XN--BCHER-KVA.de` |
    | `bücher.de` | `xn--bcher-kva.de` |
    | `例え.jp` / `例え。jp` / `例え．jp` / `例え｡jp` | `xn--r8jz45g.jp` |
    | `straße.de` | `strasse.de` |
    | `faß.de` | `fass.de` |
    | `ß` | `ss` |
    | `ς.gr` | `xn--4xa.gr` |
    | `ΑΒΓ.gr` | `xn--mxacd.gr` |
    | `ｅｘａｍｐｌｅ.com` | `example.com` |
    | `Ⅸ.com` | `ix.com` |
    | `café.fr` | `xn--caf-dma.fr` |
    | `äöü.example` | `xn--4ca0bs.example` |
    | `المغرب.ma` | `xn--mgbc0a9azcg.ma` |
    | `испытание.рф` | `xn--80akhbyknj4f.xn--p1ai` |
    | `İstanbul.com` / `İstanbul.com` | `xn--istanbul-o0e.com` |
    | `­­ab.com` | `ab.com` |
    | `a‍b.com` | `ab.com` |
    | `نامه‌ای.ir` | `xn--mgba3gch31f.ir` |
    | `ミカサ.jp` | `xn--lckua8j.jp` |
    | `ｶﾞ.jp` | `xn--mck.jp` |
    | `à́.com` | `xn--0ca40i.com` |
    | `"é".repeat(30) + "例".repeat(10) + ".com"` | `xn--9caaaaaaaaaaaaaaaaaaaaaaaaaaaaaa44153daaaaaaaaa.com` |
    | `"ü".repeat(56) + ".com"` | `"xn--td" + "a".repeat(56) + ".com"` |
    | `ex_ample.com`, `-a-.com`, `a b.com`, `ab--cd.com`, `192.168.0.1`, `[::1]`, `a.`, `.`, `""` | unchanged |

  - `PureIdnTest.rejects` — each throws `IllegalArgumentException`: `a..b`, `.a`, `"a".repeat(64) + ".com"`, `"ü".repeat(60) + ".com"`.
  - `PureIdnTest.knownDivergenceFromJavaIdn` — `PureIdn.toAscii("😀.com") == "xn--e28h.com"`, `PureIdn.toAscii("ẞ.de") == "ss.de"`.
  - `IdnDifferentialTest` (JVM): for every `EQUAL_CORPUS` and `rejects` input, `runCatching { PureIdn.toAscii(it) }` and `runCatching { java.net.IDN.toASCII(it) }` either both succeed with equal strings or both fail with `IllegalArgumentException`; and for the two divergent inputs `java.net.IDN.toASCII` throws. Share the corpus via an `internal object IdnCorpus` in the test sources.

- [ ] **Step 2: Run to verify failure.** `./gradlew :links-detektor:test --tests '*Idn*' --tests '*Punycode*'` → unresolved references.

- [ ] **Step 3: Implement `Punycode.encode`** per RFC 3492 §6.3 over **code points** (combine surrogate pairs first). Parameters: `base 36, tMin 1, tMax 26, skew 38, damp 700, initialBias 72, initialN 128`, delimiter `-` emitted only if at least one basic code point was copied. Digit map: `0..25 → 'a'..'z'`, `26..35 → '0'..'9'`. Bias adaptation: `delta = if (first) delta / damp else delta / 2; delta += delta / numPoints; k = 0; while (delta > ((base - tMin) * tMax) / 2) { delta /= base - tMin; k += base }; bias = k + (base - tMin + 1) * delta / (delta + skew)`.

- [ ] **Step 4: Implement `PureIdn.toAscii`.** The tests do not determine the order of operations, so:
  1. Split the host on `.`, `。`, `．`, `｡`; join results with `.`.
  2. An empty label is legal only as the last label (and for a wholly empty host); otherwise throw.
  3. A label whose chars are all `< 0x80` is returned verbatim (case preserved); if longer than 63 → throw.
  4. Otherwise: remove the Nameprep "map to nothing" set (`U+00AD, U+034F, U+1806, U+180B..U+180D, U+200B..U+200D, U+2060, U+FE00..U+FE0F, U+FEFF`); `lowercase()`; replace `ß`→`ss` and `ς`→`σ`; `nfkc()`; `lowercase()` again.
  5. If the mapped label is all ASCII return it; if it starts with `xn--` throw; else `"xn--" + Punycode.encode(label)`; if the result is longer than 63 → throw.
  Documented divergence from `java.net.IDN` (put it in the `PureIdn` KDoc): no Nameprep prohibited/bidi/unassigned checks.

- [ ] **Step 5: Route `HostNormalizer` through the seam.** Replace `IDN.toASCII(this@HostNormalizer.host)` with `idnToAscii(…)`; keep the `catch (IllegalArgumentException)` → return. `HostNormalizer.kt` must no longer import `java.*`.

- [ ] **Step 6: Run.** `./gradlew :links-detektor:test :links-detektor:detekt` → BUILD SUCCESSFUL.
  `grep -rn "^import java\." links-detektor/src/main` → only `internal/Platform.kt` (2 imports) and the `MalformedURLException` imports in `Url.kt`, `NormalizedUrl.kt`.

- [ ] **Step 7: Commit.** `git commit -m "Add pure-Kotlin IDN behind an idnToAscii seam"`

---

### Task 7: Toolchain bump, catalog, build-logic with `ld.detekt`, delete `:sample`

The module stays `kotlin("jvm")` in this task; only the tooling moves.

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties:3`, `settings.gradle.kts`, `gradle.properties`, `build.gradle.kts`, `gradle/libs.versions.toml`, `links-detektor/build.gradle.kts`, `.github/labeler.yml` (drop the `sample` label block; `library` glob → `links-detektor/src/**/*`)
- Create: `build-logic/settings.gradle.kts`, `build-logic/build.gradle.kts`, `build-logic/src/main/kotlin/ld.detekt.gradle.kts`
- Delete: `sample/`

**Interfaces:**
- Produces: root task `detektCheck`; root task `bannedCollections`; catalog aliases used by later tasks: versions `kotlin`, `agp`, `detekt`, `dokka`, `vanniktech`, `kotlinx-collections-immutable`, `doist-normalize`, `android-compileSdk` (`37`), `android-minSdk` (`23`), `lib` (`3.0.0-alpha1`); libraries `kotlinx-collections-immutable`, `doist-normalize`, `detekt-formatting`, `kotlin-test`.

- [ ] **Step 1: Wrapper.** `./gradlew wrapper --gradle-version 9.9.0` (run twice so the jar and scripts regenerate). Expected: `distributionUrl=…gradle-9.9.0-bin.zip`.

- [ ] **Step 2: Catalog.** Rewrite `gradle/libs.versions.toml` to exactly the aliases above with: `kotlin = "2.4.20"`, `agp = "9.3.3"`, `detekt = "1.23.8"`, `dokka = "2.3.0"`, `vanniktech = "0.37.0"`, `kotlinx-collections-immutable = "0.5.2"`, `doist-normalize = "1.2.0"`, `lib = "3.0.0-alpha1"`. Rename the old `detekt` library alias to `detekt-formatting`. Plugins block: `kotlin-jvm`, `kotlin-multiplatform`, `android-kotlin-multiplatform-library`, `detekt`, `dokka`, `vanniktech-maven-publish`.

- [ ] **Step 3: Settings and properties.** `settings.gradle.kts` as `/Users/slothie/IdeaProjects/hr-system-kmp/settings.gradle.kts` (`pluginManagement { includeBuild("build-logic"); google(); mavenCentral(); gradlePluginPortal() }`, foojay resolver `1.0.0`, `dependencyResolutionManagement` with `google()` + `mavenCentral()`, `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`), `rootProject.name = "links-detektor"`, `include(":links-detektor")`. `gradle.properties`: copy hr's file verbatim. `git rm -r sample`.

- [ ] **Step 4: build-logic.** `settings.gradle.kts` and `build.gradle.kts` as in hr's `build-logic/`, with dependencies: AGP, `kotlin-gradle-plugin`, `detekt-gradle-plugin`, `com.vanniktech:gradle-maven-publish-plugin:${libs.versions.vanniktech.get()}`, `org.jetbrains.dokka:dokka-gradle-plugin:${libs.versions.dokka.get()}` (no Compose, serialization or Metro).
  `ld.detekt.gradle.kts`: port `/Users/slothie/IdeaProjects/hr-system-kmp/build-logic/src/main/kotlin/hr.detekt.gradle.kts` — keep `base`, the `detektCheck` task and its excludes; **drop** `bannedTerms`; add:
  ```kotlin
  val bannedCollections by tasks.registering {
      description = "Fails when production sources use mutable or java.util collections."
      group = "verification"
      val sources = fileTree(rootDir) { include("**/src/*Main/**/*.kt", "**/src/main/**/*.kt"); exclude("**/build/**", "build-logic/**") }
      inputs.files(sources)
      doLast {
          val banned = Regex("""\b(mutableListOf|arrayListOf|mutableSetOf|hashSetOf|linkedSetOf|mutableMapOf|hashMapOf|linkedMapOf|ArrayList|HashSet|HashMap|LinkedHashSet|LinkedHashMap)\s*[<(]|^import java\.util\.""", RegexOption.MULTILINE)
          val hits = sources.files.filter { banned.containsMatchIn(it.readText()) }
          check(hits.isEmpty()) { "Mutable collections are banned (house rule): $hits" }
      }
  }
  detektCheck { dependsOn(bannedCollections) }
  ```
- [ ] **Step 5: Root and module build files.** Root `build.gradle.kts`: `plugins { alias(libs.plugins.kotlin.jvm) apply false; alias(libs.plugins.kotlin.multiplatform) apply false; alias(libs.plugins.android.kotlin.multiplatform.library) apply false; id("ld.detekt") }` and nothing else. Module: `plugins { alias(libs.plugins.kotlin.jvm); alias(libs.plugins.dokka); alias(libs.plugins.vanniktech.maven.publish) }`; remove the module `detekt {}` block and the detekt plugin, `repositories {}`, the `signing {}` block and `signing` plugin, `copyDokkaToDocsDir`, the `build.finalizedBy`, and the whole `tasks.dokkaHtml {}` block; replace the last with a minimal `dokka { moduleName.set("Links Detektor") }`. Keep `mavenPublishing {}` as is. Keep `jvmToolchain(21)`.

- [ ] **Step 6: Spike — vanniktech 0.37.0 and Dokka 2.3.0 on Gradle 9.9.0 / Kotlin 2.4.20.** Run `./gradlew :links-detektor:publishToMavenLocal :links-detektor:dokkaGenerate`.
  Expected: BUILD SUCCESSFUL; `~/.m2/repository/com/slothiesmooth/links-detektor/3.0.0-alpha1/` contains jar, sources jar, javadoc jar, pom; `links-detektor/build/dokka/html/index.html` exists.
  Fallbacks, in order: (a) pick the newest plugin release that configures cleanly (check `https://repo1.maven.org/maven2/com/vanniktech/gradle-maven-publish-plugin/maven-metadata.xml` and the Dokka equivalent) and record it in the catalog; (b) if Dokka cannot run, set `configure(KotlinJvm(javadocJar = JavadocJar.Empty(), sourcesJar = true))` and leave Dokka to Task 12; (c) if vanniktech cannot run at all, stop and report — do not hand-roll publishing.

- [ ] **Step 7: Keep CI truthful.** In `.github/workflows/ci.yml:45-49` and `develop.yml:34-38` replace the two Gradle steps with one: `./gradlew detektCheck :links-detektor:build`.

- [ ] **Step 8: Run.** `./gradlew detektCheck :links-detektor:test` → BUILD SUCCESSFUL, same test count as after Task 6. Then prove the guard: add `val x = HashMap<String, Int>()` to `UrlPart.kt`, `./gradlew detektCheck` → FAILED "Mutable collections are banned"; remove it.

- [ ] **Step 9: Commit.** `git commit -m "Move to Gradle 9.9, Kotlin 2.4.20 and build-logic; remove sample"`

---

### Task 8: Flip to KMP (jvm only) with `ld.published.library`

**Files:**
- Create: `build-logic/src/main/kotlin/ld.published.library.gradle.kts`
- Modify: `links-detektor/build.gradle.kts`
- Move: `links-detektor/src/main/kotlin/**` → `src/commonMain/kotlin/**`, except `internal/Platform.kt`, `Url.kt`'s exception usage (see below)
- Move: `links-detektor/src/test/kotlin/**` → `src/commonTest/kotlin/**`, except `JvmIdnHostTest.kt`, `TurkishLocaleTest.kt`, `IdnDifferentialTest.kt` → `src/jvmTest/kotlin/**`
- Modify: `.github/workflows/ci.yml`, `develop.yml` (command), `.github/labeler.yml`

**Interfaces:**
- Produces: convention plugin id `ld.published.library` (jvm target, JVM 17 bytecode, kotlin.test wiring, `explicitApi` **not yet**); tasks `:links-detektor:jvmTest`, `:links-detektor:jvmJar`.

- [ ] **Step 1: Write `ld.published.library`** (jvm-only for now). Based on `/Users/slothie/IdeaProjects/Nyx/build-logic/src/main/kotlin/nyx.kmp.library.gradle.kts`, without Android/iOS/wasm yet:
  - plugins: `org.jetbrains.kotlin.multiplatform`
  - `kotlin { jvm { testRuns.named("test") { executionTask.configure { useJUnitPlatform() } } }; applyDefaultHierarchyTemplate(); jvmToolchain(21) }`
  - `tasks.withType<KotlinJvmCompile>().configureEach { compilerOptions { jvmTarget.set(JvmTarget.JVM_17); freeCompilerArgs.add("-Xjdk-release=17") } }`
  - `commonMain` → `api(kotlinx-collections-immutable)`; `commonTest` → `implementation(kotlin("test"))`; `jvmTest` → `kotlin-test-junit5` + `junit-platform-launcher` exactly as hr's `hr.kmp.library` does (add `junit-bom = "5.14.4"` and the three libraries to the catalog).
  - Do **not** copy Nyx's `base { archivesName }` override: this module is published, and its name `links-detektor` is already unique.

- [ ] **Step 2: Module build file.** `plugins { id("ld.published.library"); alias(libs.plugins.dokka); alias(libs.plugins.vanniktech.maven.publish) }`; keep `group`, `version`, `mavenPublishing {}`; remove `dependencies {}`, `java {}`, `kotlin { jvmToolchain }`, `tasks.withType<Jar>`, `tasks.withType<Test>`.

- [ ] **Step 3: Move sources** with `git mv` as listed under Files. Temporarily everything that still imports `java.*` (`internal/Platform.kt`, and `Url.kt` + `NormalizedUrl.kt` because of `java.net.MalformedURLException`) cannot compile in `commonMain` with more than one target, but with jvm as the only target `commonMain` still sees the JDK — so they compile today. Leave them in `commonMain`; Task 9 fixes them before Task 10 adds targets.

- [ ] **Step 4: Run.** `./gradlew detektCheck :links-detektor:jvmTest` → BUILD SUCCESSFUL, same test count as Task 7.

- [ ] **Step 5: Verify bytecode level.** `./gradlew :links-detektor:jvmJar && javap -v -cp links-detektor/build/libs/links-detektor-jvm-3.0.0-alpha1.jar com.slothiesmooth.linksdetektor.Url | grep "major version"`
  Expected: `major version: 61`.

- [ ] **Step 6: CI + labeler.** Workflows run `./gradlew detektCheck :links-detektor:jvmTest`; the "Upload JARs" path stays `links-detektor/build/libs/*.jar`.

- [ ] **Step 7: Commit.** `git commit -m "Build the library with Kotlin Multiplatform (jvm target)"`

---

### Task 9: Platform seams as `expect`, `MalformedUrlException`, `HostNormalizer` internal, `explicitApi`

Still jvm-only, so every step is verifiable with `jvmTest`.

**Files:**
- Create: `src/commonMain/…/MalformedUrlException.kt`, `src/commonMain/…/internal/Platform.kt` (now `expect`)
- Create: `src/jvmAndAndroidMain/…/MalformedUrlException.jvmAndAndroid.kt`, `src/jvmAndAndroidMain/…/internal/Platform.jvmAndAndroid.kt`
- Modify: `Url.kt:7,353-361`, `NormalizedUrl.kt:6,96`, `internal/HostNormalizer.kt:26`, every public declaration (explicit `public`), `build-logic/…/ld.published.library.gradle.kts`
- Modify tests: `UrlCreateTest.kt`, `LinksDetektorTest.kt` (drop the last `java.net` import)
- Test: `src/jvmTest/…/MalformedUrlExceptionJvmTest.kt`

**Interfaces:**
- Produces:
  - `public expect open class MalformedUrlException(message: String) : Exception`
  - JVM/Android: `public actual typealias MalformedUrlException = java.net.MalformedURLException`
  - `internal expect fun nfkc(input: String): String`, `internal expect fun idnToAscii(host: String): String`
  - `@Throws(MalformedUrlException::class) public fun Url.Companion.create(url: String): Url`
  - `internal class HostNormalizer`
  - source set `jvmAndAndroidMain` (today: parent of `jvmMain` only)

- [ ] **Step 1: Write the failing tests.**
  - `UrlCreateTest`: change every `assertFailsWith<java.net.MalformedURLException>` to `assertFailsWith<MalformedUrlException>` (unresolved → fails to compile).
  - `MalformedUrlExceptionJvmTest`:
    ```kotlin
    @Test
    fun `existing JVM catch sites still work`() {
        val thrown = assertFailsWith<java.net.MalformedURLException> { Url.create("hello") }
        assertTrue(thrown is java.io.IOException)
        assertEquals("We couldn't find any urls in string: hello", thrown.message)
    }
    ```

- [ ] **Step 2: Run to verify failure.** `./gradlew :links-detektor:jvmTest` → unresolved reference `MalformedUrlException`.

- [ ] **Step 3: Add the source-set hierarchy** to `ld.published.library` (replaces the bare `applyDefaultHierarchyTemplate()`):
  ```kotlin
  applyDefaultHierarchyTemplate {
      common {
          group("jvmAndAndroid") { withJvm(); withAndroidTarget() }
          group("nonJvm") { group("native"); group("web") }
          group("native") { group("nativeNonApple") { withLinux(); withMingw() } }
      }
  }
  ```
  Verify: `./gradlew :links-detektor:jvmTest` configures without a "source set … not found" or "unused source set" error. The `web` group (js + wasmJs) exists in the default template since Kotlin 2.2.20; if 2.4.20 reports it unknown, declare it explicitly as `group("web") { withJs(); withWasmJs() }`. If `withAndroidTarget()` does not bind the `com.android.kotlin.multiplatform.library` target (checked in Task 10 Step 3), fall back to duplicating the two `jvmAndAndroid` files into `jvmMain` and `androidMain`.

- [ ] **Step 4: Implement the seams.** Common `expect` declarations as in Interfaces. `Platform.jvmAndAndroid.kt`: `actual fun nfkc` = `java.text.Normalizer` NFKC; `actual fun idnToAscii` = `java.net.IDN.toASCII(host)`. `Url.create` and `NormalizedUrl.create` throw and declare `MalformedUrlException`; messages unchanged.

- [ ] **Step 5: `HostNormalizer` → `internal class`; turn on `explicitApi()`** in the convention plugin. Add `public` to: `LinksDetektor` (class, constructor, `backtracked`, `detect`), `LinksDetektorOptions` (+ `value`, `hasFlag`), `Url` (class and all public members incl. `range`, `Companion.create`), `NormalizedUrl` (class, `host`, `path`, `hostBytes`). `Url.rawHost`/`rawPath` stay `protected`. No signature changes.

- [ ] **Step 6: Run.** `./gradlew detektCheck :links-detektor:jvmTest` → BUILD SUCCESSFUL.
  `grep -rn "^import java\." links-detektor/src/commonMain` → no output.

- [ ] **Step 7: Commit.** `git commit -m "Add MalformedUrlException and platform seams; make HostNormalizer internal"`

---

### Task 10: All targets and their actuals

**Files:**
- Modify: `build-logic/src/main/kotlin/ld.published.library.gradle.kts`
- Create: `src/nonJvmMain/…/MalformedUrlException.nonJvm.kt`, `src/nonJvmMain/…/internal/Idn.nonJvm.kt`
- Create: `src/appleMain/…/internal/Nfkc.apple.kt`, `src/webMain/…/internal/Nfkc.web.kt`, `src/nativeNonAppleMain/…/internal/Nfkc.nativeNonApple.kt`
- Test: `src/commonTest/…/internal/NfkcTest.kt`

**Interfaces:**
- Consumes: `expect` declarations from Task 9; `PureIdn.toAscii` from Task 6.
- Produces: tasks `jvmTest`, `testAndroidHostTest`, `jsNodeTest`, `wasmJsNodeTest`, `iosSimulatorArm64Test`, `macosArm64Test`, `linuxX64Test`, `allTests`.

- [ ] **Step 1: Write the failing test** `NfkcTest` (common, runs everywhere):
  `nfkc("ｅｘ") == "ex"`, `nfkc("Ⅸ") == "IX"`, `nfkc("café") == "café"`, `nfkc("ｶﾞ") == "ガ"`, `nfkc("ﬁ") == "fi"`, `nfkc("abc") == "abc"`, `nfkc("") == ""`.

- [ ] **Step 2: Spike — `com.doist.x:normalize` 1.2.0 against Kotlin 2.4.20.** Add only `linuxX64()` plus the `nativeNonAppleMain` dependency `implementation(libs.findLibrary("doist-normalize").get())` and the actual `import doist.x.normalize.Form; import doist.x.normalize.normalize; internal actual fun nfkc(input: String): String = input.normalize(Form.NFKC)`, plus the two `nonJvm` actuals from Step 4. Run `./gradlew :links-detektor:compileKotlinLinuxX64`.
  Expected: BUILD SUCCESSFUL.
  Fallbacks: (a) klib ABI/version error → try `1.4.0` (latest on Maven Central; confirm it still publishes `normalize-linuxx64`, `-linuxarm64`, `-mingwx64`); (b) still failing → stop and report; the owner-approved fallback is generated NFKC tables, which is its own plan.
  Confirm scoping: `./gradlew :links-detektor:dependencies --configuration jvmRuntimeClasspath | grep -c doist` → `0`.

- [ ] **Step 3: Add the remaining targets** to the convention plugin (apply `com.android.kotlin.multiplatform.library`; `@file:OptIn(ExperimentalWasmDsl::class)`):
  ```kotlin
  android {
      namespace = "com.slothiesmooth.linksdetektor"
      compileSdk = 37   // from catalog android-compileSdk
      minSdk = 23       // from catalog android-minSdk
      withHostTestBuilder {}.configure {}
  }
  iosArm64(); iosSimulatorArm64(); macosArm64()
  linuxX64(); linuxArm64(); mingwX64()
  js(IR) { nodejs(); browser { testTask { enabled = false } } }
  wasmJs { nodejs(); browser { testTask { enabled = false } } }
  ```
  No `androidResources`, no device-test builder. Check `./gradlew :links-detektor:compileAndroidMain` (or the name `tasks --all | grep -i compile.*Android` reports) resolves `java.net.IDN` — that proves `jvmAndAndroid` is wired; otherwise apply the Task 9 Step 3 fallback. If Gradle reports "Inconsistent JVM-target compatibility" for Android, set the Android Java compile options to 17 in the same block.

- [ ] **Step 4: Write the actuals.**
  - `MalformedUrlException.nonJvm.kt`: `public actual open class MalformedUrlException actual constructor(message: String) : Exception(message)`
  - `Idn.nonJvm.kt`: `internal actual fun idnToAscii(host: String): String = PureIdn.toAscii(host)`
  - `Nfkc.apple.kt`: `(input as NSString).precomposedStringWithCompatibilityMapping` (`platform.Foundation`; the cast needs `@Suppress("CAST_NEVER_SUCCEEDS")` — the one permitted suppression, with a comment)
  - `Nfkc.web.kt`: `private fun jsNfkc(s: String): String = js("s.normalize('NFKC')")`; `internal actual fun nfkc(input: String): String = jsNfkc(input)`. If `js(...)` is rejected in the shared `webMain`, duplicate the file into `jsMain` and `wasmJsMain`.
  - `Nfkc.nativeNonApple.kt`: from Step 2.

- [ ] **Step 5: Run on this Mac.** `./gradlew :links-detektor:allTests`
  Expected: BUILD SUCCESSFUL; `jvmTest`, `testAndroidHostTest`, `jsNodeTest`, `wasmJsNodeTest`, `iosSimulatorArm64Test`, `macosArm64Test` all executed with the same common test count; `linuxX64Test`/`mingwX64Test` are skipped on macOS (they run in CI, Task 13).
  Then `./gradlew :links-detektor:assemble` → compiles `linuxX64`, `linuxArm64`, `mingwX64` klibs too.
  Known adjustments: if a target rejects a backticked test name (JS/Native forbid some characters), rename **that** test to camelCase; if `HostNormalizerGoldenTest` fails off-JVM on a row, the row is an IDN divergence — do not weaken it: either fix `PureIdn` or move the row to `JvmIdnHostTest` and add the off-JVM value to `PureIdnTest.knownDivergenceFromJavaIdn`, and list it in the commit body.

- [ ] **Step 6: Commit.** `git commit -m "Add android, apple, web, linux and mingw targets"`

---

### Task 11: ABI validation

**Files:**
- Modify: `build-logic/src/main/kotlin/ld.published.library.gradle.kts`
- Create: `links-detektor/api/**` (generated dumps)

**Interfaces:**
- Produces: ABI check/update tasks (names recorded in Step 1), used by CI in Task 13.

- [ ] **Step 1: Spike the DSL and task names.** Add `kotlin { @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class) abiValidation { enabled.set(true) } }`, then `./gradlew :links-detektor:tasks --all | grep -i abi`.
  Expected: a check/update pair — `checkKotlinAbi`/`updateKotlinAbi` in Kotlin 2.2–2.3; 2.4.20 may name them differently. Use whatever is printed in the next steps and in Task 13. Fallback if the built-in DSL is gone or broken: `org.jetbrains.kotlinx.binary-compatibility-validator` with `klib { enabled = true }` (`apiCheck`/`apiDump`).

- [ ] **Step 2: Generate and commit the dump.** `./gradlew :links-detektor:updateKotlinAbi`. Inspect it: the public surface must be exactly `LinksDetektor`, `LinksDetektorOptions`, `Url`, `NormalizedUrl`, `MalformedUrlException` — **no** `internal.*` class (in particular no `HostNormalizer`), and `detect` returns `kotlinx/collections/immutable/ImmutableList`.

- [ ] **Step 3: Prove the gate.** Temporarily make `LinksDetektor.backtracked` `internal`; `./gradlew :links-detektor:checkKotlinAbi` → FAILED. Revert; rerun → BUILD SUCCESSFUL.

- [ ] **Step 4: Commit.** `git commit -m "Track the public ABI of the library"`

---

### Task 12: KMP publishing, Dokka v2, drop committed docs

**Files:**
- Modify: `build-logic/src/main/kotlin/ld.published.library.gradle.kts` (apply `com.vanniktech.maven.publish` and `org.jetbrains.dokka`), `links-detektor/build.gradle.kts`, `links-detektor/packages.md`
- Delete (git): `docs/images`, `docs/index.html`, `docs/links-detektor`, `docs/navigation.html`, `docs/scripts`, `docs/styles`, `docs/ui-kit`

**Interfaces:**
- Produces: `:links-detektor:publishToMavenLocal`, `:links-detektor:publishAllPublicationsToMavenCentralRepository`, `:links-detektor:dokkaGenerate` → `links-detektor/build/dokka/html`.

- [ ] **Step 1: Publishing config.** Module `build.gradle.kts` keeps only `group`, `version = System.getenv("VERSION") ?: libs.versions.lib.get()` and:
  ```kotlin
  mavenPublishing {
      configure(KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"), sourcesJar = true))
      coordinates("com.slothiesmooth", "links-detektor", version.toString())
      pom { /* unchanged block from today's file, lines 142-167 */ }
      publishToMavenCentral(automaticRelease = true)
      signAllPublications()
  }
  ```
  Spike: `./gradlew :links-detektor:publishToMavenLocal`. If `JavadocJar.Dokka` rejects that task name, try `"dokkaGenerateHtml"`, then `JavadocJar.Empty()` (Maven Central only needs the jar to exist). If the plugin does not recognise the Android KMP target, see Task 7 Step 6 fallback (a).

- [ ] **Step 2: Dokka v2** in the convention plugin:
  ```kotlin
  dokka {
      moduleName.set("Links Detektor")
      dokkaSourceSets.configureEach {
          includes.from("packages.md")
          sourceLink {
              localDirectory.set(file("src/commonMain/kotlin"))
              remoteUrl("https://github.com/daksh7011/links-detektor/blob/master/links-detektor/src/commonMain/kotlin")
              remoteLineSuffix.set("#L")
          }
          perPackageOption { matchingRegex.set("""com\.slothiesmooth\.linksdetektor\.internal.*"""); suppress.set(true) }
      }
      pluginsConfiguration.html { footerMessage.set("© 2026 Slothie Smooth - Links Detektor") }
  }
  ```
  Leave `gradle.properties` without any `org.jetbrains.dokka.experimental…` flag unless Dokka 2.3.0 asks for one.

- [ ] **Step 3: Verify.** `./gradlew :links-detektor:publishToMavenLocal :links-detektor:dokkaGenerate`
  Expected under `~/.m2/repository/com/slothiesmooth/`: directories `links-detektor` (root, with `.module` + kotlin-tooling metadata), `links-detektor-jvm`, `links-detektor-android`, `links-detektor-iosarm64`, `links-detektor-iossimulatorarm64`, `links-detektor-macosarm64`, `links-detektor-js`, `links-detektor-wasm-js`, `links-detektor-linuxx64`, `links-detektor-linuxarm64`, `links-detektor-mingwx64`, each at `3.0.0-alpha1` with sources and javadoc jars. `grep -c "kotlinx-collections-immutable" ~/.m2/repository/com/slothiesmooth/links-detektor-jvm/3.0.0-alpha1/*.pom` → `1` with scope `compile`; `grep -c doist ~/.m2/repository/com/slothiesmooth/links-detektor-*/3.0.0-alpha1/*.pom` → `0` for every target; every pom's only dependency is kotlinx-collections-immutable (plus stdlib). `links-detektor/build/dokka/html/index.html` exists and contains no `internal` package page.

- [ ] **Step 4: Remove committed docs.** `git rm -r docs/images docs/index.html docs/links-detektor docs/navigation.html docs/scripts docs/styles docs/ui-kit`. `git status` must still show `docs/superpowers/` intact.

- [ ] **Step 5: Commit.** `git commit -m "Publish all KMP targets and generate docs with Dokka v2"`

---

### Task 13: CI workflows and pre-release versioning

**Decision — version source.** Release Drafter can only produce `3.0.0-alpha.1` (with a dot) from its own resolver, so the version comes from `lib` in `gradle/libs.versions.toml`. Bumping `alphaN` is a one-line catalog change in the release PR. `master.yml` reads it, marks the GitHub release as pre-release when the version contains `-`, and skips publishing when that version is already on Maven Central (releases are immutable).

**Files:**
- Create: `.github/workflows/_build.yml`, `.github/workflows/_apple.yml`
- Modify: `.github/workflows/ci.yml`, `develop.yml`, `master.yml`, `.github/release-drafter.yml`

**Interfaces:**
- Consumes: task names from Tasks 10–12 (substitute the ABI task name found in Task 11 Step 1).

- [ ] **Step 1: `_build.yml`** (`on: workflow_call`, `runs-on: ubuntu-latest`, action versions as in `/Users/slothie/IdeaProjects/Nyx/.github/workflows/_build.yml`: `actions/checkout@v7`, `actions/setup-java@v5` temurin 21, `gradle/actions/setup-gradle@v6`, konan cache step from Nyx `_ios.yml`). One job running:
  `./gradlew detektCheck :links-detektor:jvmTest :links-detektor:testAndroidHostTest :links-detektor:jsNodeTest :links-detektor:wasmJsNodeTest :links-detektor:linuxX64Test --continue`

- [ ] **Step 2: `_apple.yml`** (`on: workflow_call`, `runs-on: macos-latest`, same setup + `~/.konan` cache keyed on `hashFiles('gradle/libs.versions.toml')`):
  `./gradlew :links-detektor:iosSimulatorArm64Test :links-detektor:macosArm64Test :links-detektor:checkKotlinAbi :links-detektor:assemble`
  (ABI check lives here because Apple klib dumps cannot be produced on Linux; `assemble` cross-compiles linuxArm64 and mingwX64.)

- [ ] **Step 3: `ci.yml` and `develop.yml`.** Keep triggers, concurrency and the labeler job; replace the build job with `build: uses: ./.github/workflows/_build.yml` and `apple: uses: ./.github/workflows/_apple.yml`. Remove the "Upload JARs" steps.

- [ ] **Step 4: `master.yml`** — single job on `macos-latest` (all Kotlin/Native targets publish from one host), `needs` both reusable workflows. Steps, in order:
  1. checkout, Java 21, setup-gradle (`dependency-graph: generate-and-submit`), konan cache.
  2. Resolve version:
     ```bash
     VERSION=$(grep -m1 '^lib = ' gradle/libs.versions.toml | cut -d'"' -f2)
     echo "$VERSION" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)[0-9]+)?$' || { echo "::error::Bad version $VERSION"; exit 1; }
     echo "VERSION=$VERSION" >> "$GITHUB_ENV"
     case "$VERSION" in *-*) echo "PRERELEASE=true" ;; *) echo "PRERELEASE=false" ;; esac >> "$GITHUB_ENV"
     CODE=$(curl -s -o /dev/null -w '%{http_code}' "https://repo1.maven.org/maven2/com/slothiesmooth/links-detektor/$VERSION/")
     [ "$CODE" = "200" ] && echo "ALREADY_PUBLISHED=true" >> "$GITHUB_ENV" || echo "ALREADY_PUBLISHED=false" >> "$GITHUB_ENV"
     ```
  3. `release-drafter/release-drafter@v6` with `version`, `tag`, `name` = `${{ env.VERSION }}` and `prerelease: ${{ env.PRERELEASE }}`.
  4. `./gradlew :links-detektor:allTests :links-detektor:checkKotlinAbi`
  5. `if: env.ALREADY_PUBLISHED == 'false'` → `./gradlew :links-detektor:publishAllPublicationsToMavenCentralRepository` with the five `ORG_GRADLE_PROJECT_*` secrets exactly as today (`master.yml:49-54`).
  6. `./gradlew :links-detektor:dokkaGenerate`, then `actions/configure-pages`, `actions/upload-pages-artifact` with `path: links-detektor/build/dokka/html`, `actions/deploy-pages`.
  Remove the "Upload JARs" and "Upload Assets to Release" steps (there is no single jar any more).

- [ ] **Step 5: `release-drafter.yml`.** Keep categories and templates; delete the `version-resolver` block (version is now supplied by the workflow).

- [ ] **Step 6: Verify.**
  - `for f in .github/workflows/*.yml; do ruby -ryaml -e "YAML.load_file('$f')" || echo "BAD $f"; done` → no output.
  - Run the Step 4.2 script locally: prints nothing, and with `echo` shows `VERSION=3.0.0-alpha1`, `PRERELEASE=true`, `ALREADY_PUBLISHED=false`. Change the catalog locally to `3.0.0-alpha.1` → script exits 1; revert.
  - Run the exact Gradle command of `_apple.yml` locally → BUILD SUCCESSFUL.
  - After pushing the branch: the PR's `build` and `apple` checks are green. If Release Drafter ignores the `prerelease` input (check the draft on the first master run), add a step `gh release edit "$VERSION" --prerelease` guarded by `env.PRERELEASE == 'true'`.

- [ ] **Step 7: Commit.** `git commit -m "Run CI on Linux and macOS; publish pre-releases from the catalog version"`

---

### Task 14: README and `3.0.0-alpha1` dry run

**Files:**
- Modify: `README.md:5,23-61,63-146,170-187,207-231`, `links-detektor/packages.md`

- [ ] **Step 1: README.**
  - Kotlin badge → `2.4.20`; add a platforms line: JVM 17+, Android 23+, iOS, macOS, JS, WasmJS, Linux, Windows (mingw).
  - Installation: a KMP snippet (`commonMain.dependencies { implementation("com.slothiesmooth:links-detektor:<latest_version>") }`), the existing Gradle snippets, and a Maven snippet using `<artifactId>links-detektor-jvm</artifactId>` with one sentence saying why.
  - Usage: `detect()` returns `ImmutableList<Url>`; show `url.range` highlighting (`text.substring(url.range)`); catch `MalformedUrlException` (note: on JVM it is `java.net.MalformedURLException`); the `extractLinks` example returns `ImmutableList<String>`.
  - New section "Migrating from 2.x": `detect()` return type; `HostNormalizer` is internal; `commons-math3` no longer on the classpath; Maven users need `-jvm`; host lowercasing is locale-independent; the backtrack guard throws `IllegalStateException`; off-JVM IDN skips Nameprep prohibited/bidi/unassigned checks (emoji hosts encode instead of being rejected).
  - Remove every mention of `:sample`; development setup commands become `./gradlew detektCheck :links-detektor:allTests`.

- [ ] **Step 2: Full dry run.** `./gradlew clean detektCheck :links-detektor:allTests :links-detektor:checkKotlinAbi :links-detektor:publishToMavenLocal :links-detektor:dokkaGenerate`
  Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Consume the artifact.** In the scratch directory (not in the repo) create a one-file Gradle JVM project with `repositories { mavenLocal(); mavenCentral() }` and `implementation("com.slothiesmooth:links-detektor:3.0.0-alpha1")` on JDK 17, whose `main` prints `LinksDetektor("see http://example.com ok", LinksDetektorOptions.Default).detect().map { it.range }`.
  Expected output: `[4..21]`. This proves Gradle metadata resolution, JVM 17 compatibility and the transitive `api` dependency. Delete the scratch project.

- [ ] **Step 4: Commit.** `git commit -m "Document the multiplatform 3.0 API"`

- [ ] **Step 5: Stop.** Do not merge to `master` or publish; the owner triggers the first `3.0.0-alpha1` release.
