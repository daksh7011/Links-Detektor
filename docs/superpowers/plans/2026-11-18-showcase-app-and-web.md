# Links Detektor Showcase App and Web Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Compose Multiplatform showcase app for the `links-detektor` library (desktop and web hosts, feature code compiling for android, ios, jvm and wasmJs) and deploy the web build as its own Cloudflare Worker under `technowolf.in/links-detektor/`.

**Architecture:** A port of hr-system-kmp's layout: `build-logic` convention plugins, `:shared:presentation`, `:shared:design-library`, `:feature:common:client:{api,metro}`, one feature pair `:feature:detektor:client:{api,basic}`, a `:client` module holding the un-annotated `BaseAppGraph` and `App()`, and one `@DependencyGraph(AppScope::class)` per host (`:desktopApp`, `:webApp`). Metro single `AppScope`; view models contribute with `@ContributesIntoMap`, providers with `@ContributesIntoSet`. The wasm build is served by a static-assets Worker with `assets.base_path = "/links-detektor"`, deployed by this repo's CI on every release.

**Tech Stack:** Kotlin 2.4.20, AGP 9.3.3, Compose Multiplatform 1.12.2 (material3 1.9.0), Metro 1.5.2, navigation-compose 2.10.0, lifecycle 2.11.0, coroutines 1.11.0, kotlinx-collections-immutable 0.5.2, turbine 1.2.1, detekt 1.23.8, kotlinx-browser 0.5.0, Wrangler >= 4.148.0 (first version with `assets.base_path`), Node >= 22.12.

**Spec:** `docs/superpowers/specs/2026-11-18-kmp-migration-design.md` (phases 6-8). Its "Owner decisions" and "House rule: immutable collections" sections override everything above them.

**Reference repos (read, never modify, except Tasks 16-17 which edit the website repo):**
- `/Users/slothie/IdeaProjects/hr-system-kmp` — architecture to mirror. "Port" in this plan means: copy the named file, rename `hr`/`Hr` to `ld`/`Ld`, change the package root `dev.slothiesmooth.hrsystem` to `dev.slothiesmooth.linksdetektor`, apply the listed changes, nothing else.
- `/Users/slothie/IdeaProjects/Nyx` — wasm target and `:webApp`.
- `/Users/slothie/Desktop/technowolf-website` — hosting site, `DESIGN.md`, `PRODUCT.md`.

**Precondition: the library plan has landed.** This plan consumes, and does not create:
- `LinksDetektor(content: String, options: ImmutableSet<LinksDetektorOptions>).detect(): ImmutableList<Url>` (primary constructor: any combination of options works, and the empty set is treated as `Default`), plus the older `LinksDetektor(content: String, options: LinksDetektorOptions)`
- `enum class LinksDetektorOptions(val value: Int)`: `Default(0)`, `QUOTE_MATCH(1)`, `SINGLE_QUOTE_MATCH(2)`, `BRACKET_MATCH(4)`, `JSON(5)`, `JAVASCRIPT(7)`, `XML(9)`, `HTML(27)`, `ALLOW_SINGLE_LEVEL_DOMAIN(32)`; `fun hasFlag(flag): Boolean`
- `Url`: `scheme`, `username`, `password`, `host`, `port: Int`, `path`, `query`, `fragment`, `originalUrl`, `fullUrl`, `normalize(): NormalizedUrl`, `range: IntRange`; `Url.create(String)` throws `MalformedUrlException`
- `build-logic` with `ld.detekt` (includes the mutable-collection `ForbiddenMethodCall`/`ForbiddenImport` rules) and `ld.published.library`
- `gradle/libs.versions.toml` aligned to hr-system-kmp, Gradle 9.9.0, root `settings.gradle.kts` with `includeBuild("build-logic")`, foojay resolver and `TYPESAFE_PROJECT_ACCESSORS`
- `.github/workflows/ci.yml` and `master.yml` rewritten for KMP

Before Task 1, run `./gradlew detektCheck :links-detektor:jvmTest` and confirm it passes. If any item above is missing, stop and report; do not recreate it here.

## Global Constraints

- Immutable collections everywhere: `ImmutableList`/`ImmutableSet`/`ImmutableMap` (or `Persistent*`) in every signature, property, `StateFlow`, and field. No plain `List`/`Set`/`Map`, no `mutableListOf`/`mutableSetOf`/`mutableMapOf`/`ArrayList`/`HashSet`/`HashMap`. Build with `persistentListOf()`, `.toImmutableList()`, or `.builder()` then `.build()`.
- The only permitted plain-collection types are the two Metro imposes, each confined to one file and converted at once: `Set<FeatureProvider>` on `BaseAppGraph.features` (multibinding type) and the three `Map<KClass<...>, () -> ...>` overrides in `LdViewModelFactory` (declared by `MetroViewModelFactory`). No other exception.
- Never `!!`. No `@Suppress` to pass detekt, except the two hr already carries (`TooManyFunctions` on `BaseViewModel` and on `FeatureContext`/`FeatureHostContext`).
- App modules use package and Android namespace root `dev.slothiesmooth.linksdetektor.*`. The library keeps `com.slothiesmooth.linksdetektor`.
- Versions only in `gradle/libs.versions.toml`; build behaviour only in `build-logic/`. Metro for DI, never Koin.
- App JVM toolchain 21. Library bytecode stays 17 (do not touch `ld.published.library`).
- Feature modules compile for android, iosArm64, iosSimulatorArm64, jvm and wasmJs from the first commit that creates them.
- Composables are dumb; view models expose render-ready state. Behaviour, states and UI text live in `commonMain`. US English UI copy.
- `LdTheme` is dark only. Tokens, verbatim: stage `#0d0c0a`, surface `#161411`, surface-2 `#1e1b17`, line `#2a2722`, fg `#f5f1ea`, dim `#a8a296`, ok `#8bd7a2`, amber `#ffb547`, amber-hover `#ffc670`, amber-ink `#1a1206`. Pills (99dp) for every button and chip, 20dp panels, 12dp inputs at least 52dp tall. Geist for all text; Geist Mono only on URLs and URL parts.
- From `DESIGN.md`: amber is the only accent; no red error colour, no cool greys, no drop shadows on flat panels or buttons, no small uppercase labels above headings.
- Web: served under `/links-detektor/` with relative asset URLs only; page background `#0d0c0a` before any script runs; compressed payload budget about 5 MB.
- Out of scope: `:androidApp`, the iOS host, desktop `nativeDistributions`. Keep `BaseAppGraph` un-annotated so those hosts add their own graph later.
- Commits: one short subject line, at most 1-3 short body lines, no `Co-Authored-By`, no AI or assistant mention. Stage by path. Never stage `.idea/`, `.DS_Store`, `local.properties`.
- Gate for every task unless it states otherwise: `./gradlew detektCheck` passes in addition to the task's own commands.

## Review Focus

1. **Text edited while a detection is in flight.** Highlight ranges must never be applied to text they were not computed from; an out-of-bounds or overlapping range must be skipped, not crash. Pinned in Task 10 (`a newer input discards the older result`) and Task 11 (`ranges outside the text are skipped`).
2. **A huge paste.** Input beyond 20,000 characters is cut at a code-point boundary, the user is told, and the UI thread on wasm is never handed more than the cap. Pinned in Task 10 (`input over the cap is cut and flagged`, `the cut never splits a surrogate pair`).
3. **Any option combination, including the empty set (the library treats it as Default).** Pinned in Task 10 (`toggling options flips the set and any combination detects`).
4. **The library throws** (from `detect()` or from `normalize()` on one odd link). The screen shows the error state with Retry, or the one link without a normalized column; the app keeps running. Pinned in Task 9 (`a throwing engine becomes Failure`, `a link that cannot be normalized keeps its original parts`).
5. **Opening `/links-detektor` without the trailing slash, or in a browser without WasmGC.** Relative assets must still resolve (redirect to the slash form) and an unsupported browser sees a readable message on the dark background, not a blank page. Pinned in Task 8 (fallback check) and Task 14 (curl checks).

---

## File Structure

```
gradle/libs.versions.toml                      (modify: add app entries)
build.gradle.kts, settings.gradle.kts          (modify: plugin aliases, includes)
build-logic/build.gradle.kts                   (modify: compose, serialization, metro plugin deps)
build-logic/src/main/kotlin/
  ld.kmp.library.gradle.kts  ld.compose.gradle.kts  ld.metro.gradle.kts
  ld.feature.api.gradle.kts  ld.feature.basic.gradle.kts  ld.client.gradle.kts
shared/presentation/        BaseViewModel, UiState
shared/design-library/      tokens/ atoms/ molecules/ + composeResources/font (Geist, Geist Mono)
feature/common/client/api/  Feature, FeatureProvider, BaseFeatureProvider, FeatureContext, FeatureHost, AppStartRoute, RouteNames
feature/common/client/metro/ MetroFeatureProvider, metroFeatureViewModel
feature/detektor/client/api/ DetektorFeature, DetektorRoute
feature/detektor/client/basic/
  domain/model/   DetectedLink.kt
  domain/usecase/ LinkEngine.kt, DetectLinksUseCase.kt
  presentation/   DetektorUiState.kt, DetektorViewModel.kt, Highlight.kt, DetailRows.kt,
                  DetektorScreen.kt, DetektorPanes.kt, BasicDetektorProvider.kt
client/                     BaseAppGraph, App, FeatureOrder, LdViewModelFactory, DetektorStartRoute
desktopApp/                 DesktopAppGraph, Main
webApp/                     WebAppGraph, Main, resources/index.html, resources/styles.css
scripts/wasm-size.mjs       compressed-size measurement against the budget
deploy/web/                 wrangler.jsonc, package.json, README.md (routing decision record)
.github/workflows/          ci.yml, master.yml (modify)
```

---

### Task 1: Catalog entries and the remaining `ld.*` convention plugins

**Files:**
- Modify: `gradle/libs.versions.toml`, `build-logic/build.gradle.kts`, `build.gradle.kts`
- Create: `build-logic/src/main/kotlin/ld.kmp.library.gradle.kts`, `ld.compose.gradle.kts`, `ld.metro.gradle.kts`, `ld.feature.api.gradle.kts`, `ld.feature.basic.gradle.kts`, `ld.client.gradle.kts`

**Interfaces:**
- Consumes: the library plan's `build-logic` (with `ld.detekt`, `ld.published.library`) and catalog. This task ADDS to both; it does not edit `ld.detekt` or `ld.published.library`.
- Produces: plugin ids `ld.kmp.library`, `ld.compose`, `ld.metro`, `ld.feature.api`, `ld.feature.basic`, `ld.client`; catalog aliases listed below, named exactly as in hr-system-kmp's `gradle/libs.versions.toml`.

- [ ] **Step 1: Add missing catalog entries.** For each key below, add it only if the library plan did not; use hr's exact key name and coordinates (copy the line from `/Users/slothie/IdeaProjects/hr-system-kmp/gradle/libs.versions.toml`).
  - `[versions]`: `metro = "1.5.2"`, `compose-multiplatform = "1.12.2"`, `compose-material3 = "1.9.0"`, `kotlinx-coroutines = "1.11.0"`, `kotlinx-serialization = "1.12.0"`, `kotlinx-collections-immutable = "0.5.2"`, `jetbrains-navigation = "2.10.0"`, `jetbrains-lifecycle = "2.11.0"`, `turbine = "1.2.1"`, `junit4 = "4.13.2"`, `junit-bom = "5.14.4"`, `androidx-test-runner = "1.7.0"`, `androidx-test-ext-junit = "1.3.0"`, `androidx-test-espresso = "3.7.0"`, `androidx-compose-ui-test = "1.12.1"`, `kotlinx-browser = "0.5.0"`, and one new key `android-app-minSdk = "31"` (hr's app floor; the library's `android-minSdk = "23"` is untouched).
  - `[libraries]`: `compose-runtime`, `compose-foundation`, `compose-material3`, `compose-ui`, `compose-components-resources`, `compose-ui-tooling-preview`, `compose-ui-tooling`, `compose-ui-test`, `compose-ui-test-manifest`, `metro-runtime`, `metrox-viewmodel`, `metrox-viewmodel-compose`, `kotlin-test`, `kotlin-test-junit5`, `junit4`, `junit-bom`, `junit-platform-launcher`, `turbine`, `androidx-test-runner`, `androidx-test-ext-junit`, `androidx-test-espresso-core`, `kotlinx-coroutines-core`, `kotlinx-coroutines-test`, `kotlinx-coroutines-swing`, `kotlinx-serialization-core`, `kotlinx-serialization-json`, `kotlinx-collections-immutable`, `jetbrains-navigation-compose`, `jetbrains-lifecycle-viewmodel-compose`, `jetbrains-lifecycle-runtime-compose`, and `kotlinx-browser = { module = "org.jetbrains.kotlinx:kotlinx-browser", version.ref = "kotlinx-browser" }`.
  - `[plugins]`: `android-kotlin-multiplatform-library`, `kotlin-multiplatform`, `kotlin-jvm`, `kotlin-plugin-compose`, `kotlin-plugin-serialization`, `compose-multiplatform`, `metro`.

