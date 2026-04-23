package com.bialger.voxclient.domain.repository

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.SduiScreen

interface SduiGateway {
    fun fetchScreen(
        serverBaseUrl: String,
        deviceId: String,
        appVersionCode: Int,
        appVersionName: String?,
        locale: String?,
    ): VoxResult<SduiScreen?>

    fun postEvent(
        serverBaseUrl: String,
        deviceId: String,
        screenId: String,
        event: String,
    ): VoxResult<Unit>
}

