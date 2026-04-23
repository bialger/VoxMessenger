package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.SduiScreen
import com.bialger.voxclient.domain.repository.SduiGateway

class ResolveSduiScreenUseCase(
    private val sduiGateway: SduiGateway,
) {
    operator fun invoke(
        serverBaseUrl: String,
        deviceId: String,
        appVersionCode: Int,
        appVersionName: String?,
        locale: String?,
    ): VoxResult<SduiScreen?> =
        sduiGateway.fetchScreen(
            serverBaseUrl = serverBaseUrl,
            deviceId = deviceId,
            appVersionCode = appVersionCode,
            appVersionName = appVersionName,
            locale = locale,
        )
}

