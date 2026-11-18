# Links-Detektor → KMP library + Compose showcase app (DESIGN)

Status: approved by owner 2026-11-18. Owner decisions at the bottom override anything above them.
References: hr-system-kmp (architecture, Metro), Nyx (wasmJs + pure engine modules), technowolf-website (hosting).

## 0. Findings that shape the plan
- `internal/HostNormalizer` is a **public** class; everything else in `internal` is `internal`. Making it internal is a binary break.
- `NormalizedUrl.create` sits in an `internal companion object`; only `Url.create` exposes `MalformedURLException` publicly.
- Normalisation is barely pinned by tests: the IDN test only asserts `assertNotNull(host)`, IPv6 normalised output is unpinned. Golden tests needed before swapping code.
- The mockk test `detect calls appropriate internal methods` mocks `LinksDetektor` and verifies the mock — vacuous; delete. mockk has no other use.
- Extra JVM-only APIs: 16 `StringBuilder.delete/replace(0,…)` sites and `System.arraycopy` in `HostNormalizer`.
- Dokka inconsistent: root plugin 1.9.20 vs catalog 2.0.0; source link points to non-existent `lib/src/main/kotlin`; `build` finalised by copying into committed `docs/`.
- Metro 1.5.2 `runtime` and `metrox-viewmodel-compose` publish `wasmJs` variants.
- hr's `hr.kmp.library` has no wasm; `nyx.kmp.library` has wasm + Karma + `archivesName` klib-collision fix.
- technowolf-website has no CI; deploy is a local `npm run deploy`.

## House rule: immutable collections everywhere (owner decision)
- Every module, library included, uses `kotlinx.collections.immutable` (0.5.2, publishes all tier-1 + linux/mingw targets). No plain `List`/`Set`/`Map` in any signature, property, Flow/StateFlow, or field; no `mutableListOf`/`ArrayList`/`HashSet`/`HashMap`.
- Library public API changes (3.0.0): `LinksDetektor.detect(): ImmutableList<Url>`; any set/map-shaped public value becomes `ImmutableSet`/`ImmutableMap`. Library depends on `kotlinx-collections-immutable` as `api` (it is no longer stdlib-only; the Doist normalize dep stays scoped to linux/mingw).
- Internals: parser scratch state (e.g. the old `Stack`, `VALID_SCHEMES`, result accumulation) uses `persistentListOf()`/`persistentSetOf()` or a persistent `.builder()` → `.build()`. `StringBuilder` for text buffers stays (it is not a collection).
- Enforcement: a detekt `ForbiddenMethodCall`/`ForbiddenImport` config in `ld.detekt` banning `kotlin.collections.mutableListOf`, `arrayListOf`, `hashMapOf`, `hashSetOf`, `mutableSetOf`, `mutableMapOf`, `java.util.*` collections, plus a review check for `List<`/`Set<`/`Map<` in signatures. Tests assert on immutable results too.

## 1. Target layout
```
build-logic/src/main/kotlin/
  ld.published.library  ld.kmp.library  ld.compose  ld.metro
  ld.feature.api  ld.feature.basic  ld.client  ld.detekt
:links-detektor                    (published; pure KMP, stdlib only — like Nyx :crypto)
:shared:presentation               (BaseViewModel, UiState)
:shared:design-library             (Ld tokens/atoms/molecules, Geist fonts)
:feature:common:client:{api,metro}
:feature:detektor:client:{api,basic}
:client                            (BaseAppGraph, App(), FeatureOrder, LdViewModelFactory)
:webApp   :desktopApp
```
- Library keeps `:links-detektor`, coordinates `com.slothiesmooth:links-detektor`, package `com.slothiesmooth.linksdetektor`. No Compose, no DI.
- `ld.published.library` separate from `ld.kmp.library`: wider targets, `explicitApi()`, lower minSdk, publishing, Dokka.
- `ld.kmp.library` = port of `nyx.kmp.library` (wasm + Karma + `archivesName`) merged with hr's JUnit Platform jvm block; drop hr's `integrationTest`/`bannedTerms`.
- Skip from hr: `:shared:api-models`, `:shared:data`, `:feature:navigation` (one destination; adaptive shell is dead weight). `FeatureHost` still owns Scaffold + NavHost so a second feature slots in later.
- `feature/detektor/client/basic` (Nyx split):
  - `domain/model/DetectedLink` (range + scheme/host/port/path/query/fragment, original + normalised)
  - `domain/usecase/DetectLinksUseCase` wrapping `LinksDetektor` on `Dispatchers.Default`
  - `presentation/DetektorViewModel` — `@Inject @ViewModelKey @ContributesIntoMap(AppScope::class, binding<ViewModel>())`, debounced input, `ImmutableSet<LinksDetektorOptions>` toggles
  - Dumb composables: input, option chips, highlighted `AnnotatedString`, result list, detail pane (side-by-side wide / sheet narrow)
  - `BasicDetektorProvider : MetroFeatureProvider(), DetektorFeature`
