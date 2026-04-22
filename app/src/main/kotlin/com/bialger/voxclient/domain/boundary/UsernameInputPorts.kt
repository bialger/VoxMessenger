package com.bialger.voxclient.domain.boundary

import com.bialger.voxclient.core.common.result.VoxResult

interface FetchUsernameByUserIdInputPort {
    operator fun invoke(serverBaseUrl: String, accessToken: String, userId: String): VoxResult<String>
}

interface FetchUsernamesBatchByUserIdsInputPort {
    operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        userIds: Collection<String>,
    ): VoxResult<Map<String, String>>
}

interface RegisterUsernamesBatchInputPort {
    operator fun invoke(
        serverBaseUrl: String,
        accessToken: String,
        usernamesByUserId: Map<String, String>,
    ): VoxResult<Unit>
}
