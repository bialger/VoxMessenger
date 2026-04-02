package com.bialger.voxclient.ui.welcome

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import com.bialger.voxclient.R
import com.bialger.voxclient.databinding.FragmentWelcomeBinding
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding ?: error("Binding is only valid between onViewCreated and onDestroyView")
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWelcomeBinding.bind(view)

        binding.titleLabel.text = "Vox Messenger"
        binding.subtitleLabel.text = "Private messaging on your server"
        binding.securityLabel.text = "Your keys stay on this device"

        binding.checkServerButton.setOnClickListener {
            binding.serverStatusLabel.text = "Checking..."
            updateLastCheckedLabel()
        }
    }

    override fun onDestroyView() {
        mainHandler.removeCallbacksAndMessages(null)
        _binding = null
        super.onDestroyView()
    }

    private fun updateLastCheckedLabel() {
        mainHandler.post {
            _binding?.lastCheckedLabel?.text = "Last checked: ${LocalTime.now().format(timeFormatter)}"
        }
    }
}
