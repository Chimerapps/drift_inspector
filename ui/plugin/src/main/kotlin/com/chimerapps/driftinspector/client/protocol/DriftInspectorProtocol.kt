package com.chimerapps.driftinspector.client.protocol

import com.google.gsonpackaged.JsonObject
import org.java_websocket.client.WebSocketClient

interface DriftInspectorProtocol {

    /**
     * Called when a new websocket message has been received
     *
     * @param socket    The websocket client
     * @param message   The 'parsed' message contained in the websocket message
     */
    fun onMessage(socket: WebSocketClient, message: JsonObject)

}
