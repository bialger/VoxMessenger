package com.bialger.voxclient.core.model

data class SduiScreen(
    val schemaVersion: Int,
    val screenId: String,
    val title: String,
    val body: List<SduiNode>,
    val meta: SduiMeta?,
)

sealed interface SduiNode {
    data class Text(
        val value: String,
        val style: String,
    ) : SduiNode

    data class Link(
        val label: String,
        val style: String,
        val url: String,
    ) : SduiNode

    data object Divider : SduiNode

    data class Button(
        val value: String,
        val style: String,
        val action: SduiAction,
    ) : SduiNode

    data class ButtonRow(
        val buttons: List<SduiButton>,
    ) : SduiNode
}

data class SduiButton(
    val value: String,
    val style: String,
    val action: SduiAction,
)

data class SduiAction(
    val type: String,
    val url: String? = null,
    val event: String? = null,
)

data class SduiMeta(
    val eulaVersion: String?,
    val minClientVersionCode: Int?,
    val latestClientVersionCode: Int?,
    val updatePolicy: String?,
)
