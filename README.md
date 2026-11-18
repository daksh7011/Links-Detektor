# 🔍 Links Detektor

[![Maven Central](https://img.shields.io/maven-central/v/com.slothiesmooth/links-detektor.svg)](https://search.maven.org/search?q=g:com.slothiesmooth%20AND%20a:links-detektor)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.4.20-blue.svg)](https://kotlinlang.org)

🚀 **A robust library for detecting and extracting URLs from text content.** Links Detektor provides a powerful URL detection engine that can identify various URL formats within
arbitrary text.

**Platforms:** JVM 17+, Android 23+, iOS, macOS, JS, WasmJS, Linux and Windows (mingw). One Kotlin Multiplatform
library, one behaviour everywhere.

## ✨ Features

- 🔎 **Detects URLs** in plain text, JSON, XML, JavaScript, and HTML content
- 🌐 **Handles various URL formats**:
    - Standard URLs (http, https, ftp, ftps)
    - URLs with usernames and passwords
    - URLs with IPv4 addresses in various formats (decimal, hex, octal)
    - URLs with IPv6 addresses
    - URLs with international domain names
- 🧹 **Normalizes URLs** for consistent representation and comparison
- 🔓 **More lenient** than standard URL parsers (handles URLs that browsers accept but standard parsers reject)
- ⚙️ **Customizable detection** behavior through options

## 📦 Installation

> 💡 **Tip:** You can always check for the latest version on [Maven Central](https://central.sonatype.com/artifact/com.slothiesmooth/links-detektor)

<details open>
<summary><b>📋 Kotlin Multiplatform</b></summary>

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.slothiesmooth:links-detektor:<latest_version>")
        }
    }
}
```

</details>

<details>
<summary><b>📋 Gradle (Kotlin DSL, JVM or Android only)</b></summary>

```kotlin
dependencies {
    implementation("com.slothiesmooth:links-detektor:<latest_version>")
}
```

</details>

<details>
<summary><b>📋 Gradle (Groovy)</b></summary>

```groovy
dependencies {
    implementation 'com.slothiesmooth:links-detektor:<latest_version>'
}
```

</details>

<details>
<summary><b>📋 Maven</b></summary>

```xml

<dependency>
    <groupId>com.slothiesmooth</groupId>
    <artifactId>links-detektor-jvm</artifactId>
    <version>latest_version</version>
</dependency>
```

Maven does not read Gradle module metadata, so it cannot pick the JVM variant of the multiplatform root artifact;
depend on the `-jvm` artifact directly.

</details>

## 🚀 Quick Start

<div style="text-align: center;">

### 🏃‍♂️ Get started in just a few lines of code!

</div>

```kotlin
// Sample text containing URLs
val text = "Check out these websites: https://www.example.com and http://github.com/some-repo"

// Create a LinksDetektor instance with HTML options
val detector = LinksDetektor(text, LinksDetektorOptions.HTML)

// Detect URLs in the text: an ImmutableList<Url>
val urls = detector.detect()

// Print the detected URLs
urls.forEach { url ->
    println("URL: ${url.originalUrl}")
    println("  Scheme: ${url.scheme}")
    println("  Host: ${url.host}")
    println("  Path: ${url.path}")
}

// Highlight each match: `range` is its position in the original text
urls.forEach { url -> println(text.substring(url.range)) }
```

Several options can be combined by passing a set (`kotlinx.collections.immutable.persistentSetOf`):

```kotlin
import kotlinx.collections.immutable.persistentSetOf

