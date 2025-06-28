package com.slothiesmooth.linksdetektor.internal

/**
 * Represents the different parts of a URL in their hierarchical order.
 * Each part knows about the next part in the URL structure.
 * The order is: SCHEME -> USERNAME_PASSWORD -> HOST -> PORT -> PATH -> QUERY -> FRAGMENT
 */
internal enum class UrlPart(
    /**
     * This is the next url part that follows in the URL structure.
     * For example, HOST is followed by PORT, PATH is followed by QUERY, etc.
     * FRAGMENT is the last part and has no next part (null).
     */
    val nextPart: UrlPart?
) {
    FRAGMENT(null),
    QUERY(FRAGMENT),
    PATH(QUERY),
    PORT(PATH),
    HOST(PORT),
    USERNAME_PASSWORD(HOST),
    SCHEME(USERNAME_PASSWORD);
}
