package com.chimerapps.moorinspector.ui.util.localization

enum class Tr(val key: String) {
    ActionConnect("moorinspector.action.connect"), //Connect
    ActionConnectDescription("moorinspector.action.connect.description"), //Connect to moor inspector server
    ActionDisconnect("moorinspector.action.disconnect"), //Disconnect
    ActionDisconnectDescription("moorinspector.action.disconnect.description"), //Disconnect from moor inspector server
    ActionNewSession("moorinspector.action.new.session"), //New session
    ActionNewSessionDescription("moorinspector.action.new.session.description"), //Start a new session
    PreferencesBrowseAdbDescription("moorinspector.preferences.browse.adb.description"), //Path to adb
    PreferencesBrowseAdbTitle("moorinspector.preferences.browse.adb.title"), //Moor inspector - adb
    PreferencesBrowseIdeviceDescription("moorinspector.preferences.browse.idevice.description"), //Path to imobiledevice folders
    PreferencesBrowseIdeviceTitle("moorinspector.preferences.browse.idevice.title"), //Moor inspector - imobiledevice
    PreferencesButtonTestConfiguration("moorinspector.preferences.button.test.configuration"), //Test configuration
    PreferencesOptionPathToAdb("moorinspector.preferences.option.path.to.adb"), //Path to adb:
    PreferencesOptionPathToIdevice("moorinspector.preferences.option.path.to.idevice"), //Path to idevice binaries:
    PreferencesTestMessageAdbFoundAt("moorinspector.preferences.test.message.adb.found.at"), //ADB defined at path: %s
    PreferencesTestMessageAdbNotFound("moorinspector.preferences.test.message.adb.not.found"), //Path to ADB not found
    PreferencesTestMessageAdbOk("moorinspector.preferences.test.message.adb.ok"), //ADB path seems ok
    PreferencesTestMessageCheckingAdb("moorinspector.preferences.test.message.checking.adb"), //Checking adb command
    PreferencesTestMessageErrorAdbNotExecutable("moorinspector.preferences.test.message.error.adb.not.executable"), //ERROR - ADB file not executable
    PreferencesTestMessageErrorAdbNotFound("moorinspector.preferences.test.message.error.adb.not.found"), //ERROR - ADB file not found
    PreferencesTestMessageErrorCommunicationFailed("moorinspector.preferences.test.message.error.communication.failed"), //ERROR - Failed to communicate with adb
    PreferencesTestMessageErrorFileNotExecutable("moorinspector.preferences.test.message.error.file.not.executable"), //ERROR - %s file not executable
    PreferencesTestMessageErrorFileNotFound("moorinspector.preferences.test.message.error.file.not.found"), //ERROR - %s file not found
    PreferencesTestMessageErrorIdeviceNotDirectory("moorinspector.preferences.test.message.error.idevice.not.directory"), //ERROR - iMobileDevice path is not a directory
    PreferencesTestMessageErrorIdeviceNotFound("moorinspector.preferences.test.message.error.idevice.not.found"), //ERROR - iMobileDevice folder not found
    PreferencesTestMessageErrorPathIsDir("moorinspector.preferences.test.message.error.path.is.dir"), //ERROR - Specified path is a directory
    PreferencesTestMessageFileOk("moorinspector.preferences.test.message.file.ok"), //%s seems ok
    PreferencesTestMessageFoundDevicesCount("moorinspector.preferences.test.message.found.devices.count"), //ADB devices returns: %d device(s)
    PreferencesTestMessageIdevicePath("moorinspector.preferences.test.message.idevice.path"), //iMobileDevice folder defined at path: %s
    PreferencesTestMessageListingAdbDevices("moorinspector.preferences.test.message.listing.adb.devices"), //Listing devices
    PreferencesTestMessageStartingAdb("moorinspector.preferences.test.message.starting.adb"), //Starting adb server
    PreferencesTestMessageTestingAdbTitle("moorinspector.preferences.test.message.testing.adb.title"), //Testing ADB\n=======================================
    PreferencesTestMessageTestingIdeviceTitle("moorinspector.preferences.test.message.testing.idevice.title"), //\nTesting iDevice\n=======================================
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