val detector = LinksDetektor(
    text,
    persistentSetOf(LinksDetektorOptions.QUOTE_MATCH, LinksDetektorOptions.BRACKET_MATCH)
)
```

> 💡 **Tip:** Use various `LinksDetektorOptions` to cater the detection to your needs.

## 🔧 Advanced Usage

<details open>
<summary><b>🔗 Creating a URL from a String</b></summary>

```kotlin
try {
    val url = Url.create("https://www.example.com/path?query=value#fragment")
    println("Scheme: ${url.scheme}")
    println("Host: ${url.host}")
    println("Path: ${url.path}")
    println("Query: ${url.query}")
    println("Fragment: ${url.fragment}")
} catch (e: MalformedUrlException) {
    println("Invalid URL: ${e.message}")
}
```

> `MalformedUrlException` is `java.net.MalformedURLException` on JVM and Android, and a library class everywhere else.

</details>

<details>
<summary><b>✨ Normalizing URLs</b></summary>

```kotlin
val url = Url.create("HTTP://ExAmPlE.com/a/b/.//./../c")
val normalizedUrl = url.normalize()

println("Original: $url")
println("Normalized: $normalizedUrl")
// Output: Normalized: HTTP://example.com/a/c
```

> 🧹 **Normalization** cleans up URLs by lowercasing the host, resolving path segments, and standardizing IP addresses. The scheme is kept as written (`HTTP` stays `HTTP`).
</details>

<details>
<summary><b>🔍 Extracting All Links from Text</b></summary>

```kotlin
fun String.extractLinks(): ImmutableList<String> {
    return LinksDetektorOptions.entries.asSequence()
        .flatMap { option ->
            LinksDetektor(this, option).detect().asSequence().mapNotNull { it.host }
        }
        .distinct()
        .toImmutableList()
}

