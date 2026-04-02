package com.bialger.voxclient.domain.repository

import com.bialger.voxclient.core.common.result.VoxResult

interface UsernameGateway {
    fun fetchUsernameByUserId(
        serverBaseUrl: String,
        accessToken: String,
        userId: String,
    ): VoxResult<String>

    fun fetchUsernamesBatchByUserIds(
        serverBaseUrl: String,
        accessToken: String,
        userIds: Collection<String>,
    ): VoxResult<Map<String, String>>

    fun registerUsernamesBatch(
        serverBaseUrl: String,
        accessToken: String,
        usernamesByUserId: Map<String, String>,
    ): VoxResult<Unit>
}