- [ ] **Step 2: Extend `build-logic/build.gradle.kts`** with the dependencies hr's file has and the library plan's may lack: `compose-compiler-gradle-plugin` and `kotlin-serialization` at `libs.versions.kotlin`, `org.jetbrains.compose:compose-gradle-plugin` at `libs.versions.compose.multiplatform`, `dev.zacsweers.metro:gradle-plugin` at `libs.versions.metro`.

- [ ] **Step 3: Add `apply false` aliases to the root `build.gradle.kts`** for `kotlin.jvm`, `kotlin.plugin.compose`, `kotlin.plugin.serialization`, `compose.multiplatform`, `metro` (keep whatever the library plan put there).

- [ ] **Step 4: Create `ld.kmp.library.gradle.kts`.** Port `nyx.kmp.library.gradle.kts` (file-level `@OptIn(ExperimentalWasmDsl::class)`, the `base { archivesName.set(project.path.removePrefix(":").replace(":", "-")) }` block with its comment, `android {}` with host and device test builders, `iosArm64()`, `iosSimulatorArm64()`, `wasmJs { browser { testTask { useKarma { useChromeHeadless() } } } }`, `applyDefaultHierarchyTemplate()`, `jvmToolchain(21)`) with these changes:
  - `minSdk` reads `android-app-minSdk`; `compileSdk` reads `android-compileSdk`.
  - `defaultAndroidNamespace` returns `"dev.slothiesmooth.linksdetektor.$suffix"` (same segment rule: drop `client`, strip `-`, `:client` keeps `client`).
  - Replace `jvm()` with hr's block: `jvm { testRuns.named("test") { executionTask.configure { useJUnitPlatform() } } }`, and add hr's `jvmTest.dependencies` (`platform(junit-bom)`, `kotlin-test-junit5`, `runtimeOnly("org.junit.platform:junit-platform-launcher")`).
  - Do not port hr's `integrationTest` task, the `hr.test` system property, or `bannedTerms`.

- [ ] **Step 5: Create the other five plugins** by porting hr's files one to one:
  - `ld.compose` from `hr.compose`: resource package `"dev.slothiesmooth.linksdetektor.$suffix.resources"`; everything else unchanged (including `jvmTestImplementation(compose.desktop.currentOs)` and the `ExperimentalMaterial3Api` opt-in).
  - `ld.metro` from `hr.metro`: unchanged.
  - `ld.feature.api`, `ld.feature.basic`, `ld.client` from the `hr.*` namesakes: plugin ids renamed, dependency lists unchanged.

- [ ] **Step 6: Verify**

