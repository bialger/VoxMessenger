package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.ServerHealth
import com.bialger.voxclient.domain.repository.ServerHealthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StageThreeNetworkUseCaseTest {
    @Test
    fun invoke_failsWhenServerUrlIsBlank() {
        val repository = FakeServerHealthRepository()
        val useCase = CheckServerHealthUseCase(repository)

        val result = useCase("  ")

        val failure = result as VoxResult.Failure
        assertTrue(failure.error is VoxError.Validation)
    }

    @Test
    fun invoke_normalizesBaseUrlAndDelegatesToRepository() {
        val repository = FakeServerHealthRepository()
        val useCase = CheckServerHealthUseCase(repository)

        val result = useCase("vox.example.com")

        assertTrue(result is VoxResult.Success)
        assertEquals("https://vox.example.com/", repository.lastServerBaseUrl)
    }

    @Test
    fun invoke_failsWhenServerUrlIsInvalid() {
        val repository = FakeServerHealthRepository()
        val useCase = CheckServerHealthUseCase(repository)

        val result = useCase("https://")

        val failure = result as VoxResult.Failure
        assertTrue(failure.error is VoxError.Validation)
    }

    private class FakeServerHealthRepository : ServerHealthRepository {
        var lastServerBaseUrl: String? = null

        override fun checkHealth(serverBaseUrl: String): VoxResult<ServerHealth> {
            lastServerBaseUrl = serverBaseUrl
            return VoxResult.Success(ServerHealth(status = "ok"))
        }
    }
}
