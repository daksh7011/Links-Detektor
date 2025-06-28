package com.slothiesmooth.linksdetektor

/**
 * Options for configuring URL detection behavior.
 * 
 * This enum is implemented as a bit mask, allowing multiple options to be combined.
 * Each option represents a specific detection behavior that can be enabled or disabled.
 * Options can be combined using bitwise operations through the [hasFlag] method.
 *
 * @property value The numeric bit value of the option, used in bitwise operations.
 */
enum class LinksDetektorOptions(
    /**
     * The numeric bit value of this option.
     */
    val value: Int
) {
    /**
     * Default options with no special checks.
     * 
     * This is the base configuration with no additional detection features enabled.
     * 
     * Bit value: 0 (00000000)
     */
    Default(0),

    /**
     * Enables double quote matching at the beginning and end of URLs.
     * 
     * When a URL is found within double quotes, the quotes themselves are excluded from the URL.
     * For example, in the text `"http://example.com"`, only `http://example.com` will be extracted.
     * 
     * Bit value: 1 (00000001)
     */
    QUOTE_MATCH(1),

    /**
     * Enables single quote matching at the beginning and end of URLs.
     * 
     * When a URL is found within single quotes, the quotes themselves are excluded from the URL.
     * For example, in the text `'http://example.com'`, only `http://example.com` will be extracted.
     * 
     * Bit value: 2 (00000010)
     */
    SINGLE_QUOTE_MATCH(2),

    /**
     * Enables bracket matching for URLs.
     * 
     * This option handles URLs enclosed in various bracket types: (), {}, and [].
     * Similar to quote matching, the brackets themselves are excluded from the URL.
     * 
     * Bit value: 4 (00000100)
     */
    BRACKET_MATCH(4),

    /**
     * Configures detection for URLs in JSON content.
     * 
     * This option enables both bracket matching and double quote matching,
     * which are common in JSON formatted content.
     * 
     * Bit value: 5 (00000101) = QUOTE_MATCH(1) | BRACKET_MATCH(4)
     */
    JSON(5),

    /**
     * Configures detection for URLs in JavaScript content.
     * 
     * This option enables bracket matching, double quote matching, and single quote matching,
     * covering the common string delimiters used in JavaScript.
     * 
     * Bit value: 7 (00000111) = QUOTE_MATCH(1) | SINGLE_QUOTE_MATCH(2) | BRACKET_MATCH(4)
     */
    JAVASCRIPT(7),

    /**
     * Configures detection for URLs in XML content.
     * 
     * This option enables XML tag detection and double quote matching,
     * which are common in XML formatted content.
     * 
     * Bit value: 9 (00001001)
     */
    XML(9),

    /**
     * Configures detection for URLs in HTML content.
     * 
     * This comprehensive option enables detection for URLs in various HTML contexts,
     * including attributes, JavaScript blocks, and plain text.
     * 
     * Bit value: 27 (00011011)
     */
    HTML(27),

    /**
     * Enables detection of single-level domain URLs.
     * 
     * With this option, URLs with single-level domains (no dot in the domain part)
     * will be recognized. Examples include `http://localhost` or `http://intranet`.
     * 
     * Bit value: 32 (00100000)
     */
    ALLOW_SINGLE_LEVEL_DOMAIN(32);

    /**
     * Checks if this options instance has the specified flag enabled.
     * 
     * This method performs a bitwise AND operation between the current options value
     * and the specified flag's value to determine if the flag is set.
     *
     * @param flag The option flag to check for.
     * @return `true` if the specified flag is enabled in this options instance, `false` otherwise.
     */
    fun hasFlag(flag: LinksDetektorOptions): Boolean {
        return value and flag.value == flag.value
    }
}
