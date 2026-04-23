package com.bialger.voxclient.data.serialization.gson

import com.bialger.voxclient.core.model.SduiAction
import com.bialger.voxclient.core.model.SduiButton
import com.bialger.voxclient.core.model.SduiNode
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import java.util.Locale

/**
 * Polymorphic deserializer for SDUI nodes.
 *
 * This exists to prevent Gson from attempting to instantiate an abstract/sealed base type.
 * It is only used when a response/model is parsed directly into `SduiNode`.
 */
class SduiNodeJsonAdapter : JsonDeserializer<SduiNode> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SduiNode {
        val obj =
            json.asJsonObject
                ?: throw JsonParseException("SDUI node must be a JSON object.")

        val type = obj["type"]?.asString?.trim().orEmpty().lowercase(Locale.ROOT)
        return when (type) {
            "text" -> {
                val value = obj["value"]?.asString?.trim().orEmpty()
                val style = obj["style"]?.asString?.trim().orEmpty().ifBlank { "body" }
                if (value.isBlank()) throw JsonParseException("SDUI text value is blank.")
                SduiNode.Text(value = value, style = style)
            }
            "link" -> {
                val label = obj["label"]?.asString?.trim().orEmpty()
                val url = obj["url"]?.asString?.trim().orEmpty()
                val style = obj["style"]?.asString?.trim().orEmpty().ifBlank { "body" }
                if (label.isBlank()) throw JsonParseException("SDUI link label is blank.")
                if (url.isBlank()) throw JsonParseException("SDUI link url is blank.")
                SduiNode.Link(label = label, style = style, url = url)
            }
            "divider" -> SduiNode.Divider
            "button" -> {
                val value = obj["value"]?.asString?.trim().orEmpty()
                val style = obj["style"]?.asString?.trim().orEmpty().ifBlank { "primary" }
                val actionObj = obj["action"]?.asJsonObject
                    ?: throw JsonParseException("SDUI button action is missing.")
                val actionType = actionObj["type"]?.asString?.trim().orEmpty()
                if (value.isBlank()) throw JsonParseException("SDUI button value is blank.")
                val action =
                    SduiAction(
                        type = actionType,
                        url = actionObj["url"]?.asString?.trim(),
                        event = actionObj["event"]?.asString?.trim(),
                    )
                SduiNode.Button(value = value, style = style, action = action)
            }
            "button_row" -> {
                val buttonsArr = obj["buttons"]?.asJsonArray
                    ?: throw JsonParseException("SDUI button_row buttons are missing.")
                if (buttonsArr.size() == 0) throw JsonParseException("SDUI button_row buttons are empty.")
                val buttons =
                    buttonsArr.mapIndexed { index, el ->
                        val bObj = el.asJsonObject
                            ?: throw JsonParseException("SDUI button_row button[$index] must be an object.")
                        val value = bObj["value"]?.asString?.trim().orEmpty()
                        val style = bObj["style"]?.asString?.trim().orEmpty().ifBlank { "secondary" }
                        val actionObj = bObj["action"]?.asJsonObject
                            ?: throw JsonParseException("SDUI button_row button[$index] action is missing.")
                        val actionType = actionObj["type"]?.asString?.trim().orEmpty()
                        if (value.isBlank()) throw JsonParseException("SDUI button_row button[$index] value is blank.")
                        val action =
                            SduiAction(
                                type = actionType,
                                url = actionObj["url"]?.asString?.trim(),
                                event = actionObj["event"]?.asString?.trim(),
                            )
                        SduiButton(value = value, style = style, action = action)
                    }
                SduiNode.ButtonRow(buttons = buttons)
            }
            else -> throw JsonParseException("Unsupported SDUI node type: $type")
        }
    }
}

