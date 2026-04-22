package com.bialger.voxclient.ui.common

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ServerUrlNormalizer {

    fun normalize(rawServerBaseUrl: String): String? {
        val trimmed = rawServerBaseUrl.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        val withScheme =
            if (trimmed.startsWith(HTTP_SCHEME) || trimmed.startsWith(HTTPS_SCHEME)) {
                trimmed
            } else {
                "$HTTPS_SCHEME$trimmed"
            }

        val withTrailingSlash =
            if (withScheme.endsWith('/')) {
                withScheme
            } else {
                "$withScheme/"
            }

        return withTrailingSlash.toHttpUrlOrNull()?.toString()
    }

    private const val HTTP_SCHEME = "http://"
    private const val HTTPS_SCHEME = "https://"
}
