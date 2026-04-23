package com.bialger.voxclient.data.mapper

import com.bialger.voxclient.core.common.error.VoxError
import com.bialger.voxclient.core.common.result.VoxResult
import com.bialger.voxclient.core.model.SduiAction
import com.bialger.voxclient.core.model.SduiButton
import com.bialger.voxclient.core.model.SduiMeta
import com.bialger.voxclient.core.model.SduiNode
import com.bialger.voxclient.core.model.SduiScreen
import com.bialger.voxclient.data.dto.SduiActionDto
import com.bialger.voxclient.data.dto.SduiButtonDto
import com.bialger.voxclient.data.dto.SduiComponentDto
import com.bialger.voxclient.data.dto.SduiMetaDto
import com.bialger.voxclient.data.dto.SduiScreenDto

object SduiMapper {
    fun toModel(dto: SduiScreenDto): VoxResult<SduiScreen> {
        val screenId = dto.screenId.trim()
        val title = dto.title.trim()
        if (screenId.isBlank()) return VoxResult.Failure(VoxError.Validation("SDUI screen_id is blank."))
        if (title.isBlank()) return VoxResult.Failure(VoxError.Validation("SDUI title is blank."))

        val mappedBody = ArrayList<SduiNode>(dto.body.size)
        for (component in dto.body) {
            when (val node = toNode(component)) {
                is VoxResult.Success -> mappedBody.add(node.value)
                is VoxResult.Failure -> return node
            }
        }

        return VoxResult.Success(
            SduiScreen(
                schemaVersion = dto.schemaVersion,
                screenId = screenId,
                title = title,
                body = mappedBody,
                meta = dto.meta?.toModel(),
            ),
        )
    }

    private fun SduiMetaDto.toModel(): SduiMeta =
        SduiMeta(
            eulaVersion = eulaVersion,
            minClientVersionCode = minClientVersionCode,
            latestClientVersionCode = latestClientVersionCode,
            updatePolicy = updatePolicy,
        )

    private fun toNode(dto: SduiComponentDto): VoxResult<SduiNode> {
        val type = dto.type.trim().lowercase()
        return when (type) {
            "text" -> {
                val value = dto.value?.trim().orEmpty()
                val style = dto.style?.trim().orEmpty()
                if (value.isBlank()) VoxResult.Failure(VoxError.Validation("SDUI text value is blank."))
                else VoxResult.Success(SduiNode.Text(value = value, style = style.ifBlank { "body" }))
            }
            "link" -> {
                val label = dto.label?.trim().orEmpty()
                val url = dto.url?.trim().orEmpty()
                val style = dto.style?.trim().orEmpty()
                if (label.isBlank()) VoxResult.Failure(VoxError.Validation("SDUI link label is blank."))
                else if (url.isBlank()) VoxResult.Failure(VoxError.Validation("SDUI link url is blank."))
                else VoxResult.Success(SduiNode.Link(label = label, style = style.ifBlank { "body" }, url = url))
            }
            "divider" -> VoxResult.Success(SduiNode.Divider)
            "button" -> {
                val value = dto.value?.trim().orEmpty()
                val style = dto.style?.trim().orEmpty()
                val action = dto.action?.toModel()
                if (value.isBlank()) VoxResult.Failure(VoxError.Validation("SDUI button value is blank."))
                else if (action == null) VoxResult.Failure(VoxError.Validation("SDUI button action is missing."))
                else VoxResult.Success(SduiNode.Button(value = value, style = style.ifBlank { "primary" }, action = action))
            }
            "button_row" -> {
                val buttons = dto.buttons.orEmpty()
                if (buttons.isEmpty()) return VoxResult.Failure(VoxError.Validation("SDUI button_row buttons are empty."))
                val mappedButtons = ArrayList<SduiButton>(buttons.size)
                for (b in buttons) {
                    when (val mb = b.toModel()) {
                        is VoxResult.Success -> mappedButtons.add(mb.value)
                        is VoxResult.Failure -> return mb
                    }
                }
                VoxResult.Success(SduiNode.ButtonRow(buttons = mappedButtons))
            }
            else -> VoxResult.Failure(VoxError.Validation("Unsupported SDUI component type: ${dto.type}"))
        }
    }

    private fun SduiActionDto.toModel(): SduiAction =
        SduiAction(
            type = type.trim(),
            url = url?.trim(),
            event = event?.trim(),
        )

    private fun SduiButtonDto.toModel(): VoxResult<SduiButton> {
        val value = value?.trim().orEmpty()
        val style = style?.trim().orEmpty()
        val action = action?.toModel()
        if (value.isBlank()) return VoxResult.Failure(VoxError.Validation("SDUI button_row button value is blank."))
        if (action == null) return VoxResult.Failure(VoxError.Validation("SDUI button_row button action is missing."))
        return VoxResult.Success(
            SduiButton(
                value = value,
                style = style.ifBlank { "secondary" },
                action = action,
            ),
        )
    }
}

