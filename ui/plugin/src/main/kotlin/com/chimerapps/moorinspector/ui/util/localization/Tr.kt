package com.chimerapps.moorinspector.ui.util.localization

enum class Tr(val key: String) {
    ActionConnect("moorinspector.action.connect"), //Connect
    ActionConnectDescription("moorinspector.action.connect.description"), //Connect to niddler server
    ActionDisconnect("moorinspector.action.disconnect"), //Disconnect
    ActionDisconnectDescription("moorinspector.action.disconnect.description"), //Disconnect from niddler server
    ActionNewSession("moorinspector.action.new.session"), //New session
    ActionNewSessionDescription("moorinspector.action.new.session.description"), //Start a new session
    StatusConnected("moorinspector.status.connected"), //Connected
    StatusConnectedTo("moorinspector.status.connected.to"), //to
    StatusDisconnected("moorinspector.status.disconnected"), //Disconnected
    ViewSession("moorinspector.view.session"), //Session
    ViewStartingAdb("moorinspector.view.starting.adb"); //Starting adb

    fun tr(vararg arguments: Any) : String {
        val raw = Localization.getString(key)
        if (arguments.isEmpty()) return raw
        return String.format(raw, *arguments)
    }
}