package com.bialger.voxclient.ui.conversation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ConversationViewModel : ViewModel() {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val _messages = MutableLiveData<List<ConversationMessageUi>>(emptyList())
    val messages: LiveData<List<ConversationMessageUi>> = _messages

    fun loadMockMessages() {
        _messages.value =
            listOf(
                ConversationMessageUi(
                    id = "msg_1",
                    body = "Session bootstrap completed for all your devices.",
                    timestampText = "09:10",
                    isOutgoing = false,
                    authorName = "alex",
                ),
                ConversationMessageUi(
                    id = "msg_2",
                    body = "Great, sync is now converged at cursor c_2394.",
                    timestampText = "09:11",
                    isOutgoing = true,
                    authorName = "you",
                ),
            )
    }

    fun replaceMessages(messages: List<ConversationMessageUi>) {
        _messages.value = messages
    }

    fun sendMessage(rawInput: String, authorName: String? = null): ConversationMessageUi? {
        val body = rawInput.trim()
        if (body.isEmpty()) {
            return null
        }
        return appendMessage(body = body, isOutgoing = true, authorName = authorName)
    }

    fun addAttachmentPlaceholder(placeholder: String, authorName: String? = null): ConversationMessageUi =
        appendMessage(body = placeholder, isOutgoing = true, authorName = authorName)

    private fun appendMessage(body: String, isOutgoing: Boolean, authorName: String? = null): ConversationMessageUi {
        val currentItems = _messages.value.orEmpty()
        val message =
            ConversationMessageUi(
                id = "msg_${currentItems.size + 1}",
                body = body,
                timestampText = LocalTime.now().format(timeFormatter),
                isOutgoing = isOutgoing,
                authorName = authorName,
            )
        _messages.value = currentItems + message
        return message
    }
}
