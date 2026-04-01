package com.bialger.voxclient.core.model

interface CryptoEngine {
    fun generateIdentityKeyPair(): ByteArray

    fun verifySignedPrekey(
        identityKeyPublic: ByteArray,
        signedPrekeyPublic: ByteArray,
        signature: ByteArray,
    ): Boolean
}

