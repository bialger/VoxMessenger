package com.bialger.voxclient.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bialger.voxclient.R
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.databinding.DialogAddMemberBinding
import com.bialger.voxclient.databinding.FragmentConversationBinding
import com.bialger.voxclient.di.AppGraph
import com.bialger.voxclient.domain.entity.VoxConversationEnvelope
import com.bialger.voxclient.domain.entity.VoxSendMessageCommand
import com.bialger.voxclient.ui.common.MessageCipherCodec
import com.bialger.voxclient.ui.session.UserSessionArgs
import com.bialger.voxclient.ui.session.readUserSessionArgs
import com.bialger.voxclient.ui.session.toBundle
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ConversationFragment : Fragment(R.layout.fragment_conversation) {

    private var _binding: FragmentConversationBinding? = null
    private val binding get() = _binding ?: error("Binding is only valid between onViewCreated and onDestroyView")
    private val viewModel: ConversationViewModel by viewModels()
    private val messageAdapter = MessageAdapter()
    private val loadConversationHistoryUseCase = AppGraph.loadConversationHistoryUseCase
    private val sendMessageUseCase = AppGraph.sendMessageUseCase
    private val loadConversationDetailsUseCase = AppGraph.loadConversationDetailsUseCase
    private val resolveUserIdByUsernameUseCase = AppGraph.resolveUserIdByUsernameUseCase
    private val addConversationMemberUseCase = AppGraph.addConversationMemberUseCase
    private val fetchUsernamesBatchByUserIdsUseCase = AppGraph.fetchUsernamesBatchByUserIdsUseCase
    private val registerUsernamesBatchUseCase = AppGraph.registerUsernamesBatchUseCase
    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val timestampFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

    private lateinit var session: UserSessionArgs
    private lateinit var conversationId: String
    private var conversationType: Int = TYPE_DM
    private var canManageMembers: Boolean = false
    private var canSendMessages: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentConversationBinding.bind(view)

        val args = arguments
        session = args?.readUserSessionArgs() ?: fallbackMockSession()
        if (session.accessToken.isBlank() || session.serverBaseUrl.isBlank()) {
            session = session.copy(isMock = true)
        }

        conversationId = args?.getString(ARG_CONVERSATION_ID).orEmpty().ifBlank {
            "mock_conversation"
        }
        conversationType = args?.getInt(ARG_CONVERSATION_TYPE, TYPE_DM) ?: TYPE_DM
        val title = args?.getString(ARG_TITLE).orEmpty().ifBlank {
            getString(R.string.conversation_default_title)
        }
        val typeLabel = args?.getString(ARG_TYPE_LABEL).orEmpty()
        val toolbar: MaterialToolbar = binding.root.findViewById(R.id.conversationHeader)

        toolbar.title = title
        toolbar.subtitle =
            if (session.isMock) {
                "$typeLabel (mock)"
            } else {
                typeLabel
            }
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        configureConversationActions(toolbar)

        binding.messagesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext()).apply {
                reverseLayout = false
                stackFromEnd = false
            }
        binding.messagesRecyclerView.adapter = messageAdapter

        binding.sendButton.setOnClickListener {
            onSendPressed()
        }
        binding.attachmentButton.setOnClickListener {
            onAttachmentPressed()
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            messageAdapter.submitList(messages)
        }

        applyComposerAccess()
        loadConversationMetadata(toolbar)
        loadConversationHistory()
    }

    override fun onDestroyView() {
        mainHandler.removeCallbacksAndMessages(null)
        binding.messagesRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        backgroundExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun loadConversationHistory() {
        if (session.isMock) {
            viewModel.loadMockMessages()
            if (conversationType == TYPE_DM) {
                viewModel.replaceMessages(
                    viewModel.messages.value.orEmpty().map { it.copy(authorName = null) },
                )
            }
            return
        }

        Toast.makeText(requireContext(), R.string.conversation_loading, Toast.LENGTH_SHORT).show()
        backgroundExecutor.execute {
            val result = loadConversationHistoryUseCase(
                serverBaseUrl = session.serverBaseUrl,
                accessToken = session.accessToken,
                conversationId = conversationId,
            )
            mainHandler.post {
                if (_binding == null) {
                    return@post
                }
                when (result) {
                    is VoxResult.Success -> {
                        val senderNameByUserId = resolveSenderNames(result.value)
                        val messages =
                            result.value.map { envelope ->
                                val decryptedBody = MessageCipherCodec.decryptOrNull(envelope.ciphertext)
                                val authorName =
                                    resolveAuthorName(
                                        senderUserId = envelope.senderUserId,
                                        senderDeviceId = envelope.senderDeviceId,
                                        senderNameByUserId = senderNameByUserId,
                                    )
                                ConversationMessageUi(
                                    id = envelope.envelopeId,
                                    body = decryptedBody ?: getString(R.string.conversation_encrypted_message, shortId(envelope.envelopeId)),
                                    timestampText = formatTimestamp(envelope.serverTimestamp),
                                    isOutgoing =
                                        if (envelope.senderUserId.isNullOrBlank()) {
                                            envelope.senderDeviceId == session.deviceId
                                        } else {
                                            envelope.senderUserId == session.userId &&
                                                envelope.senderDeviceId == session.deviceId
                                        },
                                    authorName = authorName,
                                )
                            }
                        viewModel.replaceMessages(messages)
                    }

                    is VoxResult.Failure -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.conversation_error_load, result.error.message),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }
    }

    private fun onSendPressed() {
        if (!canSendMessages) {
            Toast.makeText(requireContext(), R.string.conversation_channel_read_only, Toast.LENGTH_SHORT).show()
            return
        }

        val rawInput = binding.messageInput.text?.toString().orEmpty()
        if (rawInput.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.conversation_missing_message_warning, Toast.LENGTH_SHORT).show()
            return
        }

        if (session.isMock) {
            viewModel.sendMessage(rawInput, localAuthorName())
            binding.messageInput.text?.clear()
            return
        }

        binding.sendButton.isEnabled = false
        val ciphertext = MessageCipherCodec.encrypt(rawInput)
        val command =
            VoxSendMessageCommand(
                deviceId = session.deviceId,
                conversationId = conversationId,
                ciphertext = ciphertext,
                envelopeId = "env_${UUID.randomUUID()}",
                envelopeType = 0,
                orderingEpoch = null,
            )
        backgroundExecutor.execute {
            val result = sendMessageUseCase(
                serverBaseUrl = session.serverBaseUrl,
                accessToken = session.accessToken,
                command = command,
            )
            mainHandler.post {
                if (_binding == null) {
                    return@post
                }
                binding.sendButton.isEnabled = true
                when (result) {
                    is VoxResult.Success -> {
                        viewModel.sendMessage(rawInput, localAuthorName())
                        binding.messageInput.text?.clear()
                    }

                    is VoxResult.Failure -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.conversation_error_send, result.error.message),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }
    }

    private fun onAttachmentPressed() {
        viewModel.addAttachmentPlaceholder(
            placeholder = getString(R.string.conversation_attachment_placeholder),
            authorName = localAuthorName(),
        )
    }

    private fun resolveSenderNames(envelopes: List<VoxConversationEnvelope>): Map<String, String> {
        val senderUserIds =
            envelopes
                .mapNotNull { it.senderUserId?.trim() }
                .filter { it.isNotBlank() }
                .distinct()

        if (senderUserIds.isEmpty()) {
            return emptyMap()
        }

        val resolved =
            when (
                val result =
                    fetchUsernamesBatchByUserIdsUseCase(
                        serverBaseUrl = session.serverBaseUrl,
                        accessToken = session.accessToken,
                        userIds = senderUserIds,
                    )
            ) {
                is VoxResult.Success -> result.value.toMutableMap()
                is VoxResult.Failure -> mutableMapOf()
            }

        if (session.userId.isNotBlank() && session.username.isNotBlank()) {
            resolved[session.userId] = session.username
        }

        if (resolved.isNotEmpty()) {
            registerUsernamesBatchUseCase(
                serverBaseUrl = session.serverBaseUrl,
                accessToken = session.accessToken,
                usernamesByUserId = resolved,
            )
        }

        return resolved
    }

    private fun resolveAuthorName(
        senderUserId: String?,
        senderDeviceId: String,
        senderNameByUserId: Map<String, String>,
    ): String? {
        if (conversationType == TYPE_DM) {
            return null
        }

        if (!senderUserId.isNullOrBlank()) {
            if (senderUserId == session.userId && session.username.isNotBlank()) {
                return session.username
            }
            return senderNameByUserId[senderUserId]?.takeIf { it.isNotBlank() } ?: sixCharPiece(senderUserId)
        }

        return if (senderDeviceId == session.deviceId && session.username.isNotBlank()) {
            session.username
        } else {
            sixCharPiece(senderDeviceId)
        }
    }

    private fun localAuthorName(): String? =
        if (conversationType == TYPE_DM) {
            null
        } else {
            session.username.ifBlank { shortId(session.userId) }
        }

    private fun configureConversationActions(toolbar: MaterialToolbar) {
        if (conversationType == TYPE_DM) {
            toolbar.menu.clear()
            return
        }
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.conversation_menu)
        toolbar.menu.findItem(R.id.menuAddMember)?.isVisible =
            conversationType == TYPE_GROUP && canManageMembers
        toolbar.menu.findItem(R.id.menuShareChannelId)?.isVisible = conversationType == TYPE_CHANNEL
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuAddMember -> {
                    showAddMemberDialog()
                    true
                }

                R.id.menuShareChannelId -> {
                    shareChannelId()
                    true
                }

                else -> false
            }
        }
    }

    private fun loadConversationMetadata(toolbar: MaterialToolbar) {
        if (session.isMock) {
            canManageMembers = conversationType == TYPE_GROUP
            canSendMessages = true
            configureConversationActions(toolbar)
            applyComposerAccess()
            return
        }
        backgroundExecutor.execute {
            val result =
                loadConversationDetailsUseCase(
                    serverBaseUrl = session.serverBaseUrl,
                    accessToken = session.accessToken,
                    conversationId = conversationId,
                )
            mainHandler.post {
                if (_binding == null) {
                    return@post
                }
                val myRole = (result as? VoxResult.Success)?.value?.myRole?.lowercase()
                canManageMembers =
                    when (myRole) {
                        "owner", "admin" -> true
                        else -> false
                    }
                canSendMessages =
                    if (conversationType == TYPE_CHANNEL) {
                        myRole == "owner" || myRole == "admin"
                    } else {
                        true
                    }
                configureConversationActions(toolbar)
                applyComposerAccess()
            }
        }
    }

    private fun applyComposerAccess() {
        if (_binding == null) {
            return
        }
        binding.inputRow.isVisible = canSendMessages
    }

    private fun shareChannelId() {
        if (conversationType != TYPE_CHANNEL) {
            return
        }
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("channel_id", conversationId),
        )
        Toast.makeText(
            requireContext(),
            getString(R.string.conversation_channel_id_copied, conversationId),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun showAddMemberDialog() {
        val dialogBinding = DialogAddMemberBinding.inflate(layoutInflater)
        dialogBinding.addAsAdminCheck.isEnabled = conversationType != TYPE_DM
        val dialog =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.conversation_action_add_member)
                .setView(dialogBinding.root)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.chat_create_action_create, null)
                .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val username = dialogBinding.memberUsernameInput.text?.toString().orEmpty().trim()
                if (username.isEmpty()) {
                    dialogBinding.memberUsernameInputLayout.error = getString(R.string.chat_create_error_username_required)
                    return@setOnClickListener
                }
                dialogBinding.memberUsernameInputLayout.error = null
                dialog.dismiss()
                addMemberToConversation(username, dialogBinding.addAsAdminCheck.isChecked)
            }
        }
        dialog.show()
    }

    private fun addMemberToConversation(username: String, asAdmin: Boolean) {
        if (session.isMock) {
            Toast.makeText(requireContext(), R.string.conversation_add_member_success, Toast.LENGTH_SHORT).show()
            return
        }
        if (!canManageMembers) {
            Toast.makeText(requireContext(), R.string.conversation_add_member_no_permission, Toast.LENGTH_SHORT).show()
            return
        }
        backgroundExecutor.execute {
            val resolved =
                resolveUserIdByUsernameUseCase(
                    serverBaseUrl = session.serverBaseUrl,
                    accessToken = session.accessToken,
                    username = username,
                )
            val addResult =
                if (resolved is VoxResult.Success) {
                    addConversationMemberUseCase(
                        serverBaseUrl = session.serverBaseUrl,
                        accessToken = session.accessToken,
                        conversationId = conversationId,
                        userId = resolved.value,
                        role = if (asAdmin) "admin" else "member",
                    )
                } else {
                    resolved
                }

            mainHandler.post {
                if (_binding == null) {
                    return@post
                }
                when (addResult) {
                    is VoxResult.Success -> {
                        Toast.makeText(requireContext(), R.string.conversation_add_member_success, Toast.LENGTH_SHORT).show()
                    }

                    is VoxResult.Failure -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.conversation_add_member_error, addResult.error.message),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }
    }

    private fun formatTimestamp(unixSeconds: Long): String {
        return try {
            Instant.ofEpochSecond(unixSeconds)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(timestampFormatter)
        } catch (_: Throwable) {
            ""
        }
    }

    private fun shortId(value: String): String =
        if (value.length <= 6) value else value.takeLast(6)

    private fun sixCharPiece(value: String): String = shortId(value)

    private fun fallbackMockSession(): UserSessionArgs {
        Log.w(TAG, "Missing ConversationFragment args, using fallback mock session")
        return UserSessionArgs(
            serverBaseUrl = "https://mock.local/",
            accessToken = "",
            deviceId = "mock_device_id",
            userId = "mock_user_id",
            username = "mock",
            isMock = true,
        )
    }

    companion object {
        private const val TAG = "ConversationFragment"
        private const val ARG_CONVERSATION_ID = "conversation_id"
        private const val ARG_CONVERSATION_TYPE = "conversation_type"
        private const val ARG_TITLE = "title"
        private const val ARG_TYPE_LABEL = "type_label"
        private const val TYPE_DM = 0
        private const val TYPE_GROUP = 1
        private const val TYPE_CHANNEL = 2

        fun newInstance(
            conversationId: String,
            conversationType: Int,
            title: String,
            typeLabel: String,
            session: UserSessionArgs,
        ): ConversationFragment =
            ConversationFragment().apply {
                arguments =
                    bundleOf(
                        ARG_CONVERSATION_ID to conversationId,
                        ARG_CONVERSATION_TYPE to conversationType,
                        ARG_TITLE to title,
                        ARG_TYPE_LABEL to typeLabel,
                    ).apply {
                        putAll(session.toBundle())
                    }
            }
    }
}
