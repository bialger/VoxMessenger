package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.model.DeviceId

data class VoxPrimitiveExamples(
    val username: String,
    val unreadCount: Int,
    val serverTimestamp: Long,
    val attachmentSizeBytes: Long,
    val isOnline: Boolean,
)

fun primitiveExamples(): VoxPrimitiveExamples =
    VoxPrimitiveExamples(
        username = "alice",
        unreadCount = 4,
        serverTimestamp = 1_712_500_000L,
        attachmentSizeBytes = 4_194_304L,
        isOnline = true,
    )

fun sampleDeviceIds(username: String = "alice"): Array<DeviceId> =
    arrayOf(
        DeviceId("$username-phone"),
        DeviceId("$username-tablet"),
    )

fun samplePreKeyIds(size: Int = 5): IntArray = IntArray(size) { it + 1 }

