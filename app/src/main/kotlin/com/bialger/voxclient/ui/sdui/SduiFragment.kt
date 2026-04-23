package com.bialger.voxclient.ui.sdui

import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bialger.voxclient.R
import com.bialger.voxclient.core.model.SduiAction
import com.bialger.voxclient.databinding.FragmentSduiBinding
import com.bialger.voxclient.ui.auth.AuthFragment
import com.bialger.voxclient.ui.common.DeviceIdStore
import com.bialger.voxclient.ui.common.ServerUrlNormalizer
import com.bialger.voxclient.ui.sdui.renderer.SduiViewFactory
import java.util.Locale

class SduiFragment : Fragment(R.layout.fragment_sdui) {
    private var _binding: FragmentSduiBinding? = null
    private val binding get() = _binding ?: error("Binding is only valid between onViewCreated and onDestroyView")

    private val viewModel: SduiViewModel by viewModels()
    private val viewFactory = SduiViewFactory()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSduiBinding.bind(view)

        val serverBaseUrl =
            ServerUrlNormalizer.normalize(requireArguments().getString(ARG_SERVER_BASE_URL).orEmpty())
                ?: run {
                    binding.sduiStatusLabel.text = getString(R.string.welcome_status_invalid_url)
                    binding.sduiProgress.isVisible = false
                    binding.sduiBottomActions.isVisible = false
                    return
                }
        val deviceId = DeviceIdStore.getOrCreate(requireContext())
        val (versionCode, versionName) = appVersion()
        val locale = Locale.getDefault().toLanguageTag()

        binding.sduiToolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.continueButton.setOnClickListener {
            openAuth(serverBaseUrl)
        }
        binding.showEulaAndUpdatesButton.setOnClickListener {
            // Demo helper: "reset" server EULA state (decline) then reload screen.
            // If we don't have a screen yet, just load it first.
            val screenId = viewModel.uiState.value?.screen?.screenId.orEmpty()
            if (screenId.isBlank()) {
                viewModel.load(serverBaseUrl, deviceId, versionCode, versionName, locale)
            } else {
                viewModel.postEvent(serverBaseUrl, deviceId, screenId, "eula_declined")
            }
        }
        binding.acceptEulaButton.setOnClickListener {
            val screenId = viewModel.uiState.value?.screen?.screenId.orEmpty()
            viewModel.postEvent(serverBaseUrl, deviceId, screenId, "eula_accepted")
        }
        binding.declineEulaButton.setOnClickListener {
            val screenId = viewModel.uiState.value?.screen?.screenId.orEmpty()
            viewModel.postEvent(serverBaseUrl, deviceId, screenId, "eula_declined")
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.sduiProgress.isVisible = state.isLoading

            val screen = state.screen
            if (screen == null) {
                binding.sduiToolbar.title = getString(R.string.sdui_title)
                binding.sduiStatusLabel.text =
                    state.statusText ?: getString(R.string.sdui_status_nothing_to_show)
                binding.sduiContentContainer.removeAllViews()
                binding.sduiBottomActions.isVisible = true
                binding.showEulaAndUpdatesButton.isVisible = true
                binding.eulaActionRow.isVisible = false
            } else {
                binding.sduiToolbar.title = screen.title
                binding.sduiStatusLabel.text = state.statusText ?: ""
                binding.sduiContentContainer.removeAllViews()
                screen.body.forEach { node ->
                    val viewNode = viewFactory.create(binding.sduiContentContainer, node, ::handleAction)
                    binding.sduiContentContainer.addView(viewNode)
                }
                binding.sduiBottomActions.isVisible = true
                // Keep "Show server EULA" visible as a reset (decline+reload) button.
                binding.showEulaAndUpdatesButton.isVisible = true
                binding.eulaActionRow.isVisible = true
            }
        }

        viewModel.postedEvent.observe(viewLifecycleOwner) { posted ->
            val event = posted?.event ?: return@observe
            val result = posted.result
            when (result) {
                is com.bialger.voxclient.core.common.result.VoxResult.Success -> {
                    when (event.lowercase(Locale.ROOT)) {
                        "eula_accepted" -> {
                            if (isForcedUpdate()) {
                                binding.sduiStatusLabel.text = getString(R.string.sdui_status_update_required)
                            } else {
                                openAuth(serverBaseUrl)
                            }
                        }
                        else -> {
                            // eula_declined (and any other telemetry): re-check screen.
                            val (vc, vn) = appVersion()
                            val loc = Locale.getDefault().toLanguageTag()
                            binding.sduiStatusLabel.text = ""
                            viewModel.load(serverBaseUrl, deviceId, vc, vn, loc)
                        }
                    }
                }
                is com.bialger.voxclient.core.common.result.VoxResult.Failure -> {
                    binding.sduiStatusLabel.text = result.error.message
                }
            }
        }

        viewModel.load(serverBaseUrl, deviceId, versionCode, versionName, locale)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun handleAction(action: SduiAction) {
        val serverBaseUrl = requireArguments().getString(ARG_SERVER_BASE_URL).orEmpty()
        val deviceId = DeviceIdStore.getOrCreate(requireContext())
        val forcedUpdate = isForcedUpdate()

        when (action.type.trim().lowercase(Locale.ROOT)) {
            "open_url" -> {
                val url = action.url?.trim().orEmpty()
                if (url.isBlank()) {
                    binding.sduiStatusLabel.text = getString(R.string.sdui_error_unknown_action, "open_url(missing_url)")
                    return
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            "close" -> {
                if (forcedUpdate) {
                    binding.sduiStatusLabel.text = getString(R.string.sdui_status_update_required)
                } else {
                    openAuth(serverBaseUrl)
                }
            }
            "post_event" -> {
                val screenId = viewModel.uiState.value?.screen?.screenId.orEmpty()
                val event = action.event.orEmpty()
                viewModel.postEvent(serverBaseUrl, deviceId, screenId, event)
            }
            else -> {
                binding.sduiStatusLabel.text = getString(R.string.sdui_error_unknown_action, action.type)
            }
        }
    }

    private fun openAuth(serverBaseUrl: String) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, AuthFragment.newInstance(serverBaseUrl))
            .addToBackStack(AuthFragment::class.java.simpleName)
            .commit()
    }

    private fun backToWelcome() {
        parentFragmentManager.popBackStack()
    }

    private fun appVersion(): Pair<Int, String?> {
        val pm = requireContext().packageManager
        val pkg = requireContext().packageName
        return try {
            val info = pm.getPackageInfo(pkg, 0)
            @Suppress("DEPRECATION")
            val versionCode = info.longVersionCode.toInt()
            versionCode to info.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            0 to null
        }
    }

    private fun isForcedUpdate(): Boolean {
        val state = viewModel.uiState.value ?: return false
        val screen = state.screen ?: return false
        val min = screen.meta?.minClientVersionCode ?: return false
        val (versionCode, _) = appVersion()
        return versionCode in 1 until min
    }

    companion object {
        private const val ARG_SERVER_BASE_URL = "server_base_url"

        fun newInstance(serverBaseUrl: String): SduiFragment =
            SduiFragment().apply {
                arguments = bundleOf(ARG_SERVER_BASE_URL to serverBaseUrl)
            }
    }
}

