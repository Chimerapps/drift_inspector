package com.chimerapps.moorinspector.client.protocol

import com.chimerapps.moorinspector.client.MoorInspectorMessageListener
import com.google.gsonpackaged.Gson
import com.google.gsonpackaged.JsonElement
import com.google.gsonpackaged.JsonObject
import com.google.gsonpackaged.reflect.TypeToken
import org.java_websocket.client.WebSocketClient

data class MoorInspectorServerInfo(
    val bundleId: String,
    val icon: String?,
    val protocolVersion: Int,
    val databases: List<MoorInspectorDatabase>
)

data class MoorInspectorDatabase(
    val name: String,
    val id: String,
    val structure: MoorInspectorDatabaseStructure,
)

data class MoorInspectorDatabaseStructure(
    val version: Int,
    val tables: List<MoorInspectorTable>
)

data class MoorInspectorTable(
    val sqlName: String,
    val withoutRowId: Boolean,
    val primaryKey: List<String>?,
    val columns: List<MoorInspectorColumn>
)

data class MoorInspectorColumn(
    val name: String,
    val isRequired: Boolean,
    val type: String,
    val nullable: Boolean
)

class MoorInspectorV1ProtocolHandler(private val messageListener: MoorInspectorMessageListener) :
    MoorInspectorProtocol {

    companion object {
        const val MESSAGE_TYPE_FILTER_RESULT = "filterResult"
        const val MESSAGE_TYPE_SERVER_INFO = "serverInfo"
        const val MESSAGE_TYPE_UPDATE_RESULT = "updateResult"
        const val MESSAGE_TYPE_ERROR = "error"
        const val MESSAGE_TYPE_BULK_RESPONSE = "bulkResponse"
    }

    private val gson = Gson()

    override fun onMessage(socket: WebSocketClient, message: JsonObject) {
        val type = message.get("type").asString

        when (type) {
            MESSAGE_TYPE_FILTER_RESULT -> onFilterResult(message.get("body"))
            MESSAGE_TYPE_SERVER_INFO -> onServerInfo(message.get("body"))
            MESSAGE_TYPE_UPDATE_RESULT -> onUpdateResult(message.get("body"))
            MESSAGE_TYPE_ERROR -> onError(message.get("body"))
            MESSAGE_TYPE_BULK_RESPONSE -> onBulkResult(message.get("body"))
        }
    }

    private fun onFilterResult(body: JsonElement) {
        val bodyObject = body.asJsonObject

        val tableId = bodyObject.get("databaseId").asString
        val requestId = bodyObject.get("requestId").asString

        val data = bodyObject.get("data")
        if (data == null || data.isJsonNull) {
            messageListener.onFilterData(tableId, requestId, emptyList())
        } else {
            val rows = data.asJsonArray.map {
                gson.fromJson<Map<String, Any?>>(it, object : TypeToken<Map<String, Any?>>() {}.type)
            }
            messageListener.onFilterData(tableId, requestId, rows)
        }
    }

    private fun onUpdateResult(body: JsonElement) {
        val bodyObject = body.asJsonObject

        val tableId = bodyObject.get("databaseId").asString
        val requestId = bodyObject.get("requestId").asString

        val numUpdated = bodyObject.get("numUpdated").asInt
        messageListener.onUpdateResult(tableId, requestId, numUpdated)
    }

    private fun onBulkResult(body: JsonElement) {
        val bodyObject = body.asJsonObject

        val requestId = bodyObject.get("requestId").asString

        messageListener.onBulkUpdateResult(requestId)
    }

    private fun onServerInfo(body: JsonElement) {
        val info = gson.fromJson(body, MoorInspectorServerInfo::class.java)
        messageListener.onServerInfo(info)
    }

    private fun onError(body: JsonElement) {
        val bodyObject = body.asJsonObject

        val requestId = bodyObject.get("requestId").asString
        val message = bodyObject.get("message").asString

        messageListener.onError(requestId, message)
    }

}