package dev.slothiesmooth.linksdetektor.designlibrary.atoms

import dev.slothiesmooth.linksdetektor.designlibrary.tokens.LdType

/** The named text styles of the type scale. */
enum class LdTextStyle { Title, Body, BodySm, Label, Caption, Code, CodeSm }

/** The text style an [LdTextStyle] name stands for. */
fun LdType.of(style: LdTextStyle) = when (style) {
    LdTextStyle.Title -> title
    LdTextStyle.Body -> body
    LdTextStyle.BodySm -> bodySm
    LdTextStyle.Label -> label
    LdTextStyle.Caption -> caption
    LdTextStyle.Code -> code
    LdTextStyle.CodeSm -> codeSm
}
