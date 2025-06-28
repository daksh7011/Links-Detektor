package com.slothiesmooth.linksdetektor.internal.model

/**
     * Represents the possible states after attempting to read a domain name.
     *
     * This enum defines the outcomes of domain name processing and indicates
     * what part of the URL should be processed next (if any).
     */
    internal enum class ReaderNextState {
        /**
         * The domain name is invalid or could not be properly parsed.
         *
         * This state indicates that URL detection should be aborted or restarted
         * from a different position in the input.
         */
        InvalidDomainName,

        /**
         * The domain name is valid and complete.
         *
         * This state indicates that a valid domain name was successfully parsed
         * and no additional URL parts were detected.
         */
        ValidDomainName,

        /**
         * A valid domain name was found, followed by a fragment indicator (#).
         *
         * This state indicates that the next part to process is the URL fragment.
         */
        ReadFragment,

        /**
         * A valid domain name was found, followed by a path separator (/).
         *
         * This state indicates that the next part to process is the URL path.
         */
        ReadPath,

        /**
         * A valid domain name was found, followed by a port indicator (:).
         *
         * This state indicates that the next part to process is the port number.
         */
        ReadPort,

        /**
         * A valid domain name was found, followed by a query string indicator (?).
         *
         * This state indicates that the next part to process is the URL query string.
         */
        ReadQueryString
    }
