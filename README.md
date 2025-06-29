# Links Detektor

[![Maven Central](https://img.shields.io/maven-central/v/com.slothiesmooth/links-detektor.svg)](https://search.maven.org/search?q=g:com.slothiesmooth%20AND%20a:links-detektor)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.0-blue.svg)](https://kotlinlang.org)

A robust library for detecting and extracting URLs from text content. Links Detektor provides a powerful URL detection engine that can identify various URL formats within arbitrary text.

## Features

- Detects URLs in plain text, JSON, XML, JavaScript, and HTML content
- Handles various URL formats:
  - Standard URLs (http, https, ftp, ftps)
  - URLs with usernames and passwords
  - URLs with IPv4 addresses in various formats (decimal, hex, octal)
  - URLs with IPv6 addresses
  - URLs with international domain names
- Normalizes URLs for consistent representation and comparison
- More lenient than standard URL parsers (handles URLs that browsers accept but standard parsers reject)
- Customizable detection behavior through options

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.slothiesmooth:links-detektor:<latest_version>")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.slothiesmooth:links-detektor:<latest_version>'
}
```

### Maven

```xml
<dependency>
    <groupId>com.slothiesmooth</groupId>
    <artifactId>links-detektor</artifactId>
    <version>latest_version</version>
</dependency>
```

## Quick Start

```kotlin
// Sample text containing URLs
val text = "Check out these websites: https://www.example.com and http://github.com/some-repo"

// Create a LinksDetektor instance with HTML options
val detector = LinksDetektor(text, LinksDetektorOptions.HTML)

// Detect URLs in the text
val urls = detector.detect()

// Print the detected URLs
urls.forEach { url ->
    println("URL: ${url.originalUrl}")
    println("  Scheme: ${url.scheme}")
    println("  Host: ${url.host}")
    println("  Path: ${url.path}")
}
```

## Advanced Usage

### Creating a URL from a String

```kotlin
try {
    val url = Url.create("https://www.example.com/path?query=value#fragment")
    println("Scheme: ${url.scheme}")
    println("Host: ${url.host}")
    println("Path: ${url.path}")
    println("Query: ${url.query}")
    println("Fragment: ${url.fragment}")
} catch (e: MalformedURLException) {
    println("Invalid URL: ${e.message}")
}
```

### Normalizing URLs

```kotlin
val url = Url.create("HTTP://ExAmPlE.com/a/b/.//./../c")
val normalizedUrl = url.normalize()

println("Original: ${url}")
println("Normalized: ${normalizedUrl}")
// Output: Normalized: http://example.com/a/c
```

### Extracting All Links from Text

```kotlin
fun String.extractLinks(): List<String> {
    return LinksDetektorOptions.entries.asSequence()
        .flatMap { option ->
            LinksDetektor(this, option).detect().asSequence().mapNotNull { it.host }
        }
        .distinct()
        .toList()
}

val text = "Check multiple formats: https://example.com, <a href='https://github.com'>GitHub</a>, {\"url\": \"https://kotlin.org\"}"
val links = text.extractLinks()
println("Found domains: $links")
```

## Configuration Options

Links Detektor provides various configuration options through the `LinksDetektorOptions` enum:

| Option | Description |
|--------|-------------|
| `Default` | Base configuration with no additional detection features |
| `QUOTE_MATCH` | Enables double quote matching at the beginning and end of URLs |
| `SINGLE_QUOTE_MATCH` | Enables single quote matching at the beginning and end of URLs |
| `BRACKET_MATCH` | Enables bracket matching for URLs (handles (), {}, and []) |
| `JSON` | Configures detection for URLs in JSON content |
| `JAVASCRIPT` | Configures detection for URLs in JavaScript content |
| `XML` | Configures detection for URLs in XML content |
| `HTML` | Configures detection for URLs in HTML content |
| `ALLOW_SINGLE_LEVEL_DOMAIN` | Enables detection of single-level domain URLs (e.g., http://localhost) |

## API Documentation

The library provides the following main classes:

- `LinksDetektor`: The main class for detecting URLs in text
- `LinksDetektorOptions`: Configuration options for URL detection
- `Url`: Represents a URL with various components (scheme, host, path, etc.)
- `NormalizedUrl`: A normalized version of a URL for consistent representation

For detailed API documentation, please refer to the KDoc comments in the source code.

## Contributing

Contributions are welcome! Here are some ways you can contribute:

1. Report bugs and request features by creating issues
2. Submit pull requests to fix bugs or add new features
3. Improve documentation
4. Write tests to increase code coverage

### Development Setup

1. Clone the repository
2. Open the project in your IDE (IntelliJ IDEA recommended)
3. Build the project with Gradle: `./gradlew build`
4. Run tests: `./gradlew test`

### Code Style

The project uses [detekt](https://github.com/detekt/detekt) for static code analysis. Make sure your code passes the detekt checks before submitting a pull request:

```bash
./gradlew detekt
```

## Roadmap

Future plans for the library include:

- [ ] Enable magic number rule in detekt
- [ ] Enable complexity rule in detekt
- [ ] Make library KMP (Kotlin Multiplatform) ready
- [ ] Make project dokka2 ready
- [ ] Write more tests for utils and other methods to ensure functions remain stable with migrations and upgrades
    - [x] Added tests for utils and main detektor class 

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
