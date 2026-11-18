package com.slothiesmooth.linksdetektor.internal

import kotlinx.collections.immutable.persistentListOf

internal class PathNormalizer {
    /**
     * Normalizes the path by doing the following:
     * remove special spaces, decoding hex encoded characters,
     * gets rid of extra dots and slashes, and re-encodes it once
     */
    fun normalizePath(path: String?): String? {
        if (path.isNullOrEmpty()) {
            return path
        }
        val decoded = UrlUtil.decode(path)
        val sanitized = sanitizeDotsAndSlashes(decoded)
        return UrlUtil.encode(sanitized)
    }

    companion object {
        /**
         * 1. Replaces "/./" with "/" recursively.
         * 2. "/blah/asdf/.." -> "/blah"
         * 3. "/blah/blah2/blah3/../../blah4" -> "/blah/blah4"
         * 4. "//" -> "/"
         * 5. Adds a slash at the end if there isn't one
         */
        private fun sanitizeDotsAndSlashes(path: String?): String {
            val stringBuilder = StringBuilder(path.orEmpty())
            val slashIndexStack = persistentListOf<Int>().builder()
            var index = 0
            while (index < stringBuilder.length - 1) {
                if (stringBuilder[index] == '/') {
                    slashIndexStack.add(index)
                    if (stringBuilder[index + 1] == '.') {
                        if (index < stringBuilder.length - 2 && stringBuilder[index + 2] == '.') {
                            // If it looks like "/../" or ends with "/.."
                            if (index < stringBuilder.length - 3 && stringBuilder[index + 3] == '/' ||
                                index == stringBuilder.length - 3
                            ) {
                                val endOfPath = index == stringBuilder.length - 3
                                slashIndexStack.removeAt(slashIndexStack.lastIndex)
                                val endIndex = index + 3
                                // backtrack so we can detect if this / is part of another replacement
                                index = if (slashIndexStack.isEmpty()) -1 else slashIndexStack.removeAt(slashIndexStack.lastIndex) - 1
                                val startIndex = if (endOfPath) index + 1 else index
                                stringBuilder.deleteRange(startIndex + 1, endIndex)
                            }
                        } else if (index < stringBuilder.length - 2 && stringBuilder[index + 2] == '/' ||
                            index == stringBuilder.length - 2
                        ) {
                            val endOfPath = index == stringBuilder.length - 2
                            slashIndexStack.removeAt(slashIndexStack.lastIndex)
                            val startIndex = if (endOfPath) index + 1 else index
                            stringBuilder.deleteRange(startIndex, index + 2) // "/./" -> "/"
                            index-- // backtrack so we can detect if this / is part of another replacement
                        }
                    } else if (stringBuilder[index + 1] == '/') {
                        slashIndexStack.removeAt(slashIndexStack.lastIndex)
                        stringBuilder.deleteAt(index)
                        index--
                    }
                }
                index++
            }
            if (stringBuilder.isEmpty()) {
                stringBuilder.append("/") // Every path has at least a slash
            }
            return stringBuilder.toString()
        }
    }
}
