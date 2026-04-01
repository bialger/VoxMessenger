package com.bialger.voxclient.core.model

data class DiscoveredUser(
    val userId: UserId,
    val username: String,
)

data class RemoteDeviceSummary(
    val deviceId: DeviceId,
    val deviceLabel: String?,
    val isRevoked: Boolean,
    val hasPrekeys: Boolean,
)

data class RemotePreKeyBundle(
    val userId: UserId,
    val username: String,
    val deviceId: DeviceId,
    val deviceLabel: String?,
    val identityKeyPublic: String,
    val signedPrekeyPublic: String,
    val signedPrekeySignature: String,
    val oneTimePrekeyPublic: String?,
    val oneTimePrekeyId: String?,
)

data class ContactNote(
    val userId: UserId,
    val alias: String?,
    val note: String?,
)

fun filterDirectory(users: List<DiscoveredUser>, query: String): List<DiscoveredUser> {
    fun normalize(value: String): String = value.trim().lowercase()
    val normalizedQuery = normalize(query)

    return users.filter { user ->
        normalize(user.username).contains(normalizedQuery)
    }
}

