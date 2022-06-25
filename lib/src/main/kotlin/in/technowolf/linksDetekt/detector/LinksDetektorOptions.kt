package `in`.technowolf.linksDetekt.detector

/**
 * The options to use when detecting urls. This enum is used as a bit mask to be able to set multiple options at once.
 * @param value The numeric value of the enum
 */
enum class LinksDetektorOptions(
    /**
     * The numeric value.
     */
    val value: Int
) {
    /**
     * Default options, no special checks.
     */
    Default(0),

    /**
     * Matches quotes in the beginning and end of string.
     * If a string starts with a quote, then the ending quote will be eliminated. For example,
     * "http://technowolf.in" will pull out just 'http://technowolf.in' instead of 'http://technowolf.in"'
     */
    QUOTE_MATCH(1), // 00000001

    /**
     * Matches single quotes in the beginning and end of a string.
     */
    SINGLE_QUOTE_MATCH(2), // 00000010

    /**
     * Matches brackets and closes on the second one.
     * Same as quote matching but works for brackets such as (), {}, [].
     */
    BRACKET_MATCH(4), // 000000100

    /**
     * Checks for bracket characters and more importantly quotes to start and end strings.
     */
    JSON(5), // 00000101

    /**
     * Checks JSON format or but also looks for a single quote.
     */
    JAVASCRIPT(7), // 00000111

    /**
     * Checks for xml characters and uses them as ending characters as well as quotes.
     * This also includes quote_matching.
     */
    XML(9), // 00001001

    /**
     * Checks all of the rules besides brackets. This is XML but also can contain javascript.
     */
    HTML(27), // 00011011

    /**
     * Checks for single level domains as well. Ex: go/, http://localhost
     */
    ALLOW_SINGLE_LEVEL_DOMAIN(32);
    // 00100000
    /**
     * Gets the numeric value of the enum
     * @return The numeric value of the enum
     */

    /**
     * Checks if the current options have the specified flag.
     * @param flag The flag to check for.
     * @return True if this flag is active, else false.
     */
    fun hasFlag(flag: LinksDetektorOptions): Boolean {
        return value and flag.value == flag.value
    }
}