- Highlighting gap: `Url` exposes no offset → demo uses forward `indexOf(originalUrl, cursor)`, or add `Url.range` (decision 5).
- Hosts: `:webApp` + `:desktopApp` first, then `:androidApp` and the iOS host (see owner decision 4 — all four are required by the end). Feature modules compile for every target from day one.

## 2. Library targets
| Tier | Targets | Reason |
|---|---|---|
| Ship now | jvm, android, iosArm64, iosSimulatorArm64, macosArm64, wasmJs, js(IR) | No deps; each has a platform NFKC primitive |
| Ship now | linuxX64, linuxArm64, mingwX64 | NFKC via `com.doist.x:normalize` 1.2.0 (publishes linux/mingw variants), scoped to a `nativeNonApple` source set so other targets stay stdlib-only. Full IDN parity with Apple/JS targets. (Owner override: Linux is not de-scoped.) |
| Skip | iosX64, macosX64, tvOS/watchOS | CMP dropped iosX64; no demand |
- wasm/js library tests on Node (`browser(); nodejs()`); Karma only in app modules.
- Library Android minSdk ≈ 23 (not hr's 31). JVM bytecode level: decision 3.

## 3. JVM-only replacements
| Today | Replacement |
|---|---|
| `IDN.toASCII` | `internal expect fun idnToAscii(host)` |
| `InetAddress`/`Inet6Address`/`UnknownHostException` | pure-Kotlin `formatIp(bytes)` |
| `System.arraycopy` | `copyInto` |
| `StringUtils.isEmpty`/`defaultString` | `isNullOrEmpty()`/`.orEmpty()` |
| `java.util.Stack` | `PersistentList<Int>` builder (`add` / `removeAt(lastIndex)`) |
| `Collections.unmodifiableSet(...)` | `persistentSetOf(..)` |
| `lowercase(Locale.getDefault())` | `lowercase()` (fixes Turkish-locale bug; note in release) |
| `String.format("%%%02X", ...)` | `"%" + (code and 0xFF).toString(16).uppercase().padStart(2,'0')` (keep low-byte truncation quirk for parity) |
| `StringBuilder.delete/replace(0,…)` | `deleteRange` / `setRange` |
| `java.net.MalformedURLException` | `expect class MalformedUrlException` + JVM/Android `actual typealias` (Okio IOException pattern) |

- `formatIp`: IPv4-mapped (bytes 0–9 zero, 10–11 `0xff`) → bare dotted quad; else `[` eight uncompressed lowercase hextets `]`, matching Java.
- IDN: commonMain `PureIdn` (RFC 3492 punycode, dot mapping `。．｡`, `xn--`, 63-char limit) + `expect fun nfkc` (JVM `java.text.Normalizer`, Apple `precomposedStringWithCompatibilityMapping`, JS/wasm `String.normalize("NFKC")`). JVM/Android `idnToAscii` keeps `java.net.IDN` (no behaviour change for existing users); others use `PureIdn`. `jvmTest` differential test PureIdn vs java.net.IDN over a corpus. Documented off-JVM divergence: no Nameprep prohibited/bidi checks. Don't use JS `URL().hostname` (UTS-46).
- Semver: recommend **3.0.0** (Maven non-Gradle users need `-jvm` artifactId; `api(commons-math3)` leaves classpath; `HostNormalizer` → internal; Turkish-locale output change). Add Kotlin `abiValidation` + committed dump.

## 4. Test migration (before any implementation swap, still `kotlin("jvm")`)
1. Convert 3 files to `kotlin.test`: parameterized → table loops with `assertEquals(expected, actual, "case: $input")`; delete mockk test.
2. Golden tests from current JVM output: `HostNormalizerGoldenTest` (IDN; IPv4 decimal/hex/octal/mixed/single-number; IPv6 full/`::`/mapped/embedded-v4/zone; percent-encoded; extra dots), `PathNormalizerGoldenTest`, `UrlCreateTest` (0 and >1 URLs throw).
These move to `commonTest` unchanged and run on every target.

## 5. Build & publishing
- Gradle 8.14.2 → 9.9.0. Catalog aligned to hr: Kotlin 2.4.20, AGP 9.3.3, CMP 1.12.2, material3 1.9.0, Metro 1.5.2, navigation 2.10.0, lifecycle 2.11.0, coroutines 1.11.0, immutable collections 0.5.2, detekt 1.23.8, compileSdk 37; add `kotlinx-browser` 0.5.0.
- Remove: shadow, git-hooks, logback(+groovy), groovy, jansi, kotlin-logging, commons-math3, commons-lang3, junit-params, mockk, `src/main/resources/logback*.groovy`.
- Root settings: `includeBuild("build-logic")`, foojay resolver, `TYPESAFE_PROJECT_ACCESSORS`; `gradle.properties` from hr.
- `ld.detekt` = hr's root `detektCheck` over existing `detekt.yml`; clean ~10 `!!`/`@Suppress` sites.
- vanniktech 0.33.0 → current release supporting `com.android.kotlin.multiplatform.library` on AGP 9 (verify). `KotlinMultiplatform(javadocJar = JavadocJar.Dokka(...), sourcesJar = true)`; keep coordinates/POM/`publishToMavenCentral(automaticRelease = true)`/`signAllPublications()`; drop manual `signing {}`.
- Dokka 2.x v2 plugin, single catalog version, source link → `links-detektor/src/commonMain/kotlin`; drop `copyDokkaToDocsDir` and committed `docs/`; Pages uploads `links-detektor/build/dokka/html`.
- CI: ubuntu job `./gradlew detektCheck :links-detektor:jvmTest :links-detektor:wasmJsNodeTest :links-detektor:jsNodeTest :links-detektor:testAndroidHostTest :webApp:wasmJsBrowserDistribution :desktopApp:compileKotlin` + upload wasm dist artifact; macOS job (konan cache from Nyx `_ios.yml`) `:links-detektor:iosSimulatorArm64Test :links-detektor:macosArm64Test`. `master.yml` → `macos-latest`, publish `:links-detektor:publishAllPublicationsToMavenCentralRepository`, attach `links-detektor-web-$VERSION.zip` + `.sha256` to the release.
- Delete `:sample` (pinned to 2.0.0, unused); `:desktopApp` + README replace it. README: Kotlin badge, KMP + Maven `-jvm` snippets.

## 6. Web delivery (follow-up in technowolf-website)
- Artifact: `:webApp:wasmJsBrowserDistribution` (index.html, styles.css, composeApp.js, app .wasm, skiko .wasm, composeResources/).
- Served at `/links-detektor/app/` with relative URLs (Nyx-style index.html); verify via `wrangler dev`. No COOP/COEP needed.
- Transport: `scripts/fetch-links-detektor.mjs` as `prebuild` — pinned version + sha256, unpack release zip into gitignored `public/links-detektor/app/`. Manual copy acceptable for first launch.
- Size budget ≤ ~5 MB compressed (expect 10–14 MB raw, mostly skiko). Click-to-load poster panel; WasmGC-missing fallback message.
- Theme `LdTheme` dark-only from DESIGN.md: stage `#0d0c0a`, surface `#161411`, surface-2 `#1e1b17`, line `#2a2722`, fg `#f5f1ea`, dim `#a8a296`, ok `#8bd7a2`, amber `#ffb547`/hover `#ffc670`/ink `#1a1206`; pills, 20dp panels, 12dp inputs ≥52dp; Geist, Geist Mono only for URLs. `styles.css` background `#0d0c0a`.
- Site edits: `src/pages/links-detektor.astro` (PageHero + Browser frame + iframe), fetch script + `prebuild` + `.gitignore`, PRODUCT.md site structure, link from `/build/` `#technical` band.

## 7. Phases (each one green PR)
| # | Phase | Verify |
|---|---|---|
| 1 | Tests → kotlin.test, golden tests, drop mockk/junit-params (still JVM) | `./gradlew :links-detektor:test` |
| 2 | Immutable-collections conversion (`detect(): ImmutableList<Url>`, internals), pure-Kotlin swaps, IDN behind `idnToAscii` seam, `PureIdn` + differential test, remove dead deps/shadow/logback | `./gradlew :links-detektor:test :links-detektor:detekt` |
| 3 | Gradle 9.9, catalog, build-logic (`ld.detekt`, `ld.published.library`), flip to KMP jvm-only, source sets moved, delete `:sample` | `./gradlew detektCheck :links-detektor:jvmTest` |
| 4 | All tier-1 targets, actuals, `MalformedUrlException`, `HostNormalizer` internal, `explicitApi`, ABI dump | `./gradlew :links-detektor:allTests :links-detektor:checkLegacyAbi` (macOS) |
| 5 | Publishing, Dokka v2, workflows, README | `./gradlew :links-detektor:publishToMavenLocal :links-detektor:dokkaGenerate` |
| 6 | App skeleton: ld.* plugins, `:shared:*`, `feature/common`, `:client`, `:desktopApp`, `:webApp` (empty feature set) | `./gradlew :desktopApp:run :webApp:wasmJsBrowserDevelopmentRun :client:jvmTest` |
| 7 | `:feature:detektor` + tests; measure wasm size | `./gradlew build :webApp:wasmJsBrowserDistribution` |
| 8 | Web deploy: links-detektor web Worker (wrangler config + CI deploy job here); website follow-up (separate repo): service binding + `/links-detektor/*` forwarding, new "Open source" section | `wrangler dev`; load `technowolf.in/links-detektor/` |
| 9 | Desktop native installers: `nativeDistributions` (Dmg, Exe/Msi, Deb/Rpm), CI runner matrix, attach to release | installers install and launch on each OS |
| 10 | `:androidApp` (phone + tablet layouts) | `./gradlew :androidApp:assembleRelease`; run on emulator + tablet profile |
| 11 | iOS host (`client` framework + Xcode project, iPhone + iPad) | build & run in Simulator for both idioms |

Parallel: 6 needs only 3 (can run alongside 4–5); 5 can overlap 4; 7 needs 4 + 6.

## 8. Risks
- IDN divergence off-JVM; Android ICU-backed IDN may differ from OpenJDK → run goldens under android host tests.
- vanniktech/Dokka vs Kotlin 2.4.20/AGP 9.3.3 unverified (not used in hr/Nyx) → may need a spike.
- Maven Central releases are immutable with `automaticRelease = true` → dry-run `publishToMavenLocal`, consider `3.0.0-rc1`.
- Wasm payload/first paint in iframe → click-to-load.
- Metro on wasm unproven in house repos → Phase 6 empty-graph build de-risks early.
- `detect()` on wasm UI thread → debounce + input-length cap.

## Owner decisions (resolved 2026-11-18)
1. **Versioning:** pre-releases `3.0.0-alpha1`, `-alpha2`, … → `3.0.0-beta1`, … → `3.0.0-rc1`, … → `3.0.0`. `HostNormalizer` goes internal. Release Drafter / `master.yml` must support pre-release tags (mark GitHub release as pre-release; Maven Central accepts the suffix).
2. **Library targets:** tier 1 + linux/mingw (doist normalize for NFKC on those only; fallback generated tables).
3. **JVM floors:** library bytecode **17** (`jvmTarget` 17, toolchain may be newer). Apps may use 21+. Library Android minSdk 23.
4. **App hosts — end state is everywhere:** Android (phones + tablets), iOS (iPhone + iPad), desktop as **native installers** (macOS `.dmg`, Windows `.exe`/`.msi`, Linux `.deb` + ideally `.rpm`/AppImage), and web. Order: web + desktop first, then Android, then iOS. Desktop installers via `compose.desktop.nativeDistributions` + jpackage — they bundle a trimmed JRE, so users never see a jar or need Java installed. jpackage can't cross-build, so CI needs a macOS/Windows/Ubuntu runner matrix. macOS signing/notarization and Windows code signing are a later decision (unsigned builds trigger Gatekeeper/SmartScreen warnings).
5. **`Url.range`:** add it (additive, ships in 3.0.0).
6. **Namespaces:** library keeps `com.slothiesmooth.linksdetektor` and Maven group `com.slothiesmooth` (changing either breaks every consumer). All new app modules use `dev.slothiesmooth.linksdetektor.*`.
7. **Site placement:** not in the nav. Listed in an "Open source" section (the site has none today — to be created in technowolf-website) with a "Check it out live" link to the web deployment.
8. **Web deployment (industry-standard, researched):** independent deploys composed by path routing — Cloudflare's "vertical microfrontends" pattern. This repo's CI deploys the wasm build as its **own Cloudflare Worker with static assets**; technowolf.in's existing Worker forwards `/links-detektor/*` to it through a **service binding** (add the path to `run_worker_first`, strip the prefix, rewrite asset URLs if needed). Each side deploys/rolls back alone; the site repo never holds app binaries and needs no rebuild for app releases. Release zip stays attached to GitHub releases as a downloadable artifact. To verify at implementation: whether a more-specific zone route alone suffices (simpler, no forwarding code), and whether Compose wasm needs COOP/COEP (expected no, since no threads).
9. **API docs:** stop committing `docs/`. Later they'll live under a technowolf.in path (likely the same routing pattern in front of GitHub Pages); owner will clarify when we get there.
