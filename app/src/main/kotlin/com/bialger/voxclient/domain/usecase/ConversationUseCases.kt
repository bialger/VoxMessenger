package com.bialger.voxclient.domain.usecase

import com.bialger.voxclient.core.model.ChatMessage
import com.bialger.voxclient.core.model.ConversationSummary
import com.bialger.voxclient.core.model.ConversationType
import com.bialger.voxclient.core.model.SendState
import com.bialger.voxclient.core.model.preview
import com.bialger.voxclient.core.model.titleFor
import com.bialger.voxclient.core.model.totalUnread

class ResolveConversationTitleUseCase {
    operator fun invoke(type: ConversationType): String = titleFor(type)
}

class BuildMessagePreviewUseCase {
    operator fun invoke(message: ChatMessage, max: Int = 40): String = message.preview(max)
}

class AggregateUnreadCountUseCase {
    operator fun invoke(items: List<ConversationSummary>): Int = totalUnread(items)
}

class ResolveSendStateUseCase {
    operator fun invoke(isOnline: Boolean, hasEncryptedPayload: Boolean): SendState =
        when {
            !hasEncryptedPayload -> SendState.Encrypting
            !isOnline -> SendState.Failed("No network connection.")
            else -> SendState.Sending
        }
}

