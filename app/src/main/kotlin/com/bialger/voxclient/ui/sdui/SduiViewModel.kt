package com.bialger.voxclient.ui.sdui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.SduiScreen
import com.bialger.voxclient.di.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SduiViewModel : ViewModel() {
    private val resolveSduiScreenUseCase = AppGraph.resolveSduiScreenUseCase
    private val postSduiEventUseCase = AppGraph.postSduiEventUseCase

    private val _uiState = MutableLiveData(SduiUiState())
    val uiState: LiveData<SduiUiState> = _uiState

    private val _postedEvent = MutableLiveData<SduiPostedEvent?>(null)
    val postedEvent: LiveData<SduiPostedEvent?> = _postedEvent

    fun showLocalScreen(screen: SduiScreen) {
        _uiState.value =
            SduiUiState(
                isLoading = false,
                screen = screen,
                statusText = null,
            )
    }

    fun load(
        serverBaseUrl: String,
        deviceId: String,
        appVersionCode: Int,
        appVersionName: String?,
        locale: String?,
    ) {
        _uiState.value = _uiState.value?.copy(isLoading = true, statusText = null)
        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    resolveSduiScreenUseCase(
                        serverBaseUrl = serverBaseUrl,
                        deviceId = deviceId,
                        appVersionCode = appVersionCode,
                        appVersionName = appVersionName,
                        locale = locale,
                    )
                }
            when (result) {
                is VoxResult.Success -> {
                    val screen = result.value
                    _uiState.value =
                        SduiUiState(
                            isLoading = false,
                            screen = screen,
                            statusText = null,
                        )
                }
                is VoxResult.Failure -> {
                    _uiState.value =
                        SduiUiState(
                            isLoading = false,
                            screen = null,
                            statusText = result.error.message,
                        )
                }
            }
        }
    }

    fun postEvent(
        serverBaseUrl: String,
        deviceId: String,
        screenId: String,
        event: String,
    ) {
        _uiState.value = _uiState.value?.copy(isLoading = true, statusText = null)
        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    postSduiEventUseCase(
                        serverBaseUrl = serverBaseUrl,
                        deviceId = deviceId,
                        screenId = screenId,
                        event = event,
                    )
                }
            _uiState.value = _uiState.value?.copy(isLoading = false)
            _postedEvent.value = SduiPostedEvent(event = event, result = result)
        }
    }
}

data class SduiUiState(
    val isLoading: Boolean = false,
    val screen: SduiScreen? = null,
    val statusText: String? = null,
)

data class SduiPostedEvent(
    val event: String,
    val result: VoxResult<Unit>,
)