val text = "Check multiple formats: https://example.com, <a href='https://github.com'>GitHub</a>, {\"url\": \"https://kotlin.org\"}"
val links = text.extractLinks()
println("Found domains: $links")
```

> 💪 **Power tip:** This extension function tries all detection options to find as many URLs as possible!
</details>

## 🔀 Migrating from 2.x

- `detect()` now returns `ImmutableList<Url>` (from `kotlinx-collections-immutable`) instead of `List<Url>`. It is still
  a `List`, so reading code keeps working; the library exposes `kotlinx-collections-immutable` as an `api` dependency.
- `HostNormalizer` is now internal. Use `Url.normalize()` and `NormalizedUrl`.
- `commons-math3` is no longer on the classpath. If you relied on it transitively, declare it yourself.
- Maven users must depend on `links-detektor-jvm` (see Installation). Gradle resolves the right variant on its own.
- Host lowercasing is locale independent (a Turkish default locale no longer turns `I` into a dotless `i`).
- The backtrack guard throws `IllegalStateException` instead of a platform-specific error.
- Outside the JVM and Android, IDN conversion does not run the Nameprep prohibited-character, bidi and unassigned
  checks. Hosts such as emoji domains are therefore encoded instead of rejected.
- Unicode normalization (NFKC) of internationalized hosts depends on the platform: JVM and Android use `java.net.IDN`,
  JS and Wasm use the web engine's Unicode version, Apple targets use the OS's, and Linux and Windows (mingw) use bundled
  Unicode 15.0 tables. Rare IDN results can therefore differ between platforms.
- `LinksDetektor` is stateful and not thread-safe; `detect()` is idempotent. `Url.rawHost`/`rawPath` are no longer
  `protected`.

## ⚙️ Configuration Options

> 🛠️ Links Detektor provides various configuration options through the `LinksDetektorOptions` enum:

<div style="text-align: center;">

### 🎛️ Fine-tune URL detection with these powerful options

</div>

| Option                      | Description                                                       | Use Case                             |
|-----------------------------|-------------------------------------------------------------------|--------------------------------------|
| `Default`                   | 🔄 Base configuration with no additional detection features       | Simple text parsing                  |
| `QUOTE_MATCH`               | 🔤 Enables double quote matching at the beginning and end of URLs | URLs in quoted strings               |
| `SINGLE_QUOTE_MATCH`        | 🔠 Enables single quote matching at the beginning and end of URLs | URLs in single-quoted strings        |
| `BRACKET_MATCH`             | 🔣 Enables bracket matching for URLs (handles (), {}, and [])     | URLs in code or structured text      |
| `JSON`                      | 📊 Configures detection for URLs in JSON content                  | API responses, config files          |
| `JAVASCRIPT`                | 📜 Configures detection for URLs in JavaScript content            | Web scraping, code analysis          |
| `XML`                       | 📋 Configures detection for URLs in XML content                   | Configuration files, feeds           |
| `HTML`                      | 🌐 Configures detection for URLs in HTML content                  | Web pages, HTML emails               |
| `ALLOW_SINGLE_LEVEL_DOMAIN` | 🏠 Enables detection of single-level domain URLs                  | Local development (http://localhost) |

## 📚 API Documentation

<div style="text-align: center;">

### 🧩 Core Components

</div>

The library provides the following main classes:

| Class                     | Description                                                         |
|---------------------------|---------------------------------------------------------------------|
| 🔍 `LinksDetektor`        | The main class for detecting URLs in text                           |
| ⚙️ `LinksDetektorOptions` | Configuration options for URL detection                             |
| 🔗 `Url`                  | Represents a URL with various components (scheme, host, path, etc.) |
| ✨ `NormalizedUrl`         | A normalized version of a URL for consistent representation         |

> 📖 For detailed API documentation, please refer to the KDoc comments in the source code.

## 👥 Contributing

> 🤝 Contributions are welcome! We appreciate your help in making this library better.

<div style="text-align: center;">

### 🌟 Ways to Contribute

</div>

| Type                    | Description                                      |
|-------------------------|--------------------------------------------------|
| 🐛 **Bug Reports**      | Report bugs and issues by creating GitHub issues |
| 💡 **Feature Requests** | Suggest new features or improvements             |
| 📝 **Documentation**    | Improve or correct documentation                 |
| ✅ **Testing**           | Write tests to increase code coverage            |
| 🔧 **Code**             | Submit pull requests to fix bugs or add features |

### 🛠️ Development Setup

<details open>
<summary><b>Getting Started</b></summary>

1. 🔄 Clone the repository
2. 🧰 Open the project in your IDE (IntelliJ IDEA recommended)
3. 🏗️ Build the project with Gradle: `./gradlew build`
4. 🧪 Run checks and tests: `./gradlew detektCheck :links-detektor:allTests`

</details>

### 📏 Code Style

<details>
<summary><b>Style Guidelines</b></summary>

The project uses [detekt](https://github.com/detekt/detekt) for static code analysis. Make sure your code passes the detekt checks before submitting a pull request:

```bash
./gradlew detektCheck
```

> ⚠️ **Important:** Pull requests that don't pass detekt checks will not be accepted.
</details>

## 🗺️ Roadmap

<div style="text-align: center;">

### 🚀 Future Plans

</div>

> 🔮 Here's what we're planning for upcoming releases:

| Status | Feature              | Description                                   |
|--------|----------------------|-----------------------------------------------|
| ⬜      | 🛠️ **Extensions**   | Add extensions to make the library usage easy |
| ⬜      | 🔢 **Magic Numbers** | Enable magic number rule in detekt            |
| ⬜      | 🧩 **Complexity**    | Enable complexity rule in detekt              |
| ✅      | 📱 **Multiplatform** | Make library KMP (Kotlin Multiplatform) ready |
| ✅      | 📚 **Documentation** | Make project dokka2 ready                     |
| ⬜      | 🧪 **Testing**       | Write more tests for utils and other methods  |
| ✅      | 🧪 **Core Tests**    | Added tests for utils and main detektor class |

<div style="text-align: right;">
<i>✅ = Completed &nbsp;⬜ = Planned</i>
</div>

## 📜 License

<div style="text-align: center;">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>

> This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
>
> 💼 **MIT License** permits free use, modification, and distribution with minimal restrictions.
