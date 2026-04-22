package com.bialger.voxclient.ui.common

import java.nio.charset.StandardCharsets
import java.util.Base64

object MessageCipherCodec {

    private const val PREFIX = "vox1:"

    fun encrypt(plaintext: String): String {
        val payload = Base64.getEncoder().encodeToString(plaintext.toByteArray(StandardCharsets.UTF_8))
        return "$PREFIX$payload"
    }

    fun decryptOrNull(ciphertext: String): String? {
        if (ciphertext.isBlank()) {
            return ""
        }

        if (ciphertext.startsWith(PREFIX)) {
            val encoded = ciphertext.removePrefix(PREFIX)
            return decodeBase64ToTextOrNull(encoded)
        }

        if (looksReadableText(ciphertext)) {
            return ciphertext
        }

        return decodeBase64ToTextOrNull(ciphertext)
    }

    private fun decodeBase64ToTextOrNull(value: String): String? {
        return runCatching {
            String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)
        }.getOrNull()?.takeIf { looksReadableText(it) }
    }

    private fun looksReadableText(value: String): Boolean {
        if (value.isBlank()) {
            return true
        }
        var readable = 0
        value.forEach { char ->
            if (char == '\n' || char == '\r' || char == '\t' || !Character.isISOControl(char)) {
                readable++
            }
        }
        return readable >= (value.length * 0.9f)
    }
}
