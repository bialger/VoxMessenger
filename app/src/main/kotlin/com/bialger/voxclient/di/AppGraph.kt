package com.bialger.voxclient.di

import com.bialger.voxclient.data.datasource.remote.VoxPublicApiFactory
import com.bialger.voxclient.data.repository.cache.InMemoryCachingAuthSessionGateway
import com.bialger.voxclient.data.repository.cache.InMemoryCachingConversationGateway
import com.bialger.voxclient.data.repository.cache.InMemoryCachingUsernameGateway
import com.bialger.voxclient.data.repository.RetrofitServerHealthRepository
import com.bialger.voxclient.data.repository.VoxAuthRemoteRepository
import com.bialger.voxclient.data.repository.VoxConversationRemoteRepository
import com.bialger.voxclient.data.repository.VoxUsernameRemoteRepository
import com.bialger.voxclient.domain.repository.AuthSessionGateway
import com.bialger.voxclient.domain.repository.ConversationGateway
import com.bialger.voxclient.domain.repository.ServerHealthRepository
import com.bialger.voxclient.domain.repository.UsernameGateway
import com.bialger.voxclient.domain.usecase.AddConversationMemberUseCase
import com.bialger.voxclient.domain.usecase.CheckServerHealthUseCase
import com.bialger.voxclient.domain.usecase.CreateChannelUseCase
import com.bialger.voxclient.domain.usecase.CreateDmUseCase
import com.bialger.voxclient.domain.usecase.CreateGroupUseCase
import com.bialger.voxclient.domain.usecase.FetchUsernameByUserIdUseCase
import com.bialger.voxclient.domain.usecase.FetchUsernamesBatchByUserIdsUseCase
import com.bialger.voxclient.domain.usecase.LoadChatListUseCase
import com.bialger.voxclient.domain.usecase.LoadConversationDetailsUseCase
import com.bialger.voxclient.domain.usecase.LoadConversationHistoryUseCase
import com.bialger.voxclient.domain.usecase.LoadConversationMembersUseCase
import com.bialger.voxclient.domain.usecase.LoadCurrentUserUseCase
import com.bialger.voxclient.domain.usecase.LoginUseCase
import com.bialger.voxclient.domain.usecase.RegisterUserUseCase
import com.bialger.voxclient.domain.usecase.ResolveUserIdByUsernameUseCase
import com.bialger.voxclient.domain.usecase.ResolveUsernamesByUserIdsUseCase
import com.bialger.voxclient.domain.usecase.RegisterUsernamesBatchUseCase
import com.bialger.voxclient.domain.usecase.SendMessageUseCase
import com.bialger.voxclient.domain.usecase.SubscribeToChannelUseCase
import com.google.gson.Gson

object AppGraph {
    private val apiFactory: VoxPublicApiFactory by lazy { VoxPublicApiFactory() }
    private val gson: Gson by lazy { Gson() }

    private val authSessionRemoteGateway: AuthSessionGateway by lazy {
        VoxAuthRemoteRepository(apiFactory = apiFactory, gson = gson)
    }
    private val conversationRemoteGateway: ConversationGateway by lazy {
        VoxConversationRemoteRepository(apiFactory = apiFactory, gson = gson)
    }
    private val usernameRemoteGateway: UsernameGateway by lazy {
        VoxUsernameRemoteRepository(apiFactory = apiFactory, gson = gson)
    }
    private val serverHealthRemoteRepository: ServerHealthRepository by lazy {
        RetrofitServerHealthRepository(apiFactory = apiFactory)
    }
    private val authSessionGateway: AuthSessionGateway by lazy {
        InMemoryCachingAuthSessionGateway(authSessionRemoteGateway)
    }
    private val conversationGateway: ConversationGateway by lazy {
        InMemoryCachingConversationGateway(conversationRemoteGateway)
    }
    private val usernameGateway: UsernameGateway by lazy {
        InMemoryCachingUsernameGateway(usernameRemoteGateway)
    }
    private val serverHealthRepository: ServerHealthRepository by lazy {
        serverHealthRemoteRepository
    }

    val checkServerHealthUseCase: CheckServerHealthUseCase by lazy {
        CheckServerHealthUseCase(serverHealthRepository)
    }
    val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(authSessionGateway)
    }
    val registerUserUseCase: RegisterUserUseCase by lazy {
        RegisterUserUseCase(authSessionGateway)
    }
    val loadCurrentUserUseCase: LoadCurrentUserUseCase by lazy {
        LoadCurrentUserUseCase(authSessionGateway)
    }

    val loadChatListUseCase: LoadChatListUseCase by lazy {
        LoadChatListUseCase(conversationGateway)
    }
    val resolveUserIdByUsernameUseCase: ResolveUserIdByUsernameUseCase by lazy {
        ResolveUserIdByUsernameUseCase(conversationGateway)
    }
    val resolveUsernamesByUserIdsUseCase: ResolveUsernamesByUserIdsUseCase by lazy {
        ResolveUsernamesByUserIdsUseCase(conversationGateway)
    }
    val fetchUsernameByUserIdUseCase: FetchUsernameByUserIdUseCase by lazy {
        FetchUsernameByUserIdUseCase(usernameGateway)
    }
    val fetchUsernamesBatchByUserIdsUseCase: FetchUsernamesBatchByUserIdsUseCase by lazy {
        FetchUsernamesBatchByUserIdsUseCase(usernameGateway)
    }
    val registerUsernamesBatchUseCase: RegisterUsernamesBatchUseCase by lazy {
        RegisterUsernamesBatchUseCase(usernameGateway)
    }
    val createDmUseCase: CreateDmUseCase by lazy {
        CreateDmUseCase(conversationGateway)
    }
    val createGroupUseCase: CreateGroupUseCase by lazy {
        CreateGroupUseCase(conversationGateway)
    }
    val createChannelUseCase: CreateChannelUseCase by lazy {
        CreateChannelUseCase(conversationGateway)
    }
    val subscribeToChannelUseCase: SubscribeToChannelUseCase by lazy {
        SubscribeToChannelUseCase(conversationGateway)
    }
    val loadConversationDetailsUseCase: LoadConversationDetailsUseCase by lazy {
        LoadConversationDetailsUseCase(conversationGateway)
    }
    val loadConversationMembersUseCase: LoadConversationMembersUseCase by lazy {
        LoadConversationMembersUseCase(conversationGateway)
    }
    val addConversationMemberUseCase: AddConversationMemberUseCase by lazy {
        AddConversationMemberUseCase(conversationGateway)
    }
    val loadConversationHistoryUseCase: LoadConversationHistoryUseCase by lazy {
        LoadConversationHistoryUseCase(conversationGateway)
    }
    val sendMessageUseCase: SendMessageUseCase by lazy {
        SendMessageUseCase(conversationGateway)
    }
}
