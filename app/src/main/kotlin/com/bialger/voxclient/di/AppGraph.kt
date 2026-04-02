package com.bialger.voxclient.di

import com.bialger.voxclient.data.datasource.remote.VoxPublicApiFactory
import com.bialger.voxclient.data.repository.RetrofitServerHealthRepository
import com.bialger.voxclient.data.repository.VoxAuthRemoteRepository
import com.bialger.voxclient.data.repository.VoxConversationRemoteRepository
import com.bialger.voxclient.domain.repository.AuthSessionGateway
import com.bialger.voxclient.domain.repository.ConversationGateway
import com.bialger.voxclient.domain.repository.ServerHealthRepository
import com.bialger.voxclient.domain.usecase.AddConversationMemberUseCase
import com.bialger.voxclient.domain.usecase.CheckServerHealthUseCase
import com.bialger.voxclient.domain.usecase.CreateChannelUseCase
import com.bialger.voxclient.domain.usecase.CreateDmUseCase
import com.bialger.voxclient.domain.usecase.CreateGroupUseCase
import com.bialger.voxclient.domain.usecase.LoadChatListUseCase
import com.bialger.voxclient.domain.usecase.LoadConversationDetailsUseCase
import com.bialger.voxclient.domain.usecase.LoadConversationHistoryUseCase
import com.bialger.voxclient.domain.usecase.LoadConversationMembersUseCase
import com.bialger.voxclient.domain.usecase.LoadCurrentUserUseCase
import com.bialger.voxclient.domain.usecase.LoginUseCase
import com.bialger.voxclient.domain.usecase.RegisterUserUseCase
import com.bialger.voxclient.domain.usecase.ResolveUserIdByUsernameUseCase
import com.bialger.voxclient.domain.usecase.ResolveUsernamesByUserIdsUseCase
import com.bialger.voxclient.domain.usecase.SendMessageUseCase
import com.bialger.voxclient.domain.usecase.SubscribeToChannelUseCase
import com.google.gson.Gson

object AppGraph {
    private val apiFactory: VoxPublicApiFactory by lazy { VoxPublicApiFactory() }
    private val gson: Gson by lazy { Gson() }

    private val authSessionGateway: AuthSessionGateway by lazy {
        VoxAuthRemoteRepository(apiFactory = apiFactory, gson = gson)
    }
    private val conversationGateway: ConversationGateway by lazy {
        VoxConversationRemoteRepository(apiFactory = apiFactory, gson = gson)
    }
    private val serverHealthRepository: ServerHealthRepository by lazy {
        RetrofitServerHealthRepository(apiFactory = apiFactory)
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
