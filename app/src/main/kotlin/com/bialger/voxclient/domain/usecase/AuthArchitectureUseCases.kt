package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.domain.boundary.LoadCurrentUserInputPort
import com.bialger.voxclient.domain.boundary.LoginInputPort
import com.bialger.voxclient.domain.boundary.RegisterUserInputPort
import com.bialger.voxclient.domain.entity.VoxAuthSession
import com.bialger.voxclient.domain.entity.VoxLoginCommand
import com.bialger.voxclient.domain.entity.VoxRegisterCommand
import com.bialger.voxclient.domain.entity.VoxUserProfile
import com.bialger.voxclient.domain.repository.AuthSessionGateway

class LoginUseCase(
    private val authSessionGateway: AuthSessionGateway,
) : LoginInputPort {
    override operator fun invoke(command: VoxLoginCommand): VoxResult<VoxAuthSession> {
        if (command.serverBaseUrl.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Server URL must not be blank."))
        }
        if (command.username.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Username must not be blank."))
        }
        if (command.passwordDerivedValue.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Password-derived value must not be blank."))
        }
        if (command.deviceId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Device ID must not be blank."))
        }

        return authSessionGateway.login(command)
    }
}

class RegisterUserUseCase(
    private val authSessionGateway: AuthSessionGateway,
) : RegisterUserInputPort {
    override operator fun invoke(command: VoxRegisterCommand): VoxResult<VoxAuthSession> {
        if (command.serverBaseUrl.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Server URL must not be blank."))
        }
        if (command.username.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Username must not be blank."))
        }
        if (command.passwordDerivedValue.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Password-derived value must not be blank."))
        }
        if (command.deviceId.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Device ID must not be blank."))
        }
        if (command.identityKeyPublic.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Identity key must not be blank."))
        }
        if (command.signedPrekeyPublic.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Signed prekey must not be blank."))
        }
        if (command.signedPrekeySignature.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Signed prekey signature must not be blank."))
        }
        if (command.wrappedSyncKey.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Wrapped sync key must not be blank."))
        }
        if (command.syncWrapSalt.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Sync-wrap salt must not be blank."))
        }

        return authSessionGateway.register(command)
    }
}

class LoadCurrentUserUseCase(
    private val authSessionGateway: AuthSessionGateway,
) : LoadCurrentUserInputPort {
    override operator fun invoke(serverBaseUrl: String, accessToken: String): VoxResult<VoxUserProfile> {
        if (serverBaseUrl.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Server URL must not be blank."))
        }
        if (accessToken.isBlank()) {
            return VoxResult.Failure(VoxError.Validation("Access token must not be blank."))
        }
        return authSessionGateway.loadCurrentUser(serverBaseUrl = serverBaseUrl, accessToken = accessToken)
    }
}
