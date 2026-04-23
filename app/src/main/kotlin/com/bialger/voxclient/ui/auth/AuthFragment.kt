package com.bialger.voxclient.ui.auth

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bialger.voxclient.R
import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.databinding.FragmentAuthBinding
import com.bialger.voxclient.di.AppGraph
import com.bialger.voxclient.domain.entity.VoxLoginCommand
import com.bialger.voxclient.domain.entity.VoxRegisterCommand
import com.bialger.voxclient.domain.entity.VoxSyncWrapParams
import com.bialger.voxclient.ui.chatlist.ChatListFragment
import com.bialger.voxclient.ui.common.ServerUrlNormalizer
import com.bialger.voxclient.ui.common.DeviceIdStore
import com.bialger.voxclient.ui.session.UserSessionArgs
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AuthFragment : Fragment(R.layout.fragment_auth) {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding ?: error("Binding is only valid between onViewCreated and onDestroyView")

    private val loginUseCase = AppGraph.loginUseCase
    private val registerUserUseCase = AppGraph.registerUserUseCase
    private val loadCurrentUserUseCase = AppGraph.loadCurrentUserUseCase
    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val secureRandom = SecureRandom()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAuthBinding.bind(view)

        val initialServer = requireArguments().getString(ARG_INITIAL_SERVER).orEmpty()
        binding.serverInput.setText(initialServer)

        binding.authToolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.signInButton.setOnClickListener {
            authenticate(isSignUp = false)
        }
        binding.signUpButton.setOnClickListener {
            authenticate(isSignUp = true)
        }
    }

    override fun onDestroyView() {
        _binding = null
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    override fun onDestroy() {
        backgroundExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun authenticate(isSignUp: Boolean) {
        val username = binding.usernameInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty().trim()
        val serverInput = binding.serverInput.text?.toString().orEmpty()
        val normalizedServer = ServerUrlNormalizer.normalize(serverInput)

        if (username.isEmpty() || password.isEmpty()) {
            binding.authStatusLabel.text = getString(R.string.auth_error_required_fields)
            return
        }

        if (username.equals(MOCK_VALUE, ignoreCase = true) && password.equals(MOCK_VALUE, ignoreCase = true)) {
            val mockSession = UserSessionArgs(
                serverBaseUrl = normalizedServer ?: DEFAULT_MOCK_SERVER,
                accessToken = MOCK_ACCESS_TOKEN,
                deviceId = MOCK_DEVICE_ID,
                userId = MOCK_USER_ID,
                username = MOCK_USERNAME,
                isMock = true,
            )
            openChatList(mockSession)
            return
        }

        val serverBaseUrl = normalizedServer
            ?: run {
                binding.authStatusLabel.text = getString(R.string.auth_error_invalid_server)
                return
            }

        setLoading(isLoading = true, isSignUp = isSignUp)
        backgroundExecutor.execute {
            val credentials = readOrCreateAuthMaterial()
            val passwordDerivedValue = sha256Hex(password)
            val result =
                if (isSignUp) {
                    registerUserUseCase(
                        VoxRegisterCommand(
                            serverBaseUrl = serverBaseUrl,
                            username = username,
                            passwordDerivedValue = passwordDerivedValue,
                            deviceId = credentials.deviceId,
                            deviceLabel = DEFAULT_DEVICE_LABEL,
                            identityKeyPublic = credentials.identityKeyPublic,
                            signedPrekeyPublic = credentials.signedPrekeyPublic,
                            signedPrekeySignature = credentials.signedPrekeySignature,
                            wrappedSyncKey = credentials.wrappedSyncKey,
                            syncWrapSalt = credentials.syncWrapSalt,
                            syncWrapParams = DEFAULT_SYNC_WRAP_PARAMS,
                        ),
                    )
                } else {
                    loginUseCase(
                        VoxLoginCommand(
                            serverBaseUrl = serverBaseUrl,
                            username = username,
                            passwordDerivedValue = passwordDerivedValue,
                            deviceId = credentials.deviceId,
                            deviceLabel = DEFAULT_DEVICE_LABEL,
                            identityKeyPublic = credentials.identityKeyPublic,
                            signedPrekeyPublic = credentials.signedPrekeyPublic,
                            signedPrekeySignature = credentials.signedPrekeySignature,
                        ),
                    )
                }
            val preparedSession =
                if (result is VoxResult.Success) {
                    val profileResult =
                        loadCurrentUserUseCase(
                            serverBaseUrl = serverBaseUrl,
                            accessToken = result.value.accessToken,
                        )
                    val profile =
                        if (profileResult is VoxResult.Success) {
                            profileResult.value
                        } else {
                            null
                        }
                    UserSessionArgs(
                        serverBaseUrl = serverBaseUrl,
                        accessToken = result.value.accessToken,
                        deviceId = profile?.currentDeviceId ?: result.value.deviceId,
                        userId = result.value.userId,
                        username = profile?.username ?: username,
                        isMock = false,
                    )
                } else {
                    null
                }

            mainHandler.post {
                if (_binding == null) {
                    return@post
                }
                setLoading(isLoading = false, isSignUp = isSignUp)
                when (result) {
                    is VoxResult.Success -> {
                        binding.authStatusLabel.text = getString(R.string.auth_status_ready)
                        openChatList(preparedSession ?: return@post)
                    }

                    is VoxResult.Failure -> {
                        binding.authStatusLabel.text = errorMessage(result.error)
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean, isSignUp: Boolean) {
        binding.authProgress.isVisible = isLoading
        binding.signInButton.isEnabled = !isLoading
        binding.signUpButton.isEnabled = !isLoading
        binding.serverInput.isEnabled = !isLoading
        binding.usernameInput.isEnabled = !isLoading
        binding.passwordInput.isEnabled = !isLoading
        binding.authStatusLabel.text =
            if (!isLoading) {
                getString(R.string.auth_status_ready)
            } else if (isSignUp) {
                getString(R.string.auth_status_signing_up)
            } else {
                getString(R.string.auth_status_signing_in)
            }
    }

    private fun openChatList(session: UserSessionArgs) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.mainFragmentContainer,
                ChatListFragment.newInstance(session),
            )
            .addToBackStack(ChatListFragment::class.java.simpleName)
            .commit()
    }

    private fun errorMessage(error: VoxError): String =
        when (error) {
            is VoxError.Validation -> error.message
            is VoxError.Network -> error.message
            is VoxError.Conflict -> error.message
            is VoxError.Unknown -> error.message
        }

    private fun readOrCreateAuthMaterial(): AuthMaterial {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceId = DeviceIdStore.getOrCreate(requireContext())
        val identityKeyPublic = prefs.getString(KEY_IDENTITY_KEY_PUBLIC, null) ?: randomBase64(32)
        val signedPrekeyPublic = prefs.getString(KEY_SIGNED_PREKEY_PUBLIC, null) ?: randomBase64(32)
        val signedPrekeySignature = prefs.getString(KEY_SIGNED_PREKEY_SIGNATURE, null) ?: randomBase64(64)
        val wrappedSyncKey = prefs.getString(KEY_WRAPPED_SYNC_KEY, null) ?: randomBase64(48)
        val syncWrapSalt = prefs.getString(KEY_SYNC_WRAP_SALT, null) ?: randomBase64(16)
        prefs.edit()
            .putString(KEY_IDENTITY_KEY_PUBLIC, identityKeyPublic)
            .putString(KEY_SIGNED_PREKEY_PUBLIC, signedPrekeyPublic)
            .putString(KEY_SIGNED_PREKEY_SIGNATURE, signedPrekeySignature)
            .putString(KEY_WRAPPED_SYNC_KEY, wrappedSyncKey)
            .putString(KEY_SYNC_WRAP_SALT, syncWrapSalt)
            .apply()

        return AuthMaterial(
            deviceId = deviceId,
            identityKeyPublic = identityKeyPublic,
            signedPrekeyPublic = signedPrekeyPublic,
            signedPrekeySignature = signedPrekeySignature,
            wrappedSyncKey = wrappedSyncKey,
            syncWrapSalt = syncWrapSalt,
        )
    }

    private fun randomBase64(size: Int): String {
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun sha256Hex(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return buildString(digest.size * 2) {
            digest.forEach { byte ->
                append(((byte.toInt() and 0xff) + 0x100).toString(16).substring(1))
            }
        }
    }

    data class AuthMaterial(
        val deviceId: String,
        val identityKeyPublic: String,
        val signedPrekeyPublic: String,
        val signedPrekeySignature: String,
        val wrappedSyncKey: String,
        val syncWrapSalt: String,
    )

    companion object {
        private const val ARG_INITIAL_SERVER = "initial_server"
        private const val PREFS_NAME = "vox_auth_prefs"
        private const val DEFAULT_DEVICE_LABEL = "Android"
        private const val MOCK_VALUE = "mock"
        private const val DEFAULT_MOCK_SERVER = "https://mock.local/"
        private const val MOCK_ACCESS_TOKEN = "mock_access_token"
        private const val MOCK_DEVICE_ID = "mock_device_id"
        private const val MOCK_USER_ID = "mock_user_id"
        private const val MOCK_USERNAME = "mock"

        private const val KEY_IDENTITY_KEY_PUBLIC = "identity_key_public"
        private const val KEY_SIGNED_PREKEY_PUBLIC = "signed_prekey_public"
        private const val KEY_SIGNED_PREKEY_SIGNATURE = "signed_prekey_signature"
        private const val KEY_WRAPPED_SYNC_KEY = "wrapped_sync_key"
        private const val KEY_SYNC_WRAP_SALT = "sync_wrap_salt"

        private val DEFAULT_SYNC_WRAP_PARAMS =
            VoxSyncWrapParams(
                algorithm = "argon2id",
                memoryKiB = 65536,
                iterations = 3,
                parallelism = 1,
            )

        fun newInstance(initialServerBaseUrl: String): AuthFragment =
            AuthFragment().apply {
                arguments = bundleOf(ARG_INITIAL_SERVER to initialServerBaseUrl)
            }
    }
}
