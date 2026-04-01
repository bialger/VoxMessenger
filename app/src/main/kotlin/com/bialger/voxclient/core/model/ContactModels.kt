package com.bialger.voxclient.core.model

data class KnownContact(
    val userId: UserId,
    val username: String,
    val alias: String?,
    val note: String?,
    val pinnedFingerprint: String?,
    val isBlocked: Boolean,
)

