package com.chimerapps.driftinspector.client.protocol

import com.chimerapps.driftinspector.client.DriftInspectorMessageListener
import com.google.gsonpackaged.Gson
import com.google.gsonpackaged.JsonElement
import com.google.gsonpackaged.JsonObject
import com.google.gsonpackaged.reflect.TypeToken
import org.java_websocket.client.WebSocketClient

data class DriftInspectorServerInfo(
    val bundleId: String,
    val icon: String?,
    val protocolVersion: Int,
    val databases: List<DriftInspectorDatabase>
)

data class DriftInspectorDatabase(
    val name: String,
    val id: String,
    val structure: DriftInspectorDatabaseStructure,
)

data class DriftInspectorDatabaseStructure(
    val version: Int,
    val tables: List<DriftInspectorTable>
)

data class DriftInspectorTable(
    val sqlName: String,
    val withoutRowId: Boolean,
    val primaryKey: List<String>?,
    val columns: List<DriftInspectorColumn>
)

data class DriftInspectorColumn(
    val name: String,
    val isRequired: Boolean,
    val type: String,
    val nullable: Boolean,
    val isBoolean: Boolean = false,
    val autoIncrement: Boolean = false,
    val minTextLength: Int? = null,
    val maxTextLength: Int? = null
)

data class ExportResponse(
    val databaseId: String,
    val requestId: String,
    val schemas: List<String>,
    val data: List<ExportData>
)

data class ExportData(
    val name: String,
    val data: List<Map<String, Any?>>
)

class DriftInspectorV1ProtocolHandler(private val messageListener: DriftInspectorMessageListener) :
    DriftInspectorProtocol {

    companion object {
        const val MESSAGE_TYPE_FILTER_RESULT = "filterResult"
        const val MESSAGE_TYPE_SERVER_INFO = "serverInfo"
        const val MESSAGE_TYPE_UPDATE_RESULT = "updateResult"
        const val MESSAGE_TYPE_ERROR = "error"
        const val MESSAGE_TYPE_BULK_RESPONSE = "bulkResponse"
        const val MESSAGE_TYPE_EXPORT_RESPONSE = "exportResult"
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
            MESSAGE_TYPE_EXPORT_RESPONSE -> onExportResult(message.get("body"))
        }
    }

    private fun onExportResult(body: JsonElement) {
        print(body)
        val exportResponse = gson.fromJson(body, ExportResponse::class.java)
        messageListener.onExportResult(exportResponse.databaseId, exportResponse.requestId, exportResponse)
    }

    private fun onFilterResult(body: JsonElement) {
        val bodyObject = body.asJsonObject

        val tableId = bodyObject.get("databaseId").asString
        val requestId = bodyObject.get("requestId").asString

        val data = bodyObject.get("data")
        val columns = bodyObject.get("columns")
        if (data == null || data.isJsonNull) {
            messageListener.onFilterData(tableId, requestId, emptyList(), emptyList())
        } else {
            val stringColumns = columns.asJsonArray.map { it.asString }
            val rows = data.asJsonArray.map {
                gson.fromJson<Map<String, Any?>>(it, object : TypeToken<Map<String, Any?>>() {}.type)
            }
            messageListener.onFilterData(tableId, requestId, rows, stringColumns)
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
        val info = gson.fromJson(body, DriftInspectorServerInfo::class.java)
        messageListener.onServerInfo(info)
    }

    private fun onError(body: JsonElement) {
        val bodyObject = body.asJsonObject

        val requestId = bodyObject.get("requestId").asString
        val message = bodyObject.get("message").asString

        messageListener.onError(requestId, message)
    }

}