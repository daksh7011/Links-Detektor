package com.slothiesmooth.linksdetektor.internal

/**
 * The states to use to continue writing or not.
 */
internal enum class ReadEndState {
    /**
     * The current url is valid.
     */
    ValidUrl,

    /**
     * The current url is invalid.
     */
    InvalidUrl
}
