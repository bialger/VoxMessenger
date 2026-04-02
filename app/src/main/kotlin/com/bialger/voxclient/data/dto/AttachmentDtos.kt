package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class UploadAttachmentInitRequestDto(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("file_size")
    val fileSize: Long,
    @SerializedName("mime_hint")
    val mimeHint: String? = null,
)

data class UploadAttachmentInitResponseDto(
    @SerializedName("attachment_id")
    val attachmentId: String,
    @SerializedName("blob_path")
    val blobPath: String,
)

data class FinalizeAttachmentRequestDto(
    @SerializedName("ciphertext_hash")
    val ciphertextHash: String,
)
