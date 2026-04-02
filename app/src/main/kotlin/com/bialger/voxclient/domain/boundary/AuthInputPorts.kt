package com.bialger.voxclient.domain.boundary

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.entity.VoxAuthSession
import com.bialger.voxclient.domain.entity.VoxLoginCommand
import com.bialger.voxclient.domain.entity.VoxRegisterCommand
import com.bialger.voxclient.domain.entity.VoxUserProfile

interface LoginInputPort {
    operator fun invoke(command: VoxLoginCommand): VoxResult<VoxAuthSession>
}

interface RegisterUserInputPort {
    operator fun invoke(command: VoxRegisterCommand): VoxResult<VoxAuthSession>
}

interface LoadCurrentUserInputPort {
    operator fun invoke(serverBaseUrl: String, accessToken: String): VoxResult<VoxUserProfile>
}
