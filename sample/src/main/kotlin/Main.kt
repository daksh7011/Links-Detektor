package com.slothiesmooth

import com.slothiesmooth.linksdetektor.LinksDetektor
import com.slothiesmooth.linksdetektor.LinksDetektorOptions

fun main() {
    // Sample text containing URLs
    val sampleText = "Check out these websites: https://www.example.com and http://github.com/some-repo"

    // Create a LinksDetektor instance with the sample text and HTML options
    val detector = LinksDetektor(sampleText, LinksDetektorOptions.HTML)

    // Detect URLs in the text
    val detectedUrls = detector.detect()

    // Print the detected URLs
    println("Detected ${detectedUrls.size} URLs:")
    detectedUrls.forEach { url ->
        println("- ${url.originalUrl}")
    }
}

/**
 * Extracts all links from a string using various detection methods.
 *
 * This function uses LinksDetektor with all available options to extract links from various
 * contexts including plain text, brackets, quotes, JSON, JavaScript, XML, and HTML.
 *
 * @return A distinct list of domain names extracted from the string
 */
fun String.extractLinks(): List<String> {
    // Use all available options
    // Use sequence for more efficient processing
    return LinksDetektorOptions.entries.asSequence()
        .flatMap { option ->
            LinksDetektor(this, option).detect().asSequence().mapNotNull { it.host }
        }
        .distinct()
        .toList()
}
