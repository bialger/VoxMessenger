package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.repository.SduiGateway

class PostSduiEventUseCase(
    private val sduiGateway: SduiGateway,
) {
    operator fun invoke(
        serverBaseUrl: String,
        deviceId: String,
        screenId: String,
        event: String,
    ): VoxResult<Unit> =
        sduiGateway.postEvent(
            serverBaseUrl = serverBaseUrl,
            deviceId = deviceId,
            screenId = screenId,
            event = event,
        )
}

