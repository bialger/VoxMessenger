package com.bialger.voxclient.ui.welcome

import android.util.Log
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import com.bialger.voxclient.R
import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.ServerHealth
import com.bialger.voxclient.databinding.FragmentWelcomeBinding
import com.bialger.voxclient.di.AppGraph
import com.bialger.voxclient.ui.auth.AuthFragment
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding ?: error("Binding is only valid between onViewCreated and onDestroyView")
    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val requestGeneration = AtomicLong(0L)
    private val checkServerHealthUseCase = AppGraph.checkServerHealthUseCase
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWelcomeBinding.bind(view)

        binding.checkServerButton.setOnClickListener {
            checkServerHealth()
        }
        binding.continueAuthButton.setOnClickListener {
            val initialServer = binding.serverInput.text?.toString().orEmpty().trim()
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFragmentContainer, AuthFragment.newInstance(initialServer))
                .addToBackStack(AuthFragment::class.java.simpleName)
                .commit()
        }
    }

    override fun onDestroyView() {
        requestGeneration.incrementAndGet()
        mainHandler.removeCallbacksAndMessages(null)
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        backgroundExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun checkServerHealth() {
        val requestId = requestGeneration.incrementAndGet()
        val serverBaseUrl = binding.serverInput.text?.toString().orEmpty()
        binding.serverStatusLabel.text = getString(R.string.welcome_status_checking)
        updateLastCheckedLabel()

        backgroundExecutor.execute {
            val result = checkServerHealthUseCase(serverBaseUrl)
            mainHandler.post {
                if (requestId != requestGeneration.get()) {
                    return@post
                }
                renderHealthResult(result)
                updateLastCheckedLabel()
            }
        }
    }

    private fun renderHealthResult(result: VoxResult<ServerHealth>) {
        val currentBinding = _binding ?: return
        when (result) {
            is VoxResult.Success -> {
                val statusText = result.value.status.uppercase(Locale.ROOT)
                currentBinding.serverStatusLabel.text =
                    getString(R.string.welcome_status_server, statusText)
            }
            is VoxResult.Failure -> {
                currentBinding.serverStatusLabel.text =
                    if (result.error is VoxError.Validation) {
                        getString(R.string.welcome_status_invalid_url)
                    } else {
                        getString(R.string.welcome_status_unreachable)
                    }
                logNetworkFailure(result.error)
            }
        }
    }

    private fun logNetworkFailure(error: VoxError) {
        when (error) {
            is VoxError.Unknown -> Log.e(TAG, error.message, error.cause)
            is VoxError.Network -> Log.e(TAG, "${error.message} (code=${error.code})")
            else -> Log.e(TAG, error.message)
        }
    }

    private fun updateLastCheckedLabel() {
        mainHandler.post {
            _binding?.lastCheckedLabel?.text =
                getString(R.string.welcome_last_checked, LocalTime.now().format(timeFormatter))
        }
    }

    private companion object {
        const val TAG = "VoxNet"
    }
}
