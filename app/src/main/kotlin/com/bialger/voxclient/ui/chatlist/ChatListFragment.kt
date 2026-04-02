package com.bialger.voxclient.ui.chatlist

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.bialger.voxclient.R
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.databinding.DialogCreateGroupChannelBinding
import com.bialger.voxclient.databinding.DialogCreateChatBinding
import com.bialger.voxclient.databinding.FragmentChatListBinding
import com.bialger.voxclient.di.AppGraph
import com.bialger.voxclient.domain.entity.VoxConversationMember
import com.bialger.voxclient.domain.entity.VoxConversationMembers
import com.bialger.voxclient.domain.entity.VoxConversationSummary
import com.bialger.voxclient.ui.auth.AuthFragment
import com.bialger.voxclient.ui.conversation.ConversationFragment
import com.bialger.voxclient.ui.session.UserSessionArgs
import com.bialger.voxclient.ui.session.readUserSessionArgs
import com.bialger.voxclient.ui.session.toBundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding ?: error("Binding is only valid between onViewCreated and onDestroyView")

    private val viewModel: ChatListViewModel by viewModels()
    private val folderPagerAdapter = ChatFoldersPagerAdapter(::openConversation)
    private val loadChatListUseCase = AppGraph.loadChatListUseCase
    private val resolveUserIdByUsernameUseCase = AppGraph.resolveUserIdByUsernameUseCase
    private val fetchUsernameByUserIdUseCase = AppGraph.fetchUsernameByUserIdUseCase
    private val fetchUsernamesBatchByUserIdsUseCase = AppGraph.fetchUsernamesBatchByUserIdsUseCase
    private val registerUsernamesBatchUseCase = AppGraph.registerUsernamesBatchUseCase
    private val createDmUseCase = AppGraph.createDmUseCase
    private val createGroupUseCase = AppGraph.createGroupUseCase
    private val createChannelUseCase = AppGraph.createChannelUseCase
    private val subscribeToChannelUseCase = AppGraph.subscribeToChannelUseCase
    private val loadConversationDetailsUseCase = AppGraph.loadConversationDetailsUseCase
    private val loadConversationMembersUseCase = AppGraph.loadConversationMembersUseCase
    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val timestampFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    private val dmPeerUserIdByConversationId = mutableMapOf<String, String>()
    private val channelTitleByConversationId = mutableMapOf<String, String>()
    private val usernameByUserId = mutableMapOf<String, String>()
    private val pendingConversationItemsById = linkedMapOf<String, ChatListItemUi>()

    private lateinit var loadingStateView: View
    private lateinit var emptyStateView: View
    private lateinit var emptySubtitleView: TextView
    private lateinit var session: UserSessionArgs
    private var currentFolderPosition: Int = 0
    private var folderTabsMediator: TabLayoutMediator? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatListBinding.bind(view)
        loadingStateView = view.findViewById(R.id.loadingState)
        emptyStateView = view.findViewById(R.id.emptyState)
        emptySubtitleView = view.findViewById(R.id.emptySubtitle)
        session = requireArguments().readUserSessionArgs()

        binding.chatListToolbar.subtitle =
            if (session.isMock) {
                "mock"
            } else {
                session.username.ifBlank { session.userId }
            }
        binding.chatListToolbar.inflateMenu(R.menu.chat_list_menu)
        binding.chatListToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuSwitchAccount -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.mainFragmentContainer, AuthFragment.newInstance(session.serverBaseUrl))
                        .addToBackStack(AuthFragment::class.java.simpleName)
                        .commit()
                    true
                }

                else -> false
            }
        }

        binding.chatFoldersPager.offscreenPageLimit = FOLDER_PAGE_COUNT
        binding.chatFoldersPager.adapter = folderPagerAdapter
        folderTabsMediator =
            TabLayoutMediator(binding.chatFoldersTabs, binding.chatFoldersPager) { tab, position ->
                tab.text =
                    when (folderPagerAdapter.getFolderTypeAt(position)) {
                        TYPE_DM -> getString(R.string.chat_list_folder_dm)
                        TYPE_GROUP -> getString(R.string.chat_list_folder_group)
                        TYPE_CHANNEL -> getString(R.string.chat_list_folder_channel)
                        else -> getString(R.string.chat_list_folder_dm)
                    }
            }.apply {
                attach()
            }
        pageChangeCallback =
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentFolderPosition = position
                    viewModel.uiState.value?.let(::render)
                }
            }.also { callback ->
                binding.chatFoldersPager.registerOnPageChangeCallback(callback)
            }
        binding.newChatFab.setOnClickListener {
            showCreateConversationTypeDialog()
        }

        binding.searchInput.doAfterTextChanged { editable ->
            viewModel.onSearchQueryChanged(editable?.toString().orEmpty())
        }
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            render(state)
        }

        loadConversations()
    }

    override fun onDestroyView() {
        mainHandler.removeCallbacksAndMessages(null)
        pageChangeCallback?.let { callback ->
            binding.chatFoldersPager.unregisterOnPageChangeCallback(callback)
        }
        pageChangeCallback = null
        folderTabsMediator?.detach()
        folderTabsMediator = null
        binding.chatFoldersPager.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        backgroundExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun loadConversations() {
        if (session.isMock) {
            viewModel.loadMockItems()
            return
        }

        viewModel.setLoading()
        backgroundExecutor.execute {
            val result = loadChatListUseCase(session.serverBaseUrl, session.accessToken)
            mainHandler.post {
                if (_binding == null) {
                    return@post
                }
                when (result) {
                    is VoxResult.Success -> {
                        val items = mergeServerItemsWithPending(buildChatListItems(result.value))
                        viewModel.setItems(items)
                    }

                    is VoxResult.Failure -> {
                        val message = getString(R.string.chat_list_error_loading, result.error.message)
                        if (viewModel.uiState.value?.items.isNullOrEmpty()) {
                            viewModel.setError(message)
                        } else {
                            viewModel.setNonBlockingError(message)
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: ChatListUiState) {
        loadingStateView.isVisible = state.isLoading

        folderPagerAdapter.submitItems(state.items)
        val selectedFolderType = folderPagerAdapter.getFolderTypeAt(currentFolderPosition)
        val hasItems = folderPagerAdapter.getCountForType(selectedFolderType) > 0
        binding.chatFoldersTabs.isVisible = !state.isLoading
        binding.chatFoldersPager.isVisible = !state.isLoading && hasItems
        emptyStateView.isVisible = !state.isLoading && !hasItems

        emptySubtitleView.text =
            when {
                state.emptyMessage != null -> state.emptyMessage
                state.query.isBlank() ->
                    when (selectedFolderType) {
                        TYPE_DM -> getString(R.string.chat_list_empty_subtitle_dm)
                        TYPE_GROUP -> getString(R.string.chat_list_empty_subtitle_group)
                        TYPE_CHANNEL -> getString(R.string.chat_list_empty_subtitle_channel)
                        else -> getString(R.string.chat_list_empty_subtitle)
                    }
                else -> getString(R.string.chat_list_empty_for_query, state.query)
            }
    }

    private fun buildChatListItems(conversations: List<VoxConversationSummary>): List<ChatListItemUi> {
        val cachedDmPeerByConversationId = dmPeerUserIdByConversationId.toMap()
        val cachedChannelTitlesByConversationId = channelTitleByConversationId.toMap()
        val cachedUsernameByUserId = usernameByUserId.toMap()
        dmPeerUserIdByConversationId.clear()
        channelTitleByConversationId.clear()
        usernameByUserId.clear()
        dmPeerUserIdByConversationId.putAll(cachedDmPeerByConversationId)
        channelTitleByConversationId.putAll(cachedChannelTitlesByConversationId)
        usernameByUserId.putAll(cachedUsernameByUserId)
        if (session.userId.isNotBlank() && session.username.isNotBlank()) {
            usernameByUserId[session.userId] = session.username
        }
        val unresolvedUserIds = linkedSetOf<String>()

        conversations.forEach { conversation ->
            rememberResolvedUsername(
                userId = conversation.createdBy,
                username = conversation.createdByUsername,
                unresolvedUserIds = unresolvedUserIds,
            )
            val membersResult =
                loadConversationMembersUseCase(
                    serverBaseUrl = session.serverBaseUrl,
                    accessToken = session.accessToken,
                    conversationId = conversation.conversationId,
                )
            if (membersResult is VoxResult.Success) {
                rememberUsernamesFromMembers(
                    response = membersResult.value,
                    unresolvedUserIds = unresolvedUserIds,
                )
                val memberIds = extractMemberUserIds(membersResult.value)
                if (conversation.type == TYPE_DM) {
                    val peerUserId =
                        memberIds.firstOrNull { it != session.userId }
                            ?: memberIds.firstOrNull()
                    if (!peerUserId.isNullOrBlank()) {
                        dmPeerUserIdByConversationId[conversation.conversationId] = peerUserId
                    }
                }
            }

            if (conversation.type == TYPE_CHANNEL) {
                val detailResult =
                    loadConversationDetailsUseCase(
                        serverBaseUrl = session.serverBaseUrl,
                        accessToken = session.accessToken,
                        conversationId = conversation.conversationId,
                    )
                if (detailResult is VoxResult.Success) {
                    val title = detailResult.value.title.orEmpty().trim()
                    if (title.isNotEmpty()) {
                        channelTitleByConversationId[conversation.conversationId] = title
                    }
                }
            }
        }

        if (usernameByUserId.isNotEmpty()) {
            registerUsernamesBatchUseCase(
                serverBaseUrl = session.serverBaseUrl,
                accessToken = session.accessToken,
                usernamesByUserId = usernameByUserId,
            )
        }

        if (unresolvedUserIds.isNotEmpty()) {
            when (
                val resolved =
                    fetchUsernamesBatchByUserIdsUseCase(
                        serverBaseUrl = session.serverBaseUrl,
                        accessToken = session.accessToken,
                        userIds = unresolvedUserIds,
                    )
            ) {
                is VoxResult.Success -> {
                    resolved.value.forEach { (userId, username) ->
                        rememberResolvedUsername(
                            userId = userId,
                            username = username,
                            unresolvedUserIds = unresolvedUserIds,
                        )
                    }
                }

                is VoxResult.Failure -> Unit
            }
        }

        return conversations
            .sortedByDescending { it.lastActivityAt ?: it.createdAt }
            .map { it.toChatListItem() }
    }

    private fun rememberUsernamesFromMembers(
        response: VoxConversationMembers,
        unresolvedUserIds: MutableSet<String>,
    ) {
        response.members.orEmpty().forEach { member ->
            rememberResolvedUsername(
                userId = member.userId,
                username = member.username,
                unresolvedUserIds = unresolvedUserIds,
            )
        }
        response.admins.orEmpty().forEach { member ->
            rememberResolvedUsername(
                userId = member.userId,
                username = member.username,
                unresolvedUserIds = unresolvedUserIds,
            )
        }
        response.subscribers.orEmpty().forEach { member ->
            rememberResolvedUsername(
                userId = member.userId,
                username = member.username,
                unresolvedUserIds = unresolvedUserIds,
            )
        }
    }

    private fun rememberResolvedUsername(
        userId: String?,
        username: String?,
        unresolvedUserIds: MutableSet<String>,
    ) {
        val normalizedUserId = userId?.trim().orEmpty()
        if (normalizedUserId.isEmpty()) {
            return
        }

        val normalizedUsername = username?.trim().orEmpty()
        if (normalizedUsername.isNotEmpty()) {
            usernameByUserId[normalizedUserId] = normalizedUsername
            unresolvedUserIds.remove(normalizedUserId)
            return
        }

        if (!usernameByUserId.containsKey(normalizedUserId)) {
            unresolvedUserIds += normalizedUserId
        }
    }

    private fun extractMemberUserIds(response: VoxConversationMembers): List<String> {
        val ids =
            buildList {
                addAll(response.members.map(VoxConversationMember::userId))
                addAll(response.admins.map(VoxConversationMember::userId))
                addAll(response.subscribers.map(VoxConversationMember::userId))
            }
        return ids.distinct()
    }

    private fun VoxConversationSummary.toChatListItem(): ChatListItemUi {
        val typeLabel =
            when (type) {
                TYPE_DM -> getString(R.string.chat_list_type_dm)
                TYPE_GROUP -> getString(R.string.chat_list_type_group)
                TYPE_CHANNEL -> getString(R.string.chat_list_type_channel)
                else -> "UNKNOWN"
            }

        val title =
            when (type) {
                TYPE_DM -> resolveDmTitle()
                TYPE_GROUP -> "${getString(R.string.chat_list_type_group)} ${shortId(conversationId)}"
                TYPE_CHANNEL -> channelTitleByConversationId[conversationId]
                    ?: "${getString(R.string.chat_list_type_channel)} ${shortId(conversationId)}"
                else -> shortId(conversationId)
            }

        return ChatListItemUi(
            conversationId = conversationId,
            conversationType = type,
            title = title,
            preview = getString(R.string.chat_list_preview_encrypted),
            timestampText = formatTimestamp(lastActivityAt ?: createdAt),
            unreadCount = 0,
            typeLabel = typeLabel,
            isMuted = false,
            isPinned = false,
            isEncrypted = true,
        )
    }

    private fun VoxConversationSummary.resolveDmTitle(): String {
        val rawPeerUserId = dmPeerUserIdByConversationId[conversationId]
        val peerUserId =
            when {
                !rawPeerUserId.isNullOrBlank() && rawPeerUserId != session.userId -> rawPeerUserId
                createdBy != session.userId -> createdBy
                else -> rawPeerUserId
            }

        if (peerUserId == session.userId) {
            return session.username.ifBlank { sixCharPiece(session.userId) }
        }

        if (!peerUserId.isNullOrBlank()) {
            val cachedUsername = usernameByUserId[peerUserId]?.trim().orEmpty()
            if (cachedUsername.isNotEmpty()) {
                return cachedUsername
            }
            return sixCharPiece(peerUserId)
        }

        val createdByUsername = usernameByUserId[createdBy]?.trim().orEmpty()
        if (createdByUsername.isNotEmpty()) {
            return createdByUsername
        }

        return sixCharPiece(conversationId)
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

    private fun mergeServerItemsWithPending(serverItems: List<ChatListItemUi>): List<ChatListItemUi> {
        val pendingIdsToRemove = mutableSetOf<String>()
        val mergedServerItems =
            serverItems.map { item ->
                val pending = pendingConversationItemsById[item.conversationId] ?: return@map item
                if (shouldPreferPendingTitle(item, pending)) {
                    item.copy(title = pending.title)
                } else {
                    pendingIdsToRemove += item.conversationId
                    item
                }
            }

        pendingConversationItemsById.keys.removeAll(pendingIdsToRemove)
        val serverIds = mergedServerItems.map { it.conversationId }.toSet()
        val pendingOnly = pendingConversationItemsById.values.filter { it.conversationId !in serverIds }
        return pendingOnly + mergedServerItems
    }

    private fun shouldPreferPendingTitle(serverItem: ChatListItemUi, pendingItem: ChatListItemUi): Boolean {
        if (pendingItem.title.isBlank()) {
            return false
        }

        val weakServerTitle =
            serverItem.title.isBlank() ||
                serverItem.title == getString(R.string.chat_list_unknown_user) ||
                serverItem.title == getString(R.string.chat_list_type_dm) ||
                serverItem.title == getString(R.string.chat_list_type_group) ||
                serverItem.title == getString(R.string.chat_list_type_channel)
        return weakServerTitle
    }

    private fun registerPendingConversation(item: ChatListItemUi) {
        pendingConversationItemsById[item.conversationId] = item
    }

    private fun switchToFolder(conversationType: Int) {
        val targetPosition =
            when (conversationType) {
                TYPE_GROUP -> 1
                TYPE_CHANNEL -> 2
                else -> 0
            }
        currentFolderPosition = targetPosition
        if (_binding != null) {
            binding.chatFoldersPager.setCurrentItem(targetPosition, true)
        }
    }

    private fun openConversation(item: ChatListItemUi) {
        if (!isAdded) {
            return
        }
        runCatching {
            val transaction =
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(
                        R.id.mainFragmentContainer,
                        ConversationFragment.newInstance(
                            conversationId = item.conversationId,
                            conversationType = item.conversationType,
                            title = item.title,
                            typeLabel = item.typeLabel,
                            session = session,
                        ),
                    )
                    .addToBackStack(ConversationFragment::class.java.simpleName)

            if (parentFragmentManager.isStateSaved) {
                transaction.commitAllowingStateLoss()
            } else {
                transaction.commit()
            }
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to open conversation ${item.conversationId}", throwable)
            Toast.makeText(
                requireContext(),
                R.string.chat_list_error_open_conversation,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun showCreateConversationTypeDialog() {
        val options =
            arrayOf(
                getString(R.string.chat_create_type_direct),
                getString(R.string.chat_create_type_group),
                getString(R.string.chat_create_type_channel),
                getString(R.string.chat_create_type_subscribe_channel),
            )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.chat_create_type_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateDirectChatDialog()
                    1 -> showCreateGroupOrChannelDialog(isChannel = false)
                    2 -> showCreateGroupOrChannelDialog(isChannel = true)
                    3 -> showSubscribeChannelDialog()
                }
            }.show()
    }

    private fun showCreateDirectChatDialog() {
        val dialogBinding = DialogCreateChatBinding.inflate(layoutInflater)
        val dialog =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.chat_create_type_direct)
                .setView(dialogBinding.root)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.chat_create_action_create, null)
                .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val peerUsername = dialogBinding.peerUsernameInput.text?.toString().orEmpty().trim()
                if (peerUsername.isEmpty()) {
                    dialogBinding.peerUsernameInputLayout.error = getString(R.string.chat_create_error_username_required)
                    return@setOnClickListener
                }
                dialogBinding.peerUsernameInputLayout.error = null
                dialog.dismiss()
                createDirectChat(peerUsername)
            }
        }
        dialog.show()
    }

    private fun showSubscribeChannelDialog() {
        val dialogBinding = DialogCreateChatBinding.inflate(layoutInflater)
        dialogBinding.peerUsernameInputLayout.hint = getString(R.string.chat_subscribe_channel_id_hint)
        val dialog =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.chat_create_type_subscribe_channel)
                .setView(dialogBinding.root)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.chat_subscribe_action, null)
                .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val channelId = normalizeChannelId(dialogBinding.peerUsernameInput.text?.toString().orEmpty())
                if (channelId.isEmpty()) {
                    dialogBinding.peerUsernameInputLayout.error =
                        getString(R.string.chat_subscribe_error_channel_id_required)
                    return@setOnClickListener
                }
                dialogBinding.peerUsernameInputLayout.error = null
                dialog.dismiss()
                subscribeToChannel(channelId)
            }
        }
        dialog.show()
    }

    private fun showCreateGroupOrChannelDialog(isChannel: Boolean) {
        val dialogBinding = DialogCreateGroupChannelBinding.inflate(layoutInflater)
        val titleRes =
            if (isChannel) {
                R.string.chat_create_type_channel
            } else {
                R.string.chat_create_type_group
            }
        dialogBinding.memberUsernamesInputLayout.isVisible = !isChannel
        dialogBinding.adminUsernamesInputLayout.isVisible = isChannel
        dialogBinding.adminUsernamesInput.setText(session.username)

        val dialog =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setView(dialogBinding.root)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.chat_create_action_create, null)
                .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val members = parseUsernames(dialogBinding.memberUsernamesInput.text?.toString().orEmpty())
                val admins = parseUsernames(dialogBinding.adminUsernamesInput.text?.toString().orEmpty())
                if (!isChannel && members.isEmpty()) {
                    dialogBinding.memberUsernamesInputLayout.error = getString(R.string.chat_create_error_members_required)
                    return@setOnClickListener
                }
                dialogBinding.memberUsernamesInputLayout.error = null
                dialog.dismiss()
                createGroupOrChannel(
                    memberUsernames = members,
                    adminUsernames = admins,
                    isChannel = isChannel,
                )
            }
        }
        dialog.show()
    }

    private fun parseUsernames(raw: String): List<String> =
        raw
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }

    private fun createDirectChat(peerUsername: String) {
        if (session.isMock) {
            val mockConversationId = "mock_${System.currentTimeMillis()}"
            val item =
                ChatListItemUi(
                    conversationId = mockConversationId,
                    conversationType = TYPE_DM,
                    title = shortId(mockConversationId),
                    preview = getString(R.string.chat_list_preview_encrypted),
                    timestampText = "",
                    unreadCount = 0,
                    typeLabel = getString(R.string.chat_list_type_dm),
                    isMuted = false,
                    isPinned = false,
                    isEncrypted = true,
                )
            registerPendingConversation(item)
            viewModel.addItem(item)
            switchToFolder(TYPE_DM)
            openConversation(item)
            return
        }

        Toast.makeText(requireContext(), R.string.chat_create_status_creating, Toast.LENGTH_SHORT).show()
        backgroundExecutor.execute {
            val resolved = resolveUserIdByUsernameUseCase(
                serverBaseUrl = session.serverBaseUrl,
                accessToken = session.accessToken,
                username = peerUsername,
            )
            val resolvedPeerUsername =
                if (resolved is VoxResult.Success && resolved.value.isNotBlank()) {
                    when (
                        val usernameResult =
                            fetchUsernameByUserIdUseCase(
                                serverBaseUrl = session.serverBaseUrl,
                                accessToken = session.accessToken,
                                userId = resolved.value,
                            )
                    ) {
                        is VoxResult.Success -> usernameResult.value.trim().ifBlank { null }
                        is VoxResult.Failure -> null
                    }
                } else {
                    null
                }
            val peerTitle =
                if (resolved is VoxResult.Success && resolved.value.isNotBlank()) {
                    if (resolved.value == session.userId) {
                        session.username.ifBlank { sixCharPiece(session.userId) }
                    } else {
                        resolvedPeerUsername ?: peerUsername
                    }
                } else {
                    peerUsername
                }
            if (resolved is VoxResult.Success && resolved.value.isNotBlank() && !resolvedPeerUsername.isNullOrBlank()) {
                registerUsernamesBatchUseCase(
                    serverBaseUrl = session.serverBaseUrl,
                    accessToken = session.accessToken,
                    usernamesByUserId = mapOf(resolved.value to resolvedPeerUsername),
                )
                usernameByUserId[resolved.value] = resolvedPeerUsername
            }
            val creation =
                if (resolved is VoxResult.Success) {
                    createDmUseCase(
                        serverBaseUrl = session.serverBaseUrl,
                        accessToken = session.accessToken,
                        peerUserId = resolved.value,
                    )
                } else {
                    resolved
                }

            mainHandler.post {
                if (_binding == null) {
                    return@post
                }
                when (creation) {
                    is VoxResult.Success -> {
                        val item =
                            ChatListItemUi(
                                conversationId = creation.value,
                                conversationType = TYPE_DM,
                                title = peerTitle,
                                preview = getString(R.string.chat_list_preview_encrypted),
                                timestampText = "",
                                unreadCount = 0,
                                typeLabel = getString(R.string.chat_list_type_dm),
                                isMuted = false,
                                isPinned = false,
                                isEncrypted = true,
                            )
                        registerPendingConversation(item)
                        viewModel.addItem(item)
                        switchToFolder(TYPE_DM)
                        mainHandler.postDelayed(::loadConversations, REFRESH_AFTER_CREATE_MS)
                        openConversation(item)
                    }

                    is VoxResult.Failure -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.chat_create_error_failed, creation.error.message),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }
    }

    private fun subscribeToChannel(channelId: String) {
        if (session.isMock) {
            val item =
                ChatListItemUi(
                    conversationId = channelId,
                    conversationType = TYPE_CHANNEL,
                    title = "${getString(R.string.chat_list_type_channel)} ${shortId(channelId)}",
                    preview = getString(R.string.chat_list_preview_encrypted),
                    timestampText = "",
                    unreadCount = 0,
                    typeLabel = getString(R.string.chat_list_type_channel),
                    isMuted = false,
                    isPinned = false,
                    isEncrypted = true,
                )
            registerPendingConversation(item)
            viewModel.addItem(item)
            switchToFolder(TYPE_CHANNEL)
            openConversation(item)
            return
        }

        Toast.makeText(requireContext(), R.string.chat_subscribe_status_subscribing, Toast.LENGTH_SHORT).show()
        backgroundExecutor.execute {
            val result =
                subscribeToChannelUseCase(
                    serverBaseUrl = session.serverBaseUrl,
                    accessToken = session.accessToken,
                    conversationId = channelId,
                )
            mainHandler.post {
                if (_binding == null) {
                    return@post
                }
                when (result) {
                    is VoxResult.Success -> {
                        val item =
                            ChatListItemUi(
                                conversationId = channelId,
                                conversationType = TYPE_CHANNEL,
                                title = "${getString(R.string.chat_list_type_channel)} ${shortId(channelId)}",
                                preview = getString(R.string.chat_list_preview_encrypted),
                                timestampText = "",
                                unreadCount = 0,
                                typeLabel = getString(R.string.chat_list_type_channel),
                                isMuted = false,
                                isPinned = false,
                                isEncrypted = true,
                            )
                        registerPendingConversation(item)
                        viewModel.addItem(item)
                        switchToFolder(TYPE_CHANNEL)
                        loadConversations()
                        mainHandler.postDelayed(::loadConversations, REFRESH_AFTER_CREATE_MS)
                        mainHandler.postDelayed(::loadConversations, REFRESH_AFTER_SUBSCRIBE_SECOND_MS)
                        Toast.makeText(
                            requireContext(),
                            R.string.chat_subscribe_success,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }

                    is VoxResult.Failure -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.chat_subscribe_error_failed, result.error.message),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }
    }

    private fun normalizeChannelId(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return ""
        }
        val matched = CHANNEL_ID_REGEX.find(trimmed)?.value
        return matched ?: trimmed
    }

    private fun createGroupOrChannel(
        memberUsernames: List<String>,
        adminUsernames: List<String>,
        isChannel: Boolean,
    ) {
        if (session.isMock) {
            val type = if (isChannel) TYPE_CHANNEL else TYPE_GROUP
            val typeLabel =
                if (isChannel) {
                    getString(R.string.chat_list_type_channel)
                } else {
                    getString(R.string.chat_list_type_group)
                }
            val mockConversationId = "mock_${System.currentTimeMillis()}"
            val item =
                ChatListItemUi(
                    conversationId = mockConversationId,
                    conversationType = type,
                    title = "$typeLabel ${shortId(mockConversationId)}",
                    preview = getString(R.string.chat_list_preview_encrypted),
                    timestampText = "",
                    unreadCount = 0,
                    typeLabel = typeLabel,
                    isMuted = false,
                    isPinned = false,
                    isEncrypted = true,
                )
            registerPendingConversation(item)
            viewModel.addItem(item)
            switchToFolder(type)
            openConversation(item)
            return
        }

        Toast.makeText(requireContext(), R.string.chat_create_status_creating, Toast.LENGTH_SHORT).show()
        backgroundExecutor.execute {
            val creation =
                if (isChannel) {
                    val adminInput = adminUsernames.ifEmpty { listOf(session.username).filter { it.isNotBlank() } }
                    val adminIdsResult = resolveUserIds(adminInput)
                    if (adminIdsResult is VoxResult.Failure) {
                        postCreationError(adminIdsResult.error.message)
                        return@execute
                    }
                    val adminResolved = (adminIdsResult as VoxResult.Success).value
                    val adminIds =
                        (listOf(session.userId) + adminResolved.ids)
                            .filter { it.isNotBlank() }
                            .distinct()
                    createChannelUseCase(
                        serverBaseUrl = session.serverBaseUrl,
                        accessToken = session.accessToken,
                        adminUserIds = adminIds,
                        subscriberUserIds = emptyList(),
                    )
                } else {
                    val memberIdsResult = resolveUserIds(memberUsernames)
                    if (memberIdsResult is VoxResult.Failure) {
                        postCreationError(memberIdsResult.error.message)
                        return@execute
                    }

                    val memberResolved = (memberIdsResult as VoxResult.Success).value
                    val members =
                        (listOf(session.userId) + memberResolved.ids)
                            .filter { it.isNotBlank() }
                            .distinct()
                    if (members.isEmpty()) {
                        postCreationError(getString(R.string.chat_create_error_members_required))
                        return@execute
                    }
                    createGroupUseCase(
                        serverBaseUrl = session.serverBaseUrl,
                        accessToken = session.accessToken,
                        memberUserIds = members,
                    )
                }

            mainHandler.post {
                if (_binding == null) {
                    return@post
                }
                when (creation) {
                    is VoxResult.Success -> {
                        val createdType = if (isChannel) TYPE_CHANNEL else TYPE_GROUP
                        val typeLabel =
                            if (isChannel) {
                                getString(R.string.chat_list_type_channel)
                            } else {
                                getString(R.string.chat_list_type_group)
                            }
                        val item =
                            ChatListItemUi(
                                conversationId = creation.value,
                                conversationType = createdType,
                                title = "$typeLabel ${shortId(creation.value)}",
                                preview = getString(R.string.chat_list_preview_encrypted),
                                timestampText = "",
                                unreadCount = 0,
                                typeLabel = typeLabel,
                                isMuted = false,
                                isPinned = false,
                                isEncrypted = true,
                            )
                        registerPendingConversation(item)
                        viewModel.addItem(item)
                        switchToFolder(createdType)
                        mainHandler.postDelayed(::loadConversations, REFRESH_AFTER_CREATE_MS)
                        openConversation(item)
                    }

                    is VoxResult.Failure -> {
                        postCreationError(creation.error.message)
                    }
                }
            }
        }
    }

    private fun resolveUserIds(usernames: List<String>): VoxResult<ResolvedUserIds> {
        val resolvedIds = mutableListOf<String>()
        usernames.filter { it.isNotBlank() }.forEach { username ->
            val resolved =
                resolveUserIdByUsernameUseCase(
                    serverBaseUrl = session.serverBaseUrl,
                    accessToken = session.accessToken,
                    username = username,
            )
            when (resolved) {
                is VoxResult.Success -> {
                    resolvedIds += resolved.value
                }
                is VoxResult.Failure -> {
                    return VoxResult.Failure(
                        com.bialger.voxclient.core.common.error.VoxError.Validation(
                            getString(R.string.chat_create_error_resolve_user, username),
                        ),
                    )
                }
            }
        }
        return VoxResult.Success(
            ResolvedUserIds(
                ids = resolvedIds,
            ),
        )
    }

    private fun postCreationError(message: String) {
        mainHandler.post {
            if (_binding == null) {
                return@post
            }
            Toast.makeText(
                requireContext(),
                getString(R.string.chat_create_error_failed, message),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    companion object {
        private const val TAG = "ChatListFragment"
        private const val TYPE_DM = 0
        private const val TYPE_GROUP = 1
        private const val TYPE_CHANNEL = 2
        private const val FOLDER_PAGE_COUNT = 3
        private const val REFRESH_AFTER_CREATE_MS = 1500L
        private const val REFRESH_AFTER_SUBSCRIBE_SECOND_MS = 3500L
        private val CHANNEL_ID_REGEX = Regex("conv_[A-Za-z0-9_-]+")

        fun newInstance(session: UserSessionArgs): ChatListFragment =
            ChatListFragment().apply {
                arguments = session.toBundle()
            }
    }

    private data class ResolvedUserIds(
        val ids: List<String>,
    )
}