Run: `./gradlew :build-logic:compileKotlin help`
Expected: `BUILD SUCCESSFUL`.
Run: `./gradlew detektCheck :links-detektor:jvmTest`
Expected: `BUILD SUCCESSFUL` (nothing applies the new plugins yet, so the library is unaffected).

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts build-logic
git commit -m "Add app convention plugins and catalog entries"
```

---

### Task 2: `:shared:presentation`

**Files:**
- Create: `shared/presentation/build.gradle.kts`
- Create: `shared/presentation/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/shared/presentation/viewmodel/BaseViewModel.kt`
- Create: `shared/presentation/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/shared/presentation/state/UiState.kt`
- Test: `shared/presentation/src/commonTest/kotlin/dev/slothiesmooth/linksdetektor/shared/presentation/viewmodel/BaseViewModelTest.kt`
- Modify: `settings.gradle.kts` (add `include(":shared:presentation")`)

**Interfaces:**
- Consumes: `ld.kmp.library`, `ld.compose` (Task 1).
- Produces:
  - `abstract class BaseViewModel : ViewModel()` with `protected fun withState(block: () -> Unit)`, `protected fun ui(id: String, force: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job?`, `protected fun async(id, force, block): Job?`, the `doInit`/`DoBind`/`doBind`/`doResume`/`doPause`/`doDispose` hooks, and `@Composable fun bind()`.
  - `sealed interface UiState<out T>` with `data object Loading`, `data class Ready<T>(val value: T)`, `data class Failed(val message: String)`.

- [ ] **Step 1: Module file.** `plugins { id("ld.kmp.library"); id("ld.compose") }`; `commonMain` dependencies: `api(libs.jetbrains.lifecycle.viewmodel.compose)`, `api(libs.jetbrains.navigation.compose)`, `api(libs.kotlinx.collections.immutable)`, `implementation(libs.jetbrains.lifecycle.runtime.compose)`. No `shared.data`, FileKit or kotlinx-io (those hr modules are skipped by the spec).

- [ ] **Step 2: Write the failing test.** Port hr's `BaseViewModelTest.kt` unchanged apart from the package: the four tests `ui with the same id is deduplicated while in flight`, `ui with force cancels the in-flight job and runs the replacement`, `async returns the running job and dedups by id`, `withState mutation is visible after the dispatcher drains`.

- [ ] **Step 3: Run it to see it fail**

Run: `./gradlew :shared:presentation:jvmTest`
Expected: FAIL, unresolved reference `BaseViewModel`.

- [ ] **Step 4: Implement.** Port hr's `BaseViewModel.kt` with one change: the job registry is `private var jobs: PersistentMap<String, Job> = persistentMapOf()` and `launchDeduped` reassigns it with `jobs = jobs.put(id, job)` (hr's `mutableMapOf` is banned here). Write `UiState.kt` as specified above (`Failed` carries a ready-to-show message; there is no `AppError` in this project). Do not port `Messages`, `Polling`, `PagedList`, `LoadedList`, `links/`, `files/`.

- [ ] **Step 5: Verify**

Run: `./gradlew :shared:presentation:jvmTest :shared:presentation:compileKotlinWasmJs :shared:presentation:compileKotlinIosSimulatorArm64 :shared:presentation:compileAndroidMain detektCheck`
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts shared/presentation
git commit -m "Add shared presentation module"
```

---

### Task 3: `:shared:design-library` tokens, fonts and `LdTheme`

**Files:**
- Create: `shared/design-library/build.gradle.kts`
- Create: `shared/design-library/OFL-Geist.txt` (copy from hr)
- Create: `shared/design-library/src/commonMain/composeResources/font/` with `geist_regular.ttf`, `geist_medium.ttf`, `geist_semibold.ttf`, `geist_bold.ttf`, `geist_mono_regular.ttf`, `geist_mono_medium.ttf` (copy from hr's same folder; do not copy `bricolage_semibold.ttf`)
- Create: `shared/design-library/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/designlibrary/tokens/LdColors.kt`, `LdTheme.kt`, `Contrast.kt`
- Test: `shared/design-library/src/commonTest/kotlin/dev/slothiesmooth/linksdetektor/designlibrary/LdContrastTest.kt`
- Modify: `settings.gradle.kts` (add `include(":shared:design-library")`)

**Interfaces:**
- Consumes: `:shared:presentation` (Task 2).
- Produces (package `dev.slothiesmooth.linksdetektor.designlibrary.tokens`):
  - `@Immutable data class LdColors(stage, surface, surface2, line, fg, dim, ok, amber, amberHover, amberInk, focusHalo, scrim: Color)` and `val LdColorsDark: LdColors`
  - `@Immutable data class LdType(title, body, bodySm, label, caption, code, codeSm: TextStyle)`
  - `@Immutable data class LdDimens(space1 = 4.dp, space2 = 8.dp, space3 = 12.dp, space4 = 16.dp, space6 = 24.dp, space8 = 32.dp, radiusFocus = 6.dp, radiusField = 12.dp, radiusPanel = 20.dp, radiusSheet = 24.dp, radiusPill = 99.dp, fieldMinHeight = 52.dp, touch = 48.dp)`
  - `val MaterialTheme.ldColors: LdColors`, `MaterialTheme.ldType: LdType`, `MaterialTheme.ldDimens: LdDimens` (composable read-only getters, as hr's `hrColors`)
  - `@Composable fun LdTheme(content: @Composable () -> Unit)`
  - `fun contrastRatio(foreground: Color, background: Color): Double` (WCAG 2 relative-luminance ratio)

- [ ] **Step 1: Module file.** `plugins { id("ld.kmp.library"); id("ld.compose") }`; opt-ins `ExperimentalMaterial3Api` and `org.jetbrains.compose.resources.ExperimentalResourceApi` on all source sets; `commonMain`: `api(projects.shared.presentation)`, `implementation(libs.kotlinx.collections.immutable)`.

- [ ] **Step 2: Write the failing test**

```kotlin
class LdContrastTest {
    private val c = LdColorsDark

    @Test
    fun `the tokens are the DESIGN md values`() {
        assertEquals(Color(0xFF0D0C0A), c.stage)
        assertEquals(Color(0xFF161411), c.surface)
        assertEquals(Color(0xFF1E1B17), c.surface2)
        assertEquals(Color(0xFF2A2722), c.line)
        assertEquals(Color(0xFFF5F1EA), c.fg)
        assertEquals(Color(0xFFA8A296), c.dim)
        assertEquals(Color(0xFF8BD7A2), c.ok)
        assertEquals(Color(0xFFFFB547), c.amber)
        assertEquals(Color(0xFFFFC670), c.amberHover)
        assertEquals(Color(0xFF1A1206), c.amberInk)
    }

    @Test
    fun `every text pair clears 4_5 to 1`() {
        persistentListOf(
            c.fg to c.stage, c.fg to c.surface, c.fg to c.surface2,
            c.dim to c.stage, c.dim to c.surface, c.dim to c.surface2,
            c.amber to c.stage, c.amber to c.surface, c.ok to c.surface,
            c.amberInk to c.amber, c.amberInk to c.amberHover,
        ).forEach { (fg, bg) -> assertTrue(contrastRatio(fg, bg) >= 4.5, "$fg on $bg") }
    }

    @Test
    fun `contrast of white on black is 21`() {
        assertEquals(21.0, contrastRatio(Color.White, Color.Black), 0.01)
    }
}
```

- [ ] **Step 3: Run it to see it fail**

Run: `./gradlew :shared:design-library:jvmTest`
Expected: FAIL, unresolved `LdColorsDark`.

- [ ] **Step 4: Implement `LdColors.kt` and `Contrast.kt`.** `LdColors.kt` is the single raw-ARGB file. `focusHalo = amber.copy(alpha = 0.25f)`, `scrim = stage.copy(alpha = 0.7f)`.

- [ ] **Step 5: Implement `LdTheme.kt`.** Port the structure of hr's `HrTheme.kt` (composition locals, `geistFamily()`, `geistMonoFamily()` built with `org.jetbrains.compose.resources.Font` from `dev.slothiesmooth.linksdetektor.shared.designlibrary.resources.Res`) with these decisions:
  - No `darkTheme`/`highContrast` parameters, no Bricolage, no reduce-motion local.
  - Type scale from `DESIGN.md`: title 24sp / Bold / line 26sp / letter spacing -0.02em; body 18sp / Normal / 28sp; bodySm 16sp / Normal / 24sp; label 15sp / SemiBold / 20sp; caption 13sp / Normal / 18sp; code Geist Mono 14sp / Normal / 23sp; codeSm Geist Mono 11sp / Normal / 18sp.
  - `MaterialTheme(colorScheme = darkColorScheme(...))` mapped: `primary = amber`, `onPrimary = amberInk`, `background = stage`, `onBackground = fg`, `surface = surface`, `onSurface = fg`, `surfaceVariant = surface2`, `onSurfaceVariant = dim`, `surfaceContainer*` = `surface` (highest = `surface2`), `outline = line`, `outlineVariant = line`, `surfaceTint = Color.Transparent`, `scrim = scrim`, `error = amber`, `onError = amberInk` (no red exists in this system), `secondary`/`tertiary` = `amber` with `amberInk` on-colours.
  - Material `Typography` built from the Geist family so stock widgets match.

- [ ] **Step 6: Verify**

Run: `./gradlew :shared:design-library:jvmTest :shared:design-library:compileKotlinWasmJs :shared:design-library:compileKotlinIosSimulatorArm64 detektCheck`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts shared/design-library
git commit -m "Add design library tokens and dark theme"
```

---

### Task 4: Design-library atoms and molecules

**Files:**
- Create under `shared/design-library/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/designlibrary/`:
  `atoms/LdText.kt`, `atoms/LdFocusRing.kt`, `atoms/LdButton.kt`, `atoms/LdChip.kt`, `atoms/LdTextField.kt`, `molecules/LdPanel.kt`, `molecules/LdStates.kt`, `molecules/LdSheet.kt`, `molecules/LdMasterDetail.kt`
- Test: `shared/design-library/src/jvmTest/kotlin/dev/slothiesmooth/linksdetektor/designlibrary/LdComponentsTest.kt`

**Interfaces:**
- Consumes: Task 3 tokens.
- Produces:

```kotlin
enum class LdTextStyle { Title, Body, BodySm, Label, Caption, Code, CodeSm }
@Composable fun LdText(text: String, modifier: Modifier = Modifier, style: LdTextStyle = LdTextStyle.Body,
    color: Color = MaterialTheme.ldColors.fg, maxLines: Int = Int.MAX_VALUE)
@Composable fun Modifier.ldFocusRing(interactionSource: InteractionSource, cornerRadius: Dp): Modifier
enum class LdButtonStyle { Primary, Ghost }
@Composable fun LdButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    style: LdButtonStyle = LdButtonStyle.Primary, enabled: Boolean = true)
@Composable fun LdChip(text: String, selected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier)
@Composable fun LdTextField(value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, placeholder: String = "", minLines: Int = 1, maxLines: Int = Int.MAX_VALUE)
@Composable fun LdPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)
@Composable fun LdEmptyState(title: String, message: String, modifier: Modifier = Modifier)
@Composable fun LdLoadingState(label: String, modifier: Modifier = Modifier)
@Composable fun LdErrorState(title: String, message: String, onRetry: () -> Unit, modifier: Modifier = Modifier)
@Composable fun LdSheet(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit)
val LdSidePaneFrom: Dp   // 720.dp
@Composable fun LdMasterDetail(chosen: Boolean, onClose: () -> Unit,
    master: @Composable (Modifier) -> Unit, detail: @Composable () -> Unit,
    placeholderTitle: String, placeholderMessage: String)
```

Visual decisions (the rest follows the hr namesake):
- `LdButton`: pill, height 48dp, horizontal padding 24dp. Primary fills `amber` with `amberInk` label, `amberHover` on hover. Ghost is transparent with `fg` label and a 1dp `line` border, `surface` fill on hover. No elevation.
- `LdChip`: pill, 40dp tall, 1dp `line` border, `fg` label; selected fills `amber` with `amberInk` label. Exposes `Role.Checkbox` and `toggleableState` semantics.
- `LdTextField`: `stage` fill, 1dp `line` border, `radiusField`, min height `fieldMinHeight`, label above in `Label` style; focused draws a 3dp `focusHalo` ring and an `amber` border. Cursor `amber`.
- `LdPanel`: `surface` fill, `radiusPanel`, 24dp padding, no shadow, no border.
- `LdErrorState`: title in `fg`, message in `dim`, one Primary `LdButton("Try again")`. No red.
- `LdSheet`: `ModalBottomSheet` with `surface` container, `radiusSheet` top corners, `scrim` colour.
- `LdMasterDetail`: `BoxWithConstraints`; at `maxWidth >= LdSidePaneFrom` a `Row` with `master(Modifier.weight(1f))` and the detail (or an `LdEmptyState` placeholder when `!chosen`) in a `weight(1f)` box; below it `master(Modifier.fillMaxSize())` and, when `chosen`, `LdSheet(onClose) { detail() }`.
- `ldFocusRing`: port `hrFocusRing`, 2dp `amber` outline at `radiusFocus` offset, keyboard focus only.

- [ ] **Step 1: Write the failing test** (`@OptIn(ExperimentalTestApi::class)`, each test wraps content in `LdTheme`)

```kotlin
@Test fun `a chip reports its state and toggles on click`() = runComposeUiTest {
    var on by mutableStateOf(false)
    setContent { LdTheme { LdChip("HTML", selected = on, onToggle = { on = !on }) } }
    onNodeWithText("HTML").assertIsOff().performClick()
    onNodeWithText("HTML").assertIsOn()
}

@Test fun `the error state offers a retry`() = runComposeUiTest {
    var retried = 0
    setContent { LdTheme { LdErrorState("Could not check that text", "Try again.", onRetry = { retried++ }) } }
    onNodeWithText("Try again").performClick()
    assertEquals(1, retried)
}

@Test fun `wide shows master and detail side by side`() = runComposeUiTest {
    setContent { LdTheme { Box(Modifier.size(1000.dp, 600.dp)) { sample(chosen = true) } } }
    onNodeWithText("MASTER").assertIsDisplayed()
    onNodeWithText("DETAIL").assertIsDisplayed()
}

@Test fun `wide with nothing chosen shows the placeholder`() = runComposeUiTest {
    setContent { LdTheme { Box(Modifier.size(1000.dp, 600.dp)) { sample(chosen = false) } } }
    onNodeWithText("Pick a link").assertIsDisplayed()
    onNodeWithText("DETAIL").assertDoesNotExist()
}

@Test fun `narrow shows the detail only when chosen`() = runComposeUiTest {
    var chosen by mutableStateOf(false)
    setContent { LdTheme { Box(Modifier.size(400.dp, 700.dp)) { sample(chosen) } } }
    onNodeWithText("DETAIL").assertDoesNotExist()
    chosen = true
    waitForIdle()
    onNodeWithText("DETAIL").assertIsDisplayed()
}
```

`sample(chosen)` is a private composable calling `LdMasterDetail(chosen, onClose = {}, master = { LdText("MASTER", it) }, detail = { LdText("DETAIL") }, placeholderTitle = "Pick a link", placeholderMessage = "Its parts show here.")`.

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew :shared:design-library:jvmTest`
Expected: FAIL, unresolved `LdChip`.

- [ ] **Step 3: Implement the nine files** to the signatures and visual decisions above.

- [ ] **Step 4: Verify**

Run: `./gradlew :shared:design-library:jvmTest :shared:design-library:compileKotlinWasmJs :shared:design-library:compileKotlinIosSimulatorArm64 detektCheck`
Expected: `BUILD SUCCESSFUL`, 8 tests passed in the module.

- [ ] **Step 5: Commit**

```bash
git add shared/design-library
git commit -m "Add design library atoms and molecules"
```

---

### Task 5: `:feature:common:client:{api,metro}` and `:feature:detektor:client:api`

**Files:**
- Create: `feature/common/client/api/build.gradle.kts` and, under `src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/feature/common/api/`: `Feature.kt`, `FeatureProvider.kt`, `BaseFeatureProvider.kt`, `FeatureContext.kt`, `FeatureHost.kt`, `AppStartRoute.kt`, `RouteNames.kt`
- Test: `feature/common/client/api/src/jvmTest/kotlin/dev/slothiesmooth/linksdetektor/feature/common/api/FeatureHostContextTest.kt`
- Create: `feature/common/client/metro/build.gradle.kts` and, under `src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/feature/common/metro/`: `MetroFeatureProvider.kt`, `MetroViewModel.kt`
- Create: `feature/detektor/client/api/build.gradle.kts`, `feature/detektor/client/api/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/feature/detektor/api/DetektorFeature.kt`
- Modify: `settings.gradle.kts` (add `:feature:common:client:api`, `:feature:common:client:metro`, `:feature:detektor:client:api`)

**Interfaces:**
- Consumes: `ld.feature.api`, `ld.feature.basic` (Task 1); `BaseViewModel` (Task 2).
- Produces (identical to hr's signatures, new package):
  - `interface Feature`; `interface FeatureProvider : Feature { @Composable fun provideContent(context: FeatureContext, content: @Composable () -> Unit); fun provideNavigation(context: FeatureContext, builder: NavGraphBuilder) }`
  - `abstract class BaseFeatureProvider : FeatureProvider` with `@Composable abstract fun onProvideContent(...)`, `open fun onProvideNavigation(...)`, `protected open suspend fun onReceiveAction(action: Action, context: FeatureContext)`, `protected fun onSendAction(action: Action): Boolean`, `interface Action`
  - `interface FeatureContext` (all eleven members of hr's interface)
  - `class FeatureHostContext(internal val features: ImmutableList<FeatureProvider>, internal val navController: NavHostController) : FeatureContext`
  - `@Composable fun FeatureHost(context: FeatureHostContext, startDestinationProvider: () -> Any?)`
  - `interface AppStartRoute { val route: Any }`; `inline fun <reified T : Any> routeNameOf(): String`
  - `abstract class MetroFeatureProvider : BaseFeatureProvider()`; `@Composable inline fun <reified T : BaseViewModel> metroFeatureViewModel(): T`
  - `interface DetektorFeature : Feature`; `@Serializable data object DetektorRoute`

- [ ] **Step 1: Module files.** `feature/common/client/api`: `id("ld.feature.api")`, `commonMain { api(projects.shared.presentation) }`. `feature/common/client/metro`: `id("ld.feature.basic")`, `commonMain { api(projects.feature.common.client.api); api(projects.shared.presentation) }`. `feature/detektor/client/api`: `id("ld.feature.api")`, `commonMain { api(projects.feature.common.client.api) }`.

- [ ] **Step 2: Write the failing test.** Port hr's `FeatureHostContextTest.kt` (`reset stack leaves exactly the new screen, however long the stack was`) with one change: the private `stack()` helper returns `ImmutableList<String?>` via `.toImmutableList()` and the assertions compare against `persistentListOf(...)`.

- [ ] **Step 3: Run it to see it fail**

Run: `./gradlew :feature:common:client:api:jvmTest`
Expected: FAIL, unresolved `FeatureHostContext`.

- [ ] **Step 4: Implement.** Port the seven `api` files and two `metro` files from hr with these changes only:
  - `FeatureHostContext.rootEntries` becomes `private var rootEntries: PersistentSet<String> = persistentSetOf()`; `add`/`clear` become reassignments (`rootEntries = rootEntries.add(id)`, `rootEntries = persistentSetOf()`).
  - Do not port `metro/FileBindings.kt`.
  - `DetektorFeature.kt` holds the interface and the route, each with a one-line KDoc.

- [ ] **Step 5: Verify**

Run: `./gradlew :feature:common:client:api:jvmTest :feature:common:client:metro:compileKotlinWasmJs :feature:common:client:metro:compileKotlinIosSimulatorArm64 :feature:detektor:client:api:compileKotlinWasmJs :feature:detektor:client:api:compileKotlinIosSimulatorArm64 :feature:detektor:client:api:compileAndroidMain detektCheck`
Expected: `BUILD SUCCESSFUL`, 1 test passed. This is the first proof that Metro and `metrox-viewmodel-compose` resolve for wasmJs; if resolution fails here, stop and report (spec risk "Metro on wasm unproven").

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts feature/common feature/detektor/client/api
git commit -m "Add feature host modules and the detektor feature api"
```

---

### Task 6: `:client` with an empty feature set

**Files:**
- Create: `client/build.gradle.kts`
- Create under `client/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/client/`: `BaseAppGraph.kt`, `App.kt`, `FeatureOrder.kt`, `LdViewModelFactory.kt`, `DetektorStartRoute.kt`
- Test: `client/src/jvmTest/kotlin/dev/slothiesmooth/linksdetektor/client/FeatureOrderTest.kt`, `TestAppGraph.kt`, `AppSmokeTest.kt`
- Modify: `settings.gradle.kts` (add `:client`)

**Interfaces:**
- Consumes: Tasks 2-5.
- Produces:

```kotlin
interface BaseAppGraph : ViewModelGraph {
    @Multibinds(allowEmpty = true) val features: Set<FeatureProvider>   // Metro's multibinding type; convert at once
    val featureOrder: ImmutableList<KClass<out Feature>>
    val appStartRoute: AppStartRoute
    @Provides fun provideFeatureOrder(): ImmutableList<KClass<out Feature>> = DefaultFeatureOrder
}
val DefaultFeatureOrder: ImmutableList<KClass<out Feature>>            // persistentListOf(DetektorFeature::class)
fun sortFeatures(features: ImmutableSet<FeatureProvider>, order: ImmutableList<KClass<out Feature>>): ImmutableList<FeatureProvider>
@Composable fun App(graph: BaseAppGraph)
class LdViewModelFactory(...) : MetroViewModelFactory()               // @Inject @SingleIn(AppScope) @ContributesBinding(AppScope)
class DetektorStartRoute : AppStartRoute                               // @Inject @ContributesBinding(AppScope); route = DetektorRoute
```

- [ ] **Step 1: Module file.** `id("ld.client")`. Port hr's `client/build.gradle.kts` without the iOS framework block (the iOS host plan adds it) and with these dependencies: `commonMain { implementation(libs.jetbrains.lifecycle.runtime.compose); api(projects.shared.presentation); api(projects.shared.designLibrary); api(projects.feature.common.client.api); api(projects.feature.common.client.metro); api(projects.feature.detektor.client.api) }`, `jvmTest { implementation(libs.kotlinx.coroutines.swing) }`. Keep the `ExperimentalCoroutinesApi` opt-in.

- [ ] **Step 2: Write the failing tests.**
  - `FeatureOrderTest.kt`: port hr's three tests (`providers are ordered by the feature order, not by set order`, `a provider outside the order fails fast`, `a provider matching two entries fails fast`), calling `sortFeatures(persistentSetOf(second, first), persistentListOf(FirstFeature::class, SecondFeature::class))` and asserting `assertEquals(persistentListOf<FeatureProvider>(first, second), sorted)`.
  - `TestAppGraph.kt`: `@DependencyGraph(AppScope::class) interface TestAppGraph : BaseAppGraph` and nothing else.
  - `AppSmokeTest.kt`:

```kotlin
@Test fun `the graph builds and every contributed feature has a place in the order`() {
    val graph = createGraph<TestAppGraph>()
    val sorted = sortFeatures(graph.features.toImmutableSet(), graph.featureOrder)
    assertEquals(graph.features.size, sorted.size)
}

@Test fun `the app root composes with no feature installed`() = runComposeUiTest {
    val graph = createGraph<TestAppGraph>()
    setContent { App(graph) }
    onNodeWithContentDescription("Links Detektor").assertExists()
}
```

- [ ] **Step 3: Run them to see them fail**

Run: `./gradlew :client:jvmTest`
Expected: FAIL, unresolved `sortFeatures`.

- [ ] **Step 4: Implement.**
  - `FeatureOrder.kt`: port hr's `sortFeatures` body with the immutable parameter types above.
  - `LdViewModelFactory.kt`: port `HrViewModelFactory.kt`. Its three `Map` constructor properties are the documented Metro exception; add a one-line comment saying so.
  - `BaseAppGraph.kt`: as in Interfaces, with hr's KDoc about why it is not annotated. No session, queue, tile or chromeless members.
  - `App.kt`: `CompositionLocalProvider(LocalMetroViewModelFactory provides graph.metroViewModelFactory) { LdTheme { ... } }`; remember a `FeatureHostContext(sortFeatures(graph.features.toImmutableSet(), graph.featureOrder), rememberNavController())`; a `Box` with `fillMaxSize()`, `background(MaterialTheme.colorScheme.background)` and `semantics { contentDescription = "Links Detektor" }` holding `FeatureHost(context) { graph.appStartRoute.route.takeIf { graph.features.isNotEmpty() } }`. With no feature the host renders only the stage background. No fade-in, no lifecycle effects.

- [ ] **Step 5: Verify**

Run: `./gradlew :client:jvmTest :client:compileKotlinWasmJs :client:compileKotlinIosSimulatorArm64 :client:compileAndroidMain detektCheck`
Expected: `BUILD SUCCESSFUL`, 5 tests passed.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts client
git commit -m "Add client module with the base app graph"
```

---

### Task 7: `:desktopApp`

**Files:**
- Create: `desktopApp/build.gradle.kts`
- Create: `desktopApp/src/main/kotlin/dev/slothiesmooth/linksdetektor/desktop/DesktopAppGraph.kt`, `Main.kt`
- Modify: `settings.gradle.kts` (add `:desktopApp`)

**Interfaces:**
- Consumes: `App(graph)`, `BaseAppGraph` (Task 6).
- Produces: `@DependencyGraph(AppScope::class) interface DesktopAppGraph : BaseAppGraph`; `fun main()`; Gradle task `:desktopApp:run`.

- [ ] **Step 1: Module file.** Port hr's `desktopApp/build.gradle.kts` (`kotlin.jvm`, `kotlin.plugin.compose`, `compose.multiplatform`, `id("ld.metro")`, `jvmToolchain(21)`, the dependency block, `tasks.test { useJUnitPlatform() }`) with these changes: `mainClass = "dev.slothiesmooth.linksdetektor.desktop.MainKt"`, keep the `javaHome` toolchain launcher line, and omit the whole `nativeDistributions { }` block (a later plan adds installers).

- [ ] **Step 2: Implement.** `DesktopAppGraph.kt` as in Interfaces. `Main.kt`: `val graph = createGraph<DesktopAppGraph>()`, then `application { Window(onCloseRequest = ::exitApplication, title = "Links Detektor", state = rememberWindowState(size = DpSize(1280.dp, 800.dp))) { App(graph) } }`. No icon and no `WindowFit` port.

- [ ] **Step 3: Verify**

Run: `./gradlew :desktopApp:compileKotlin detektCheck`
Expected: `BUILD SUCCESSFUL` (Metro generates the desktop graph).
Run: `./gradlew :desktopApp:run`
Expected: a 1280x800 window titled "Links Detektor" filled with `#0d0c0a` and nothing else. Close it.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts desktopApp
git commit -m "Add desktop host"
```

---

### Task 8: `:webApp` and the size script

**Files:**
- Create: `webApp/build.gradle.kts`
- Create: `webApp/src/wasmJsMain/kotlin/dev/slothiesmooth/linksdetektor/web/WebAppGraph.kt`, `Main.kt`
- Create: `webApp/src/wasmJsMain/resources/index.html`, `styles.css`
- Test: `webApp/src/wasmJsTest/kotlin/dev/slothiesmooth/linksdetektor/web/WebAppGraphTest.kt`
- Create: `scripts/wasm-size.mjs`; Test: `scripts/wasm-size.test.mjs`
- Modify: `settings.gradle.kts` (add `:webApp`), `.gitignore` (add `kotlin-js-store/` only if the library plan did not commit it; if `kotlin-js-store/yarn.lock` is already tracked, commit its update instead)

**Interfaces:**
- Consumes: `App(graph)`, `BaseAppGraph` (Task 6).
- Produces: `@DependencyGraph(AppScope::class) interface WebAppGraph : BaseAppGraph`; Gradle tasks `:webApp:wasmJsBrowserDevelopmentRun`, `:webApp:wasmJsBrowserDistribution` writing to `webApp/build/dist/wasmJs/productionExecutable/`; `node scripts/wasm-size.mjs <dir> [--budget-mb N]`, which exports `measure(dir): { files: [{ path, raw, gzip, brotli }], total: { raw, gzip, brotli } }`, prints one line per file and a total, and exits 1 when the brotli total exceeds the budget (default 5).

- [ ] **Step 1: Module file.** Port Nyx's `webApp/build.gradle.kts` (`outputModuleName = "composeApp"`, `outputFileName = "composeApp.js"`, `binaries.executable()`, `jvmToolchain(21)`) and add `id("ld.metro")`. `wasmJsMain` dependencies: `projects.client`, `libs.compose.runtime`, `libs.compose.ui`, `libs.metro.runtime`, `libs.kotlinx.browser`, `libs.kotlinx.collections.immutable`, `libs.kotlinx.coroutines.core`. `wasmJsTest`: `libs.kotlin.test`. Add the Karma block from `ld.kmp.library` (`browser { testTask { useKarma { useChromeHeadless() } } }`). No Koin, no FileKit.

- [ ] **Step 2: Write the failing wasm test**

```kotlin
class WebAppGraphTest {
    @Test fun `the web graph builds on wasm`() {
        val graph = createGraph<WebAppGraph>()
        assertEquals(DefaultFeatureOrder, graph.featureOrder)
        assertEquals(DetektorRoute, graph.appStartRoute.route)
    }
}
```

Run: `./gradlew :webApp:wasmJsBrowserTest`
Expected: FAIL, unresolved `WebAppGraph`. (Needs Chrome; set `CHROME_BIN` if Karma cannot find it.)

- [ ] **Step 3: Write `index.html` and `styles.css`.** Every URL is relative so the page works under any path prefix. The inline style paints the stage before any request finishes.

```html
<!DOCTYPE html>
<html lang="en" style="background:#0d0c0a;color-scheme:dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#0d0c0a">
    <title>Links Detektor</title>
    <style>html,body{margin:0;background:#0d0c0a;color:#f5f1ea}</style>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <p id="boot" role="status">Loading Links Detektor…</p>
    <script>
        (function () {
            var boot = document.getElementById("boot");
            // A module declaring one WasmGC struct type; validates only where WasmGC exists.
            var gc = new Uint8Array([0, 97, 115, 109, 1, 0, 0, 0, 1, 5, 1, 95, 1, 120, 0]);
            var supported = typeof WebAssembly === "object" && WebAssembly.validate(gc);
            if (!supported) {
                boot.textContent = "Links Detektor needs a browser with WebAssembly GC: " +
                    "Chrome or Edge 119, Firefox 120, Safari 18.2, or newer.";
                return;
            }
            var app = document.createElement("script");
            app.src = "composeApp.js";
            app.onerror = function () { boot.textContent = "Links Detektor could not load. Reload the page to try again."; };
            document.body.appendChild(app);
        })();
    </script>
    <noscript><p>Links Detektor needs JavaScript.</p></noscript>
</body>
</html>
```

`styles.css`: Nyx's rules (`html, body { width: 100%; height: 100%; margin: 0; overflow: hidden; }`) plus `background: #0d0c0a;` on `html, body`, and `#boot, noscript p { position: fixed; inset: 0; display: grid; place-content: center; margin: 0; padding: 16px; text-align: center; color: #a8a296; font: 18px/1.5 system-ui, sans-serif; }`.

- [ ] **Step 4: Implement `WebAppGraph.kt` and `Main.kt`.** `main()` builds `createGraph<WebAppGraph>()`, then `ComposeViewport { App(graph) }` (`@OptIn(ExperimentalComposeUiApi::class)`), then removes the boot line with `document.getElementById("boot")?.remove()`.

- [ ] **Step 5: Write the size script test, then the script.**

```js
// scripts/wasm-size.test.mjs  (node --test)
test("measure sums raw and compressed sizes per file", () => {
  const dir = mkdtempSync(join(tmpdir(), "wasm-size-"));
  mkdirSync(join(dir, "sub"));
  writeFileSync(join(dir, "a.txt"), "a".repeat(10_000));
  writeFileSync(join(dir, "sub", "b.bin"), randomBytes(4_096));
  const { files, total } = measure(dir);
  assert.deepEqual(files.map((f) => f.path).sort(), ["a.txt", "sub/b.bin"]);
  assert.equal(total.raw, 14_096);
  assert.ok(total.brotli < total.raw && total.gzip < total.raw);
  assert.ok(files.find((f) => f.path === "a.txt").brotli < 200);
});
```

Run: `node --test scripts/wasm-size.test.mjs` — Expected: FAIL (module not found). Implement with `node:zlib` (`gzipSync` level 9, `brotliCompressSync` quality 11) and a recursive walk; the CLI part runs only when the file is the entry point. Run again — Expected: 1 test passed.

- [ ] **Step 6: Verify**

Run: `./gradlew :webApp:wasmJsBrowserTest :webApp:wasmJsBrowserDistribution detektCheck`
Expected: `BUILD SUCCESSFUL`, 1 wasm test passed; `ls webApp/build/dist/wasmJs/productionExecutable` shows `index.html`, `styles.css`, `composeApp.js`, at least two `.wasm` files and `composeResources/`.
Run: `node scripts/wasm-size.mjs webApp/build/dist/wasmJs/productionExecutable`
Expected: a per-file table, a total line, exit code 0. Note the brotli total in the commit body (baseline before the feature).
Run: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
Expected: the browser opens on a `#0d0c0a` page; "Loading Links Detektor…" shows briefly and disappears; no white frame at any point; the console has no errors.
Fallback check: in `index.html` temporarily change `var supported = ...` to `var supported = false;`, reload, confirm the WasmGC message shows on the dark background, then revert the line.

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts webApp scripts/wasm-size.mjs scripts/wasm-size.test.mjs
git commit -m "Add web host and wasm size script"
```

---

### Task 9: Detektor domain: model, use case

**Files:**
- Create: `feature/detektor/client/basic/build.gradle.kts`
- Create under `feature/detektor/client/basic/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/feature/detektor/basic/`: `domain/model/DetectedLink.kt`, `domain/usecase/LinkEngine.kt`, `domain/usecase/DetectLinksUseCase.kt`
- Test under `feature/detektor/client/basic/src/commonTest/kotlin/dev/slothiesmooth/linksdetektor/feature/detektor/basic/domain/`: `DetectLinksUseCaseTest.kt`
- Modify: `settings.gradle.kts` (add `:feature:detektor:client:basic`)

**Interfaces:**
- Consumes: the library API listed in the header, including the `LinksDetektor(content, options: ImmutableSet<LinksDetektorOptions>)` constructor (from the library plan, not created here); `ld.feature.basic`.
- Produces:

```kotlin
@Immutable data class LinkParts(val full: String, val scheme: String, val username: String, val password: String,
    val host: String, val port: Int, val path: String, val query: String, val fragment: String)

/** [start] inclusive, [endExclusive] exclusive, both indexes into the analyzed text. [normalized] is null when normalizing threw. */
@Immutable data class DetectedLink(val id: Int, val start: Int, val endExclusive: Int,
    val original: LinkParts, val normalized: LinkParts?)

fun interface LinkEngine { fun detect(content: String, options: ImmutableSet<LinksDetektorOptions>): ImmutableList<Url> }
class LibraryLinkEngine : LinkEngine          // @Inject @ContributesBinding(AppScope::class); calls LinksDetektor(content, options).detect()

@Qualifier annotation class DetectionDispatcher
@ContributesTo(AppScope::class) interface DetektorBindings {
    @Provides @DetectionDispatcher fun provideDetectionDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

sealed interface DetectionResult {
    data class Success(val text: String, val options: ImmutableSet<LinksDetektorOptions>, val links: ImmutableList<DetectedLink>) : DetectionResult
    data class Failure(val message: String) : DetectionResult
}

@Inject class DetectLinksUseCase(private val engine: LinkEngine, @DetectionDispatcher private val dispatcher: CoroutineDispatcher) {
    suspend operator fun invoke(text: String, options: ImmutableSet<LinksDetektorOptions>): DetectionResult
}
```

Decisions:
- Mapping a `Url` to a `DetectedLink`: `start = url.range.first`, `endExclusive = url.range.last + 1`. The library plan (Task 3) defines `Url.range` as a standard `IntRange` (`start until endExclusive`) covering the whole source span — including a stray `%` the detector drops from `originalUrl` — so deriving the end from `originalUrl.length` would under-highlight that case. Add a test row `"see http://exa%mple.com/x ok"` → highlighted span `4..<25`. Null string parts become `""`.
- `normalized` is built inside `runCatching`; a failure yields `null` (rethrow `CancellationException`).
- `invoke` runs `engine.detect` and the mapping inside `withContext(dispatcher)`; any `Exception` other than `CancellationException` becomes `Failure("That text could not be checked.")`. Blank text returns `Success(text, options, persistentListOf())` without calling the engine.

- [ ] **Step 1: Module file.** `id("ld.feature.basic")`; `commonMain { api(projects.feature.detektor.client.api); api(projects.feature.common.client.api); api(projects.feature.common.client.metro); api(projects.shared.presentation); api(projects.shared.designLibrary); implementation(projects.linksDetektor) }`.

- [ ] **Step 2: Write the failing tests.**

```kotlin
class DetectLinksUseCaseTest {
    private val text = "see https://example.com/a?b=1#c and http://user:pw@Sub.Example.org:8080/x, ok"

    @Test fun `links come back in order with ranges that cut the original out of the text`() = runTest {
        val useCase = DetectLinksUseCase(LibraryLinkEngine(), StandardTestDispatcher(testScheduler))
        val result = assertIs<DetectionResult.Success>(useCase(text, persistentSetOf()))
        assertEquals(2, result.links.size)
        assertEquals(persistentListOf(0, 1), result.links.map { it.id }.toImmutableList())
        result.links.forEach { link ->
            assertEquals(link.original.full, text.substring(link.start, link.endExclusive))
        }
        assertTrue(result.links[0].endExclusive <= result.links[1].start)
        val first = result.links[0].original
        assertEquals("example.com", first.host)
        assertEquals("/a", first.path)
        assertEquals("?b=1", first.query)
        assertEquals("#c", first.fragment)
        assertEquals("sub.example.org", assertNotNull(result.links[1].normalized).host)
    }

    @Test fun `blank text succeeds with no links and never calls the engine`() = runTest {
        var calls = 0
        val useCase = DetectLinksUseCase({ _, _ -> calls++; persistentListOf() }, StandardTestDispatcher(testScheduler))
        assertEquals(DetectionResult.Success("  \n", persistentSetOf(), persistentListOf()), useCase("  \n", persistentSetOf()))
        assertEquals(0, calls)
    }

    @Test fun `a throwing engine becomes Failure`() = runTest {
        val useCase = DetectLinksUseCase({ _, _ -> error("boom") }, StandardTestDispatcher(testScheduler))
        assertEquals(DetectionResult.Failure("That text could not be checked."), useCase("x.com", persistentSetOf()))
    }

    @Test fun `cancellation is not swallowed`() = runTest {
        val useCase = DetectLinksUseCase({ _, _ -> throw CancellationException("stop") }, StandardTestDispatcher(testScheduler))
        assertFailsWith<CancellationException> { useCase("x.com", persistentSetOf()) }
    }

    @Test fun `a link that cannot be normalized keeps its original parts`() = runTest {
        // toDetectedLink takes the normalizer as a parameter so this path is testable without a broken Url.
        val url = LibraryLinkEngine().detect("https://example.com", persistentSetOf()).first()
        val link = url.toDetectedLink(id = 0) { error("cannot normalize") }
        assertNull(link.normalized)
        assertEquals("example.com", link.original.host)
    }
}
```

The last test fixes one more signature: `internal fun Url.toDetectedLink(id: Int, normalize: (Url) -> Url = Url::normalize): DetectedLink` in `DetectedLink.kt`.

If the first test's `substring` assertion fails, the library's `originalUrl` is not the exact input slice: stop and report it as a library-plan defect rather than adjusting the mapper. If the query or fragment assertions fail only on whether the leading `?`/`#` is included, change the expected literals to what the library returns; the mapper passes parts through untouched.

- [ ] **Step 3: Run them to see them fail**

Run: `./gradlew :feature:detektor:client:basic:jvmTest`
Expected: FAIL, unresolved `LibraryLinkEngine`.

- [ ] **Step 4: Implement the three files** to the signatures and decisions above.

- [ ] **Step 5: Verify**

Run: `./gradlew :feature:detektor:client:basic:jvmTest :feature:detektor:client:basic:compileKotlinWasmJs :feature:detektor:client:basic:compileKotlinIosSimulatorArm64 :feature:detektor:client:basic:compileAndroidMain detektCheck`
Expected: `BUILD SUCCESSFUL`, 5 tests passed.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts feature/detektor/client/basic
git commit -m "Add detektor domain model and detect use case"
```

---

### Task 10: `DetektorViewModel`

**Files:**
- Create: `feature/detektor/client/basic/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/feature/detektor/basic/presentation/DetektorUiState.kt`, `DetektorViewModel.kt`
- Test: `feature/detektor/client/basic/src/commonTest/kotlin/dev/slothiesmooth/linksdetektor/feature/detektor/basic/presentation/DetektorViewModelTest.kt`, `DetektorScreenStateTest.kt`

**Interfaces:**
- Consumes: `DetectLinksUseCase`, `DetectionResult`, `DetectedLink` (Task 9); the library's `LinksDetektor(content, ImmutableSet<LinksDetektorOptions>)` constructor, reached through `LibraryLinkEngine`; `BaseViewModel`, `UiState` (Task 2).
- Produces:

```kotlin
const val MAX_INPUT_CHARS = 20_000
const val INPUT_DEBOUNCE_MS = 250L

@Immutable data class DetectionUi(val analyzedText: String, val links: ImmutableList<DetectedLink>) {
    companion object { val Empty = DetectionUi("", persistentListOf()) }
}

@Immutable data class DetektorUiState(
    val input: String = "",
    val truncated: Boolean = false,
    val options: ImmutableSet<LinksDetektorOptions> = persistentSetOf(),
    val detecting: Boolean = false,
    val result: UiState<DetectionUi> = UiState.Ready(DetectionUi.Empty),
    val selectedId: Int? = null,
) {
    val selected: DetectedLink?   // the Ready link whose id == selectedId, else null
}

enum class DetektorScreenState { Empty, Loading, NoLinks, Error, Content }
fun screenStateOf(state: DetektorUiState): DetektorScreenState

/** The toggles the screen offers, in display order: every constant except Default. */
val ToggleableOptions: ImmutableList<LinksDetektorOptions>

class DetektorViewModel(private val detectLinks: DetectLinksUseCase) : BaseViewModel() {
    val state: StateFlow<DetektorUiState>
    fun onInputChange(text: String)
    fun onToggleOption(option: LinksDetektorOptions)
    fun onSelectLink(id: Int?)
    fun onRetry()
}
```

Annotated `@Inject @ViewModelKey @ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())`, exactly as hr's `RolePickerViewModel`.

Behaviour:
- `onInputChange`: keep at most `MAX_INPUT_CHARS` chars; if the last kept char is a high surrogate, drop it too; `truncated` = whether anything was cut. Sets `input`, `detecting = true`, clears `selectedId`.
- `onToggleOption`: flips the option in `options` (added if absent, removed if present); any combination is allowed, including the empty set. Sets `detecting = true`, clears `selectedId`.
- A private `MutableStateFlow<Request(text, options, attempt: Int)>` feeds `debounce { if (it.text.isEmpty()) 0L else INPUT_DEBOUNCE_MS }.collectLatest { ... }`, launched from `init` in `viewModelScope` (`@OptIn(FlowPreview::class)`). Inside: if the current result is `Failed`, set `Loading`; call the use case; `Success` becomes `UiState.Ready(DetectionUi(result.text, result.links))`, `Failure` becomes `UiState.Failed(message)`; then `detecting = false`. `collectLatest` cancels a superseded detection, so its result is never published.
- `onRetry`: re-emit the current request with `attempt + 1`.
- `onSelectLink(id)`: sets `selectedId` only if a Ready link has that id, or `id == null`.
- `screenStateOf`: `Failed` gives `Error`; `Loading` gives `Loading`; `Ready` with blank `analyzedText` gives `Loading` when `detecting && input.isNotBlank()`, else `Empty`; `Ready` with no links gives `NoLinks`; otherwise `Content`.

- [ ] **Step 1: Write the failing tests.** `DetektorViewModelTest` installs `Dispatchers.setMain(StandardTestDispatcher())` in `@BeforeTest` (reset in `@AfterTest`), builds the use case with `StandardTestDispatcher(testScheduler)`, and uses turbine on `vm.state`. A helper `vm(engine: LinkEngine = LibraryLinkEngine())` constructs both with the test scheduler.

```kotlin
@Test fun `typing detects after the debounce, not before`() = runTest {
    val vm = vm()
    vm.onInputChange("go to https://example.com now")
    advanceTimeBy(INPUT_DEBOUNCE_MS - 1); runCurrent()
    assertTrue(vm.state.value.detecting)
    assertEquals(DetectionUi.Empty, (vm.state.value.result as UiState.Ready).value)
    advanceUntilIdle()
    val ready = (vm.state.value.result as UiState.Ready).value
    assertEquals("go to https://example.com now", ready.analyzedText)
    assertEquals(1, ready.links.size)
    assertFalse(vm.state.value.detecting)
}

@Test fun `a newer input discards the older result`() = runTest {
    var calls = 0
    val vm = vm { content, options -> calls++; LibraryLinkEngine().detect(content, options) }
    vm.onInputChange("a.com")
    advanceTimeBy(100)
    vm.onInputChange("b.com and c.com")
    advanceUntilIdle()
    assertEquals(1, calls)
    val ready = (vm.state.value.result as UiState.Ready).value
    assertEquals("b.com and c.com", ready.analyzedText)
    ready.links.forEach { assertTrue(it.endExclusive <= ready.analyzedText.length) }
}

@Test fun `input over the cap is cut and flagged`() = runTest {
    val vm = vm()
    vm.onInputChange("a".repeat(MAX_INPUT_CHARS + 5))
    assertEquals(MAX_INPUT_CHARS, vm.state.value.input.length)
    assertTrue(vm.state.value.truncated)
    vm.onInputChange("short")
    assertFalse(vm.state.value.truncated)
}

@Test fun `the cut never splits a surrogate pair`() = runTest {
    val vm = vm()
    vm.onInputChange("a".repeat(MAX_INPUT_CHARS - 1) + "😀")
    assertEquals(MAX_INPUT_CHARS - 1, vm.state.value.input.length)
    assertFalse(vm.state.value.input.last().isHighSurrogate())
    assertTrue(vm.state.value.truncated)
}

@Test fun `toggling options flips the set and any combination detects`() = runTest {
    val seen = mutableListOf<ImmutableSet<LinksDetektorOptions>>()
    val vm = vm { content, options -> seen.add(options); LibraryLinkEngine().detect(content, options) }
    vm.onInputChange("\"http://a.com\" 'http://b.com'")
    advanceUntilIdle()
    vm.onToggleOption(QUOTE_MATCH); vm.onToggleOption(SINGLE_QUOTE_MATCH)
    advanceUntilIdle()
    assertEquals(persistentSetOf(QUOTE_MATCH, SINGLE_QUOTE_MATCH), vm.state.value.options)
    assertEquals(persistentSetOf(QUOTE_MATCH, SINGLE_QUOTE_MATCH), seen.last())
    val ready = (vm.state.value.result as UiState.Ready).value
    assertEquals(listOf("http://a.com", "http://b.com"), ready.links.map { it.original.full })   // both quoted URLs detected
    vm.onToggleOption(QUOTE_MATCH); vm.onToggleOption(SINGLE_QUOTE_MATCH)
    assertEquals(persistentSetOf(), vm.state.value.options)                                      // empty set is Default in the library
}

@Test fun `a failure shows the error and retry recovers`() = runTest {
    var fail = true
    val vm = vm { content, options -> if (fail) error("boom") else LibraryLinkEngine().detect(content, options) }
    vm.state.test {
        skipItems(1)
        vm.onInputChange("example.com")
        advanceUntilIdle()
        assertEquals(UiState.Failed("That text could not be checked."), expectMostRecentItem().result)
        fail = false
        vm.onRetry()
        runCurrent()
        assertEquals(UiState.Loading, expectMostRecentItem().result)
        advanceUntilIdle()
        assertIs<UiState.Ready<DetectionUi>>(expectMostRecentItem().result)
    }
}

@Test fun `selection follows the results`() = runTest {
    val vm = vm()
    vm.onInputChange("a.com b.com"); advanceUntilIdle()
    vm.onSelectLink(1)
    assertEquals(1, vm.state.value.selected?.id)
    vm.onSelectLink(7)
    assertEquals(1, vm.state.value.selectedId)
    vm.onInputChange("a.com")
    assertNull(vm.state.value.selectedId)
}

@Test fun `clearing the input returns to empty at once`() = runTest {
    val vm = vm()
    vm.onInputChange("a.com"); advanceUntilIdle()
    vm.onInputChange(""); runCurrent()
    assertEquals(DetektorScreenState.Empty, screenStateOf(vm.state.value))
}
```

`DetektorScreenStateTest`: one test, `screen state table`, asserting `screenStateOf` for six hand-built states: default gives `Empty`; `input = "x", detecting = true` with Ready-empty gives `Loading`; `result = Loading` gives `Loading`; Ready with `analyzedText = "hello"` and no links gives `NoLinks`; Ready with one link gives `Content`; `Failed("m")` gives `Error`.

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew :feature:detektor:client:basic:jvmTest`
Expected: FAIL, unresolved `DetektorViewModel`.

- [ ] **Step 3: Implement `DetektorUiState.kt` and `DetektorViewModel.kt`** to the signatures and behaviour above.

- [ ] **Step 4: Verify**

Run: `./gradlew :feature:detektor:client:basic:jvmTest :feature:detektor:client:basic:compileKotlinWasmJs detektCheck`
Expected: `BUILD SUCCESSFUL`, 15 tests passed in the module.

- [ ] **Step 5: Commit**

```bash
git add feature/detektor/client/basic
git commit -m "Add detektor view model with debounce and input cap"
```

---

### Task 11: Highlight and detail-row builders

**Files:**
- Create: `feature/detektor/client/basic/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/feature/detektor/basic/presentation/Highlight.kt`, `DetailRows.kt`
- Test: `feature/detektor/client/basic/src/commonTest/kotlin/dev/slothiesmooth/linksdetektor/feature/detektor/basic/presentation/HighlightTest.kt`, `DetailRowsTest.kt`

**Interfaces:**
- Consumes: `DetectedLink`, `LinkParts` (Task 9).
- Produces:

```kotlin
const val LINK_TAG = "link"

@Immutable data class HighlightStyle(val link: SpanStyle, val selected: SpanStyle)

/** [text] with one "link" string annotation (the link id) and one span per valid link range; invalid ranges are skipped. */
fun buildHighlight(text: String, links: ImmutableList<DetectedLink>, selectedId: Int?, style: HighlightStyle): AnnotatedString

@Immutable data class PartRow(val label: String, val original: String, val normalized: String?, val changed: Boolean)
fun detailRows(link: DetectedLink): ImmutableList<PartRow>
```

Decisions:
- A range is valid when `0 <= start < endExclusive <= text.length` and `start >=` the previous accepted range's `endExclusive`. Links are processed in list order.
- Each accepted range gets `addStringAnnotation(LINK_TAG, id.toString(), start, endExclusive)` and `style.selected` when `id == selectedId`, else `style.link`.
- `detailRows` order and labels: `Full URL`, `Scheme`, `Username`, `Password`, `Host`, `Port`, `Path`, `Query`, `Fragment`. `Port` shows the number as text. A row whose original and normalized values are both empty is omitted, except `Full URL`, `Host` and `Port`. `normalized` is null in every row when `link.normalized` is null. `changed = normalized != null && normalized != original`.

- [ ] **Step 1: Write the failing tests.** A helper `link(id, start, end, host = "h")` builds a `DetectedLink` with `LinkParts` filled from the arguments. `style` uses two distinguishable `SpanStyle`s (`fontWeight = Bold` and `fontWeight = Black`).

```kotlin
@Test fun `each link gets one span and one annotation carrying its id`() {
    val text = "see a.com and b.com"
    val out = buildHighlight(text, persistentListOf(link(0, 4, 9), link(1, 14, 19)), selectedId = 1, style)
    assertEquals(text, out.text)
    val tags = out.getStringAnnotations(LINK_TAG, 0, text.length)
    assertEquals(persistentListOf("0" to (4 to 9), "1" to (14 to 19)), tags.map { it.item to (it.start to it.end) }.toImmutableList())
    assertEquals(style.link, out.spanStyles[0].item)
    assertEquals(style.selected, out.spanStyles[1].item)
}

@Test fun `ranges outside the text are skipped`() {
    val out = buildHighlight("short", persistentListOf(link(0, 2, 99), link(1, -1, 3), link(2, 3, 3), link(3, 1, 4)), null, style)
    assertEquals(persistentListOf("3"), out.getStringAnnotations(LINK_TAG, 0, 5).map { it.item }.toImmutableList())
}

@Test fun `an overlapping range is skipped and the earlier one kept`() {
    val out = buildHighlight("0123456789", persistentListOf(link(0, 0, 5), link(1, 3, 8), link(2, 5, 9)), null, style)
    assertEquals(persistentListOf("0", "2"), out.getStringAnnotations(LINK_TAG, 0, 10).map { it.item }.toImmutableList())
}

@Test fun `no links gives plain text`() {
    val out = buildHighlight("plain", persistentListOf(), null, style)
    assertEquals(0, out.spanStyles.size)
}
```

```kotlin
@Test fun `rows compare original and normalized and mark changes`() {
    val rows = detailRows(DetectedLink(0, 0, 0,
        original = LinkParts("HTTP://Example.COM/a/../b", "http", "", "", "Example.COM", 80, "/a/../b", "", ""),
        normalized = LinkParts("http://example.com/b", "http", "", "", "example.com", 80, "/b", "", "")))
    assertEquals(persistentListOf("Full URL", "Scheme", "Host", "Port", "Path"), rows.map { it.label }.toImmutableList())
    assertEquals(PartRow("Host", "Example.COM", "example.com", changed = true), rows[2])
    assertEquals(PartRow("Port", "80", "80", changed = false), rows[3])
    assertEquals(PartRow("Scheme", "http", "http", changed = false), rows[1])
}

@Test fun `a link that could not be normalized has no normalized column`() {
    val rows = detailRows(DetectedLink(0, 0, 0, LinkParts("x.com", "http", "", "", "x.com", 80, "", "", ""), normalized = null))
    assertTrue(rows.all { it.normalized == null && !it.changed })
}
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew :feature:detektor:client:basic:jvmTest`
Expected: FAIL, unresolved `buildHighlight`.

- [ ] **Step 3: Implement both files** to the signatures and decisions above.

- [ ] **Step 4: Verify**

Run: `./gradlew :feature:detektor:client:basic:jvmTest detektCheck`
Expected: `BUILD SUCCESSFUL`, 21 tests passed in the module.

- [ ] **Step 5: Commit**

```bash
git add feature/detektor/client/basic
git commit -m "Add highlight and detail row builders"
```

---

### Task 12: Detektor screen, provider, and wiring into the app

**Files:**
- Create under `feature/detektor/client/basic/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/feature/detektor/basic/presentation/`: `DetektorScreen.kt`, `DetektorPanes.kt`
- Create: `feature/detektor/client/basic/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/feature/detektor/basic/BasicDetektorProvider.kt`
- Test: `feature/detektor/client/basic/src/jvmTest/kotlin/dev/slothiesmooth/linksdetektor/feature/detektor/basic/presentation/DetektorScreenTest.kt`
- Modify: `client/build.gradle.kts` (add `api(projects.feature.detektor.client.basic)`), `client/src/jvmTest/kotlin/dev/slothiesmooth/linksdetektor/client/AppSmokeTest.kt`
- Modify: `webApp/src/wasmJsTest/kotlin/dev/slothiesmooth/linksdetektor/web/WebAppGraphTest.kt`

**Interfaces:**
- Consumes: `DetektorViewModel`, `DetektorUiState`, `screenStateOf`, `ToggleableOptions` (Task 10); `buildHighlight`, `detailRows`, `LINK_TAG` (Task 11); design-library components (Task 4); `MetroFeatureProvider`, `metroFeatureViewModel`, `DetektorFeature`, `DetektorRoute` (Task 5).
- Produces:

```kotlin
@Composable fun DetektorScreen(viewModel: DetektorViewModel)        // collects state, delegates
@Composable fun DetektorContent(state: DetektorUiState, onInputChange: (String) -> Unit,
    onToggleOption: (LinksDetektorOptions) -> Unit, onSelectLink: (Int?) -> Unit, onRetry: () -> Unit,
    modifier: Modifier = Modifier)                                   // stateless; what the UI test drives
@Inject @ContributesIntoSet(AppScope::class, binding = binding<FeatureProvider>())
class BasicDetektorProvider : MetroFeatureProvider(), DetektorFeature
```

`DetektorPanes.kt` holds the internal dumb composables: `InputPane`, `OptionChips`, `HighlightPane`, `ResultList`, `DetailPane`.

Layout and copy (exact strings):
- Screen column, max width 1360dp, 16dp gutter (24dp from 720dp wide), vertical scroll owned by the result list, not the page.
- Heading `LdText("Links Detektor", style = Title)`; below it `LdText("Paste any text. Every link in it is found and taken apart.", style = BodySm, color = dim)`.
- `InputPane`: `LdTextField(label = "Your text", placeholder = "Paste or type text with links in it", minLines = 4, maxLines = 10)` tagged `detektor-input`. When `state.truncated`: `LdText("Only the first 20,000 characters are checked.", style = Caption, color = amber)`.
- `OptionChips`: a `FlowRow` of `LdChip` per `ToggleableOptions` entry, tagged `option-<NAME>`. Labels: `QUOTE_MATCH` "Double quotes", `SINGLE_QUOTE_MATCH` "Single quotes", `BRACKET_MATCH` "Brackets", `JSON` "JSON", `JAVASCRIPT` "JavaScript", `XML` "XML", `HTML` "HTML", `ALLOW_SINGLE_LEVEL_DOMAIN` "Single-word hosts".
- Body by `screenStateOf(state)`:
  - `Empty`: `LdEmptyState("Nothing to check yet", "Paste some text above and the links in it show up here.")`
  - `Loading`: `LdLoadingState("Checking…")`
  - `NoLinks`: `LdEmptyState("No links found", "Try another option, or text with a web address in it.")`
  - `Error`: `LdErrorState("Could not check that text", message, onRetry)`
  - `Content`: `LdMasterDetail(chosen = state.selected != null, onClose = { onSelectLink(null) }, master = { HighlightPane + ResultList in a column }, detail = { DetailPane(state.selected) }, placeholderTitle = "Pick a link", placeholderMessage = "Its parts show here, before and after tidying.")`
- `HighlightPane` (tag `detektor-highlight`): an `LdPanel` showing `buildHighlight(analyzedText, links, selectedId, style)` in `Body` style, where `style.link = SpanStyle(color = amber, fontFamily = mono, textDecoration = Underline)` and `style.selected = SpanStyle(color = amberInk, background = amber, fontFamily = mono)`. A tap resolves the offset through `getStringAnnotations(LINK_TAG, offset, offset)` and calls `onSelectLink(id)`. The pane always renders `analyzedText`, never `state.input`.
- `ResultList` (tag `detektor-results`): heading `LdText("${links.size} links found", style = Label)` ("1 link found" for one), then a `LazyColumn` of rows, each a clickable ruled row (1dp `line` divider) with `original.full` in `Code` style and `original.host` in `Caption`/`dim`; the selected row has a `surface2` fill. Row tag `result-<id>`; `Role.Button`; `ldFocusRing`.
- `DetailPane` (tag `detektor-detail`): an `LdPanel` with a three-column table from `detailRows(link)`: label (`Label`, `dim`), `Original` and `Tidied` column headers (`Caption`, `dim`), values in `Code` style; a `changed` row draws its normalized value in `amber`. When `link.normalized == null`: `LdText("This link could not be tidied.", style = BodySm, color = dim)` in place of the second column. In the narrow sheet a Ghost `LdButton("Close")` sits under the table.
- `BasicDetektorProvider`: `onProvideContent` calls `content()`; `onProvideNavigation` registers `builder.composable<DetektorRoute> { DetektorScreen(viewModel = metroFeatureViewModel()) }`.

- [ ] **Step 1: Write the failing Compose UI test** (`runComposeUiTest`, content wrapped in `LdTheme` and a sized `Box`; `ready(text)` builds a state by running `DetectLinksUseCase(LibraryLinkEngine(), UnconfinedTestDispatcher())` under `runBlocking`).

```kotlin
private val sample = "docs at https://example.com/a and HTTP://Sub.Example.ORG/x"

@Test fun `empty input shows the prompt`() = runComposeUiTest {
    setContent { host(DetektorUiState()) }
    onNodeWithText("Nothing to check yet").assertIsDisplayed()
    onNodeWithTag("detektor-results").assertDoesNotExist()
}

@Test fun `typing reaches the callback`() = runComposeUiTest {
    var typed = ""
    setContent { host(DetektorUiState(), onInputChange = { typed = it }) }
    onNodeWithTag("detektor-input").performTextInput("a.com")
    assertEquals("a.com", typed)
}

@Test fun `results list every link and the count`() = runComposeUiTest {
    setContent { host(ready(sample), width = 1000.dp) }
    onNodeWithText("2 links found").assertIsDisplayed()
    onNodeWithTag("result-0").assertTextContains("https://example.com/a")
    onNodeWithTag("result-1").assertIsDisplayed()
    onNodeWithTag("detektor-highlight").assertTextContains(sample)
    onNodeWithText("Pick a link").assertIsDisplayed()
}

@Test fun `picking a row asks for that link and wide shows its detail beside the list`() = runComposeUiTest {
    var state by mutableStateOf(ready(sample))
    setContent { host(state, width = 1000.dp, onSelectLink = { state = state.copy(selectedId = it) }) }
    onNodeWithTag("result-1").performClick()
    onNodeWithTag("detektor-detail").assertIsDisplayed()
    onNodeWithTag("detektor-results").assertIsDisplayed()
    onNodeWithTag("detektor-detail").onChildren().assertAny(hasText("sub.example.org"))
    onNodeWithText("Tidied").assertIsDisplayed()
}

@Test fun `narrow shows the detail in a sheet that can be closed`() = runComposeUiTest {
    var state by mutableStateOf(ready(sample).copy(selectedId = 0))
    setContent { host(state, width = 400.dp, onSelectLink = { state = state.copy(selectedId = it) }) }
    onNodeWithTag("detektor-detail").assertIsDisplayed()
    onNodeWithText("Close").performClick()
    waitForIdle()
    onNodeWithTag("detektor-detail").assertDoesNotExist()
}

@Test fun `option chips reflect the set and report toggles`() = runComposeUiTest {
    var toggled: LinksDetektorOptions? = null
    setContent { host(DetektorUiState(options = persistentSetOf(QUOTE_MATCH, SINGLE_QUOTE_MATCH)), onToggleOption = { toggled = it }) }
    onNodeWithTag("option-QUOTE_MATCH").assertIsOn()
    onNodeWithTag("option-HTML").assertIsOff().performClick()
    assertEquals(HTML, toggled)
}

@Test fun `loading, no links, error and the cap notice each show their copy`() = runComposeUiTest {
    var state by mutableStateOf(DetektorUiState(input = "x", detecting = true))
    var retried = 0
    setContent { host(state, onRetry = { retried++ }) }
    onNodeWithText("Checking…").assertIsDisplayed()
    state = DetektorUiState(input = "hello", result = UiState.Ready(DetectionUi("hello", persistentListOf())))
    onNodeWithText("No links found").assertIsDisplayed()
    state = DetektorUiState(input = "x", truncated = true, result = UiState.Failed("That text could not be checked."))
    onNodeWithText("Only the first 20,000 characters are checked.").assertIsDisplayed()
    onNodeWithText("Try again").performClick()
    assertEquals(1, retried)
}

@Test fun `stale ranges never crash the highlight`() = runComposeUiTest {
    val stale = ready(sample).let { s -> s.copy(result = UiState.Ready(DetectionUi("tiny", (s.result as UiState.Ready).value.links))) }
    setContent { host(stale, width = 1000.dp) }
    onNodeWithTag("detektor-highlight").assertTextContains("tiny")
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew :feature:detektor:client:basic:jvmTest`
Expected: FAIL, unresolved `DetektorContent`.

- [ ] **Step 3: Implement `DetektorPanes.kt`, `DetektorScreen.kt`, `BasicDetektorProvider.kt`** to the layout and copy above.

- [ ] **Step 4: Wire the feature into `:client` and extend the smoke tests.** Add the `basic` dependency to `client/build.gradle.kts`. In `AppSmokeTest.kt`, replace `the app root composes with no feature installed` with:

```kotlin
@Test fun `the detektor feature is contributed and ordered`() {
    val graph = createGraph<TestAppGraph>()
    val sorted = sortFeatures(graph.features.toImmutableSet(), graph.featureOrder)
    assertEquals(1, sorted.size)
    assertIs<DetektorFeature>(sorted.single())
}

@Test fun `typing text with a link shows it in the results`() = runComposeUiTest {
    val graph = createGraph<TestAppGraph>()
    setContent { App(graph) }
    onNodeWithTag("detektor-input").performTextInput("read https://example.com/docs today")
    waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("result-0").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithText("1 link found").assertExists()
    onNodeWithTag("result-0").assertTextContains("https://example.com/docs")
}
```

In `WebAppGraphTest.kt` add `assertEquals(1, graph.features.size)` to the existing test, so the Metro contribution path is proven on wasm.

- [ ] **Step 5: Verify**

Run: `./gradlew :feature:detektor:client:basic:jvmTest :client:jvmTest :webApp:wasmJsBrowserTest :desktopApp:compileKotlin :client:compileKotlinIosSimulatorArm64 :client:compileAndroidMain detektCheck`
Expected: `BUILD SUCCESSFUL`; 29 tests in the feature module, 5 in `:client`, 1 in `:webApp`.
Run: `./gradlew :desktopApp:run`
Expected: paste `see https://example.com/a?b=1 and "http://localhost:8080"`; one link is listed until "Single-word hosts" is pressed, then two; clicking a row shows the two-column detail; shrinking the window under 720dp moves the detail into a sheet. Close the window.
Run: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
Expected: the same behaviour in the browser; typing stays responsive; fonts render as Geist with URLs in Geist Mono; no console errors.

- [ ] **Step 6: Commit**

```bash
git add feature/detektor/client/basic client webApp
git commit -m "Add detektor screen and wire the feature into the app"
```

---

### Task 13: Full gate, wasm size measurement, and CI

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `README.md` (one "Showcase app" section: the two run commands and the live URL)

**Interfaces:**
- Consumes: `scripts/wasm-size.mjs` (Task 8); the library plan's `ci.yml` (an ubuntu job running `detektCheck` and the library tests, and a macOS job running the Apple tests). Read the file first; add to those jobs rather than creating parallel ones.
- Produces: CI artifact `links-detektor-web` holding the production distribution; a failing check when the brotli total exceeds 5 MB.

- [ ] **Step 1: Run the whole gate locally**

Run: `./gradlew build detektCheck --parallel`
Expected: `BUILD SUCCESSFUL` (all host tests on every target this machine can run, including `wasmJsBrowserTest` and `iosSimulatorArm64Test`).

- [ ] **Step 2: Measure the web payload**

Run: `./gradlew :webApp:wasmJsBrowserDistribution && node scripts/wasm-size.mjs webApp/build/dist/wasmJs/productionExecutable --budget-mb 5`
Expected: exit code 0 and a brotli total at or under 5 MB (the spec expects 10-14 MB raw, mostly skiko). Record raw, gzip and brotli totals in the commit body. If the total is over budget, do not raise the budget: report the per-file table to the owner and stop.

- [ ] **Step 3: Extend the ubuntu job in `ci.yml`.** After the library steps add, in this order: a Gradle step `./gradlew jvmTest :webApp:wasmJsBrowserTest :webApp:wasmJsBrowserDistribution :desktopApp:compileKotlin` (ubuntu-latest ships Chrome for Karma); `actions/setup-node` with `node-version: 22`; `node --test scripts/wasm-size.test.mjs`; `node scripts/wasm-size.mjs webApp/build/dist/wasmJs/productionExecutable --budget-mb 5`; `actions/upload-artifact` named `links-detektor-web` with path `webApp/build/dist/wasmJs/productionExecutable`. In the macOS job append `:client:compileKotlinIosSimulatorArm64` to the Gradle command.

- [ ] **Step 4: Verify the workflow file parses**

Run: `ruby -ryaml -e 'YAML.load_file(".github/workflows/ci.yml"); puts "ok"'`
Expected: `ok`.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/ci.yml README.md
git commit -m "Build and size-check the web app in CI"
```

---

### Task 14: The web Worker, local verification, and the two routing questions

**Files:**
- Create: `deploy/web/wrangler.jsonc`, `deploy/web/package.json`, `deploy/web/package-lock.json` (generated), `deploy/web/README.md`
- Modify: `.gitignore` (add `deploy/web/node_modules/`, `deploy/web/.wrangler/`)

**Interfaces:**
- Consumes: `webApp/build/dist/wasmJs/productionExecutable/` (Task 8).
- Produces: a Worker named `links-detektor-web` serving the distribution at `/links-detektor/`; npm scripts `dev` and `deploy` in `deploy/web`; a written decision in `deploy/web/README.md` under the headings `## Routing decision` and `## Cross-origin isolation` that Tasks 15 and 16 read.

- [ ] **Step 1: Create the Worker project**

```bash
cd deploy/web && npm init -y && npm install --save-dev wrangler@^4.148.0
```

Set `"private": true`, `"type": "module"`, `"engines": { "node": ">=22.12.0" }`, and scripts `"dev": "wrangler dev"`, `"deploy": "wrangler deploy"`. Wrangler 4.148.0 is the first release with `assets.base_path`; confirm with `npx wrangler --version`.

- [ ] **Step 2: Write `wrangler.jsonc`**

```jsonc
{
  "$schema": "node_modules/wrangler/config-schema.json",
  "name": "links-detektor-web",
  "compatibility_date": "2026-09-01",
  // Assets only: no Worker script. Files sit at the root of the directory; base_path mounts them
  // under /links-detektor at serve time, so the same build also works at any other prefix.
  "assets": {
    "directory": "../../webApp/build/dist/wasmJs/productionExecutable",
    "base_path": "/links-detektor"
  },
  "workers_dev": true,
  "observability": { "enabled": true }
}
```

- [ ] **Step 3: Verify subpath serving locally**

Run: `./gradlew :webApp:wasmJsBrowserDistribution`, then in `deploy/web`: `npm run dev` (serves on `http://localhost:8787`). In a second shell:

```bash
curl -s -o /dev/null -w '%{http_code} %{content_type}\n' http://localhost:8787/links-detektor/
curl -s -o /dev/null -w '%{http_code} %{redirect_url}\n' http://localhost:8787/links-detektor
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8787/links-detektor/composeApp.js
curl -s -o /dev/null -w '%{http_code} %{content_type}\n' "http://localhost:8787/links-detektor/$(cd ../../webApp/build/dist/wasmJs/productionExecutable && ls *.wasm | head -1)"
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8787/
```

Expected, in order: `200 text/html...`; `307` or `308` redirecting to `/links-detektor/` (if it answers 200 instead, relative assets would break: add a `_redirects` file to `webApp/src/wasmJsMain/resources/` with `/links-detektor /links-detektor/ 308` and re-run); `200`; `200 application/wasm`; `404`.
Then open `http://localhost:8787/links-detektor/` in Chrome, Firefox and Safari: the app loads and detects links, the Network panel shows every request under `/links-detektor/` (including `composeResources/` fonts), nothing is requested from `/`, and there is no white flash on a hard reload.

- [ ] **Step 4: Answer "does Compose wasm need COOP/COEP?"**

Run: `curl -sI http://localhost:8787/links-detektor/ | grep -i 'cross-origin' || echo "no isolation headers"`
Expected: `no isolation headers`. In the browser console on the loaded app, `crossOriginIsolated` is `false` and there is no `SharedArrayBuffer` error.
Decision rule: if the app works in all three browsers with no isolation headers, write under `## Cross-origin isolation`: "Not needed: the app runs without COOP/COEP (checked <date>, browsers and versions)". If any browser fails with a `SharedArrayBuffer` or isolation error, add `webApp/src/wasmJsMain/resources/_headers` containing a `/links-detektor/*` block with `Cross-Origin-Opener-Policy: same-origin` and `Cross-Origin-Embedder-Policy: require-corp`, re-run Step 3, and record that instead.

- [ ] **Step 5: First deploy to workers.dev (owner step: needs Cloudflare credentials)**

Run in `deploy/web`: `npx wrangler login` (once), then `npm run deploy`.
Expected: output ends with a `https://links-detektor-web.<account>.workers.dev` URL; `curl -s -o /dev/null -w '%{http_code}\n' https://links-detektor-web.<account>.workers.dev/links-detektor/` prints `200`. Nothing on `technowolf.in` has changed.

- [ ] **Step 6: Answer "does a more-specific zone route suffice?" (owner step: touches the live zone)**

Add to `wrangler.jsonc`: `"routes": [{ "pattern": "technowolf.in/links-detektor*", "zone_name": "technowolf.in" }]`, run `npm run deploy`, then:

```bash
curl -s -o /dev/null -w '%{http_code}\n' https://technowolf.in/links-detektor/
curl -s https://technowolf.in/links-detektor/ | grep -c '<title>Links Detektor</title>'
curl -s https://technowolf.in/ | grep -c 'TechnoWolf'
curl -s -o /dev/null -w '%{http_code}\n' https://technowolf.in/build/
```

Decision rule:
- **Route suffices** when the four commands print `200`, `1`, a number above 0, and `200`: the app answers under its path and the site is untouched. Keep the `routes` entry, record "Zone route: sufficient, no forwarding in the site Worker" under `## Routing decision`, and mark Task 16 as **skipped** in this plan.
- **Route does not suffice** when the app path still returns the site's 404 page (the site's own domain binding wins) or the site breaks: remove the `routes` entry, run `npm run deploy` again, re-run the last two curl commands to confirm the site is intact, record "Zone route: insufficient because <observed result>; using service-binding forwarding", and do Task 16.
- In both cases also record whether an assets-only Worker was accepted as a service-binding target when Task 16 runs; if it is not, add `"main": "worker.js"` with `export default { fetch: (request, env) => env.ASSETS.fetch(request) };` and `"binding": "ASSETS"` to `assets`.

- [ ] **Step 7: Write `deploy/web/README.md`.** Sections: what this Worker is (two sentences), `## Local run` (the Gradle and npm commands above), `## Deploy` (CI does it; manual command), `## Routing decision`, `## Cross-origin isolation`, `## Rollback` (`npx wrangler rollback` in this folder; the site is unaffected).

- [ ] **Step 8: Commit**

```bash
git add .gitignore deploy/web
git commit -m "Add Cloudflare Worker for the web app"
```

---

### Task 15: Deploy on release and attach the web zip

**Files:**
- Modify: `.github/workflows/master.yml`

**Interfaces:**
- Consumes: the library plan's `master.yml`. Read it first. This task needs two values from its release job, exposed as job outputs (add the `outputs:` block if absent): `version` (release-drafter `resolved_version`, pre-release suffix included, e.g. `3.0.0-alpha2`) and `upload_url` (release-drafter `upload_url`). It also needs the id of the job that publishes to Maven Central, called `<publish-job>` below.
- Produces: a job `web` that runs on every release this workflow makes (stable and pre-release alike), deploys the Worker, and attaches `links-detektor-web-<version>.zip` and `links-detektor-web-<version>.zip.sha256` to the GitHub release. Repository secrets `CLOUDFLARE_API_TOKEN` (permission: Workers Scripts Edit, plus Workers Routes Edit on the zone if Task 14 kept the route) and `CLOUDFLARE_ACCOUNT_ID`.

- [ ] **Step 1: Add the `web` job** with `needs: <publish-job>`, `runs-on: ubuntu-latest`, `permissions: { contents: write }`, `env: { VERSION: ${{ needs.<publish-job>.outputs.version }} }`, and no `if:` condition on the version (pre-releases deploy too). Steps, in order:
  1. `actions/checkout`, `actions/setup-java` (temurin 21), `gradle/actions/setup-gradle`, `actions/setup-node` (`node-version: 22`, `cache: npm`, `cache-dependency-path: deploy/web/package-lock.json`) at the versions the file already uses.
  2. `./gradlew :webApp:wasmJsBrowserDistribution`
  3. `node scripts/wasm-size.mjs webApp/build/dist/wasmJs/productionExecutable --budget-mb 5`
  4. Package:

```bash
cd webApp/build/dist/wasmJs/productionExecutable
zip -r "$GITHUB_WORKSPACE/links-detektor-web-$VERSION.zip" .
cd "$GITHUB_WORKSPACE"
sha256sum "links-detektor-web-$VERSION.zip" > "links-detektor-web-$VERSION.zip.sha256"
```

  5. Two `shogo82148/actions-upload-release-asset@v1` steps (the action the workflow already uses) with `upload_url: ${{ needs.<publish-job>.outputs.upload_url }}`, one per file, `overwrite: true`.
  6. Deploy: `working-directory: deploy/web`, `run: npm ci && npx wrangler deploy`, with `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` from secrets.
  7. Smoke check: `curl -fsS -o /dev/null https://technowolf.in/links-detektor/` when Task 14 recorded a working public URL; otherwise the workers.dev URL from `deploy/web/README.md`.

- [ ] **Step 2: Add `workflow_dispatch:`** to the workflow's `on:` block only if a manual run would not re-publish to Maven Central; if the publish job has no guard, skip this step and note in `deploy/web/README.md` that a manual redeploy is `npm run deploy` from a checkout of the release tag.

- [ ] **Step 3: Verify**

Run: `ruby -ryaml -e 'YAML.load_file(".github/workflows/master.yml"); puts "ok"'`
Expected: `ok`.
Run locally, to prove the packaging commands: `VERSION=0.0.0-test GITHUB_WORKSPACE=$PWD bash -c 'cd webApp/build/dist/wasmJs/productionExecutable && zip -qr "$GITHUB_WORKSPACE/build/links-detektor-web-$VERSION.zip" . && cd "$GITHUB_WORKSPACE/build" && shasum -a 256 "links-detektor-web-$VERSION.zip" && unzip -l "links-detektor-web-$VERSION.zip" | grep -c "index.html"'`
Expected: a 64-hex digest line and `1` (`index.html` is at the zip root, not under a folder).
After the first release run: the GitHub release lists both files; `sha256sum -c links-detektor-web-<version>.zip.sha256` on a download prints `OK`; the Worker's deployment list in the Cloudflare dashboard shows a new version.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/master.yml
git commit -m "Deploy the web app and attach its zip on release"
```

---

### Task 16: [WEBSITE REPO `/Users/slothie/Desktop/technowolf-website`] Forward `/links-detektor/*` through a service binding

**Skip this task if `deploy/web/README.md` records "Zone route: sufficient".** All paths below are in the website repository, which has its own git history, `CLAUDE.md`, `DESIGN.md` and `PRODUCT.md`; follow them. The Worker `links-detektor-web` must already be deployed (Task 14 Step 5), or the site deploy fails on the unresolved binding.

**Files:**
- Create: `worker/forward.ts`; Test: `worker/forward.test.ts`
- Modify: `worker/index.ts`, `wrangler.jsonc`

**Interfaces:**
- Consumes: the deployed Worker `links-detektor-web`, which serves under `/links-detektor` itself (`assets.base_path`), so the request is forwarded unchanged: no prefix stripping and no HTML rewriting.
- Produces: `export function isLinksDetektorPath(pathname: string): boolean`; binding `LINKS_DETEKTOR: Fetcher` on `Env`.

- [ ] **Step 1: Write the failing test** (`node --test`, same style as `worker/redirects.test.ts`)

```ts
test("the app's own paths are forwarded", () => {
  for (const path of ["/links-detektor", "/links-detektor/", "/links-detektor/composeApp.js", "/links-detektor/composeResources/x/font/geist_regular.ttf"]) {
    assert.equal(isLinksDetektorPath(path), true, path);
  }
});

test("neighbouring paths are not", () => {
  for (const path of ["/", "/links", "/links-detektor-old", "/links-detektorx/", "/build/links-detektor/", "/api/links-detektor"]) {
    assert.equal(isLinksDetektorPath(path), false, path);
  }
});
```

Run: `npm test` — Expected: FAIL, cannot find `./forward`.

- [ ] **Step 2: Implement `worker/forward.ts`**: true when the pathname is exactly `/links-detektor` or starts with `/links-detektor/`.

- [ ] **Step 3: Forward in `worker/index.ts`.** Add `LINKS_DETEKTOR?: Fetcher;` to `Env`. In `fetch`, after the two `/api/` branches and before `redirectFor`: `if (isLinksDetektorPath(url.pathname)) { return env.LINKS_DETEKTOR ? env.LINKS_DETEKTOR.fetch(request) : env.ASSETS.fetch(request); }` (without the binding the site answers with its own 404 page instead of throwing).

- [ ] **Step 4: Bind and route in `wrangler.jsonc`.** Add `"services": [{ "binding": "LINKS_DETEKTOR", "service": "links-detektor-web" }]`, and extend `run_worker_first` to `["/api/*", "/apps-and-backend*", "/kotlin-multiplatform*", "/consulting*", "/links-detektor", "/links-detektor/*"]`, extending the comment above it to mention the forwarded app.

- [ ] **Step 5: Verify**

Run: `npm test && npm run check`
Expected: all worker tests pass; `tsc` reports no errors.
Run: in the links-detektor repo `cd deploy/web && npm run dev`; in the website repo `npm run preview`. Then against the site's local port:

```bash
curl -s http://localhost:<site-port>/links-detektor/ | grep -c '<title>Links Detektor</title>'
curl -s -o /dev/null -w '%{http_code} %{content_type}\n' http://localhost:<site-port>/links-detektor/composeApp.js
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:<site-port>/build/
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:<site-port>/links-detektor-old
```

Expected: `1`; `200` with a JavaScript content type; `200`; `404`. Open `/links-detektor/` through the site port in a browser and confirm the app works.

- [ ] **Step 6: Commit (website repo), then deploy (owner step)**

```bash
git add worker/forward.ts worker/forward.test.ts worker/index.ts wrangler.jsonc
git commit -m "Forward /links-detektor to its own Worker"
npm run deploy
```

After deploy: `curl -s https://technowolf.in/links-detektor/ | grep -c '<title>Links Detektor</title>'` prints `1`, and `https://technowolf.in/` still loads the site.

---

### Task 17: [WEBSITE REPO `/Users/slothie/Desktop/technowolf-website`] "Open source" section and `PRODUCT.md`

Do this only after `https://technowolf.in/links-detektor/` answers 200 (Task 14 route or Task 16), so the link is never dead.

**Files:**
- Modify: `src/pages/index.astro`, `PRODUCT.md`

**Interfaces:**
- Consumes: the existing `Rows` component (`rows: { title, body, href?, linkLabel? }[]`, `columns?: 2 | 3`), which already renders a ruled row with an amber "more" link and arrow.
- Produces: a homepage section `#open-source`. `src/data/nav.ts` is not changed, so the section is in neither the header menu nor the footer page list.

Rules that bind the copy and markup (from `PRODUCT.md` and `DESIGN.md`):
- Plain language outside technical bands: no "library", "Kotlin Multiplatform", "URL", "wasm" or other jargon in this section; sentences under about 20 words; no code.
- Honest by default: it is free open-source software, not a TechnoWolf product; do not call it a product, a launch, or proof of shipped products; invent no usage numbers.
- Ruled rows, not cards; the heading stands alone with no small uppercase label above it; no new pill button (the link is the existing amber "more" link); no new colour, no Geist Mono.

- [ ] **Step 1: Add the section to `src/pages/index.astro`.** In the frontmatter:

```ts
const openSource = [
  {
    title: "Links Detektor",
    body: "Finds every web link hidden in a piece of text and shows what each one is made of. Free for anyone to use.",
    href: "/links-detektor/",
    linkLabel: "Check it out live",
  },
];
```

Between the "Why TechnoWolf" section and `<section class="start" id="start">`:

```astro
<section class="section" id="open-source" aria-labelledby="open-source-title">
  <div class="section__head">
    <h2 id="open-source-title">Open source</h2>
    <p>Tools we built for our own work and share freely.</p>
  </div>
  <Rows rows={openSource} columns={2} />
</section>
```

Use the same head wrapper element and classes the neighbouring "Why TechnoWolf" section uses (read lines 193-199 of the file and match them exactly; `section__head` above is a placeholder for whatever that markup is). Add no new CSS unless the section head needs the same rule the neighbours already share.

- [ ] **Step 2: Update `PRODUCT.md`.**
  - Under "Capabilities and Constraints" add: "Open source: Links Detektor, a free tool that finds the web links inside text. It runs live at `/links-detektor/`, served by its own Cloudflare Worker from the links-detektor repository. It is open-source software, not a TechnoWolf product; never present it as a shipped product or as client proof."
  - Under "Evidence on Hand" replace "None." with: "One open-source tool with a live demo (Links Detektor, `/links-detektor/`)." and keep the rest of the paragraph.
  - Under "Site Structure" add after the numbered list: "The homepage also has an 'Open source' section (`/#open-source`). It is not in the header or footer navigation. `/links-detektor/` is not an Astro page: the request is answered by the separate `links-detektor-web` Worker."

- [ ] **Step 3: Verify**

Run: `npm run build`
Expected: the build succeeds.
Run: `grep -c 'id="open-source"' dist/index.html && grep -c 'Check it out live' dist/index.html && git diff --stat src/data/nav.ts src/components/SiteHeader.astro src/components/SiteFooter.astro`
Expected: `1`, `1`, and an empty diff stat (navigation untouched).
Run: `grep -ciE 'kotlin|wasm|library|url' <(sed -n '/id="open-source"/,/<\/section>/p' dist/index.html)`
Expected: `0`.
Run: `npm run preview` and look at the homepage at 1440px and 390px wide: the section reads as one ruled row with an amber arrow link, the heading has no label above it, nothing in the section is a pill or a card, and the header menu is unchanged.

- [ ] **Step 4: Commit (website repo), then deploy (owner step)**

```bash
git add src/pages/index.astro PRODUCT.md
git commit -m "Add open source section listing Links Detektor"
npm run deploy
```

After deploy: on `https://technowolf.in/#open-source`, "Check it out live" opens the running app.

---

## Verification of the whole plan

- [ ] `./gradlew build detektCheck --parallel` passes in the links-detektor repo.
- [ ] `grep -rnE '\b(mutableListOf|mutableSetOf|mutableMapOf|ArrayList|HashSet|HashMap)\b|!!' --include='*.kt' shared feature client desktopApp webApp` prints nothing.
- [ ] `grep -rnE ':\s*(List|Set|Map)<' --include='*.kt' shared feature client desktopApp webApp | grep -v '/src/.*Test/'` prints only `client/.../BaseAppGraph.kt` (one line) and `client/.../LdViewModelFactory.kt` (the Metro types).
- [ ] `https://technowolf.in/links-detektor/` loads the app with no white flash; `https://technowolf.in/links-detektor` redirects to the slash form; the site's other pages are unchanged.
- [ ] The latest GitHub release carries `links-detektor-web-<version>.zip` and its `.sha256`.
