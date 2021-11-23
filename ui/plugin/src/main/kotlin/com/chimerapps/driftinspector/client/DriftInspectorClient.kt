package com.chimerapps.driftinspector.client

import com.chimerapps.discovery.utils.debug
import com.chimerapps.discovery.utils.logger
import com.chimerapps.driftinspector.client.protocol.ExportResponse
import com.chimerapps.driftinspector.client.protocol.DriftInspectorProtocol
import com.chimerapps.driftinspector.client.protocol.DriftInspectorServerInfo
import com.chimerapps.driftinspector.client.protocol.DriftInspectorV1ProtocolHandler
import com.chimerapps.driftinspector.ui.view.BulkActionData
import com.chimerapps.driftinspector.ui.view.DriftInspectorVariable
import com.google.gsonpackaged.Gson
import com.google.gsonpackaged.JsonArray
import com.google.gsonpackaged.JsonObject
import com.google.gsonpackaged.JsonParser
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import java.io.Closeable
import java.net.URI
import java.util.*

class DriftInspectorClient(serverURI: URI) : DriftInspectorMessageListener, Closeable {

    companion object {
        const val MESSAGE_TYPE_PROTOCOL = "protocol"
    }

    internal val clientListeners: MutableSet<DriftInspectorMessageListener> = HashSet()
    private val socketClient = WebSocketDriftInspectorClient(serverURI, this)
    internal var protocolHandler: DriftInspectorProtocol? = null

    fun registerMessageListener(listener: DriftInspectorMessageListener) {
        synchronized(clientListeners) {
            clientListeners.add(listener)
        }
    }

    fun unregisterMessageListener(listener: DriftInspectorMessageListener) {
        synchronized(clientListeners) {
            clientListeners.remove(listener)
        }
    }

    internal fun registerProtocolHandler(protocolVersion: Int) {
        protocolHandler = when {
            else -> DriftInspectorV1ProtocolHandler(this)
        }
        onReady()
    }

    fun connect() {
        socketClient.connect()
    }

    override fun close() {
        socketClient.close()
    }

    override fun onFilterData(tableId: String,
                              requestId: String,
                              rows: List<Map<String, Any?>>,
                              columns: List<String>) {
        clientListeners.forEach { it.onFilterData(tableId, requestId, rows, columns) }
    }

    override fun onClosed() {
        clientListeners.forEach { it.onClosed() }
    }

    override fun onServerInfo(serverInfo: DriftInspectorServerInfo) {
        clientListeners.forEach { it.onServerInfo(serverInfo) }
    }

    override fun onReady() {
        clientListeners.forEach { it.onReady() }
    }

    override fun onUpdateResult(tableId: String, requestId: String, numRowsUpdated: Int) {
        clientListeners.forEach { it.onUpdateResult(tableId, requestId, numRowsUpdated) }
    }

    override fun onError(requestId: String, message: String) {
        clientListeners.forEach { it.onError(requestId, message) }
    }

    override fun onBulkUpdateResult(requestId: String) {
        clientListeners.forEach { it.onBulkUpdateResult(requestId) }
    }

    override fun onExportResult(databaseId: String, requestId: String, exportResponse: ExportResponse) {
        clientListeners.forEach { it.onExportResult(databaseId, requestId, exportResponse) }
    }

    fun query(requestId: String, databaseId: String, query: String) {
        socketClient.sendString("{\"type\":\"filter\", \"body\": {\"databaseId\":\"$databaseId\",\"requestId\":\"$requestId\",\"query\":\"$query\"}}")
    }

    fun update(
        requestId: String,
        databaseId: String,
        query: String,
        affectedTables: List<String>,
        variables: List<DriftInspectorVariable>
    ) {
        val gson = Gson()
        val json = JsonObject()
        json.addProperty("type", "update")
        json.add("body", JsonObject().also { body ->
            body.addProperty("databaseId", databaseId)
            body.addProperty("requestId", requestId)
            body.addProperty("query", query)
            body.add("affectedTables", JsonArray().also { array ->
                affectedTables.forEach { array.add(it) }
            })
            body.add("variables", gson.toJsonTree(variables))
        })
        socketClient.sendString(json.toString())
    }

    fun bulkUpdate(requestId: String, databaseId: String, data: List<BulkActionData>) {
        val gson = Gson()
        val json = JsonObject()
        json.addProperty("type", "batch")
        json.add("body", JsonObject().also { body ->
            body.addProperty("requestId", requestId)
            body.add("actions", JsonArray().also { array ->
                data.forEach { action ->
                    array.add(JsonObject().also { actionWrapper ->
                        actionWrapper.addProperty("type", "update")
                        actionWrapper.add("body", JsonObject().also { actionBody ->
                            actionBody.addProperty("databaseId", databaseId)
                            actionBody.addProperty("query", action.query)
                            actionBody.add("affectedTables", JsonArray().also { array ->
                                action.affectedTables.forEach { array.add(it) }
                            })
                            actionBody.add("variables", gson.toJsonTree(action.variables))
                        })
                    })
                }
            })
        })
        socketClient.sendString(json.toString())
    }

    fun export(requestId: String, databaseId: String, tableNames: List<String>) {
        val json = JsonObject()
        json.addProperty("type", "export")
        json.add("body", JsonObject().also { body ->
            body.addProperty("databaseId", databaseId)
            body.addProperty("requestId", requestId)
            body.add("tables", JsonArray().also { array ->
                tableNames.forEach { tableName -> array.add(tableName) }
            })
        })
        socketClient.sendString(json.toString())
    }
}

interface DriftInspectorMessageListener {
    /**
     * Called when the connection with the server has been closed
     */
    fun onClosed() {}

    fun onFilterData(tableId: String, requestId: String, rows: List<Map<String, Any?>>, columns: List<String>) {}

    fun onUpdateResult(tableId: String, requestId: String, numRowsUpdated: Int) {}

    fun onBulkUpdateResult(requestId: String) {}

    fun onServerInfo(serverInfo: DriftInspectorServerInfo) {}

    fun onError(requestId: String, message: String) {}

    fun onReady() {}

    fun onExportResult(databaseId: String, requestId: String, exportResponse: ExportResponse) {}
}

private class WebSocketDriftInspectorClient(serverURI: URI, private val parent: DriftInspectorClient) :
    WebSocketClient(serverURI, Draft_6455()) {

    companion object {
        private val log = logger<WebSocketDriftInspectorClient>()
    }

    override fun onOpen(handshakeData: ServerHandshake?) {
        log.debug("Connection succeeded: ${connection.remoteSocketAddress}")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        log.debug("Connection closed: $reason")
        synchronized(parent.clientListeners) {
            parent.clientListeners.forEach { it.onClosed() }
        }
    }

    override fun onMessage(message: String) {
        log.debug("Got message: $message")
        val json = JsonParser.parseString(message).asJsonObject
        val messageType = json.get("type").asString

        if (messageType == DriftInspectorClient.MESSAGE_TYPE_PROTOCOL) {
            parent.registerProtocolHandler(json.get("protocolVersion").asInt)
            return
        }
        parent.protocolHandler?.onMessage(this, json)
    }

    override fun onError(ex: Exception?) {
        log.debug(ex.toString())
    }

    fun sendString(data: String) {
        send(data)
    }
}
