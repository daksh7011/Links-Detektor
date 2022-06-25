package `in`.technowolf.linksDetekt

enum class UrlPart(
    /**
     * This is the next url part that follows.
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
