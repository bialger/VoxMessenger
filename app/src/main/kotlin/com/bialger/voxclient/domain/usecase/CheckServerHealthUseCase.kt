package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.ServerHealth
import com.bialger.voxclient.domain.repository.ServerHealthRepository
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class CheckServerHealthUseCase(
    private val serverHealthRepository: ServerHealthRepository,
) {
    operator fun invoke(rawServerBaseUrl: String): VoxResult<ServerHealth> {
        val normalizedBaseUrl = normalizeBaseUrl(rawServerBaseUrl)
            ?: return VoxResult.Failure(
                VoxError.Validation("Server URL must be a valid http(s) URL."),
            )

        return serverHealthRepository.checkHealth(serverBaseUrl = normalizedBaseUrl)
    }

    private fun normalizeBaseUrl(rawServerBaseUrl: String): String? {
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

        val parsed = withTrailingSlash.toHttpUrlOrNull() ?: return null
        return parsed.toString()
    }

    private companion object {
        const val HTTP_SCHEME = "http://"
        const val HTTPS_SCHEME = "https://"
    }
}
