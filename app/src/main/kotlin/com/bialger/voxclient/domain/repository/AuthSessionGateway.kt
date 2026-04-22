package com.bialger.voxclient.domain.repository

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.entity.VoxAuthSession
import com.bialger.voxclient.domain.entity.VoxLoginCommand
import com.bialger.voxclient.domain.entity.VoxRegisterCommand
import com.bialger.voxclient.domain.entity.VoxUserProfile

interface AuthSessionGateway {
    fun login(command: VoxLoginCommand): VoxResult<VoxAuthSession>

    fun register(command: VoxRegisterCommand): VoxResult<VoxAuthSession>

    fun loadCurrentUser(serverBaseUrl: String, accessToken: String): VoxResult<VoxUserProfile>
}
