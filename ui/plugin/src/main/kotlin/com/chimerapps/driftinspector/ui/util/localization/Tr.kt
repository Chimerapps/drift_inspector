package com.chimerapps.driftinspector.ui.util.localization

enum class Tr(val key: String) {
    ActionConnect("driftinspector.action.connect"), //Connect
    ActionConnectDescription("driftinspector.action.connect.description"), //Connect to drift inspector server
    ActionDisconnect("driftinspector.action.disconnect"), //Disconnect
    ActionDisconnectDescription("driftinspector.action.disconnect.description"), //Disconnect from drift inspector server
    ActionExportCompleteBody("driftinspector.action.export.complete.body"), //<html>Export completed to <a href="file://%s">%s</a></html>
    ActionExportCompleteTitle("driftinspector.action.export.complete.title"), //Export completed
    ActionNewSession("driftinspector.action.new.session"), //New session
    ActionNewSessionDescription("driftinspector.action.new.session.description"), //Start a new session
    PreferencesBrowseAdbDescription("driftinspector.preferences.browse.adb.description"), //Path to adb
    PreferencesBrowseAdbTitle("driftinspector.preferences.browse.adb.title"), //Drift inspector - adb
    PreferencesBrowseIdeviceDescription("driftinspector.preferences.browse.idevice.description"), //Path to imobiledevice folders
    PreferencesBrowseIdeviceTitle("driftinspector.preferences.browse.idevice.title"), //Drift inspector - imobiledevice
    PreferencesButtonTestConfiguration("driftinspector.preferences.button.test.configuration"), //Test configuration
    PreferencesOptionPathToAdb("driftinspector.preferences.option.path.to.adb"), //Path to adb:
    PreferencesOptionPathToIdevice("driftinspector.preferences.option.path.to.idevice"), //Path to idevice binaries:
    PreferencesTestMessageAdbFoundAt("driftinspector.preferences.test.message.adb.found.at"), //ADB defined at path: %s
    PreferencesTestMessageAdbNotFound("driftinspector.preferences.test.message.adb.not.found"), //Path to ADB not found
    PreferencesTestMessageAdbOk("driftinspector.preferences.test.message.adb.ok"), //ADB path seems ok
    PreferencesTestMessageCheckingAdb("driftinspector.preferences.test.message.checking.adb"), //Checking adb command
    PreferencesTestMessageErrorAdbNotExecutable("driftinspector.preferences.test.message.error.adb.not.executable"), //ERROR - ADB file not executable
    PreferencesTestMessageErrorAdbNotFound("driftinspector.preferences.test.message.error.adb.not.found"), //ERROR - ADB file not found
    PreferencesTestMessageErrorCommunicationFailed("driftinspector.preferences.test.message.error.communication.failed"), //ERROR - Failed to communicate with adb
    PreferencesTestMessageErrorFileNotExecutable("driftinspector.preferences.test.message.error.file.not.executable"), //ERROR - %s file not executable
    PreferencesTestMessageErrorFileNotFound("driftinspector.preferences.test.message.error.file.not.found"), //ERROR - %s file not found
    PreferencesTestMessageErrorIdeviceNotDirectory("driftinspector.preferences.test.message.error.idevice.not.directory"), //ERROR - iMobileDevice path is not a directory
    PreferencesTestMessageErrorIdeviceNotFound("driftinspector.preferences.test.message.error.idevice.not.found"), //ERROR - iMobileDevice folder not found
    PreferencesTestMessageErrorPathIsDir("driftinspector.preferences.test.message.error.path.is.dir"), //ERROR - Specified path is a directory
    PreferencesTestMessageFileOk("driftinspector.preferences.test.message.file.ok"), //%s seems ok
    PreferencesTestMessageFoundDevicesCount("driftinspector.preferences.test.message.found.devices.count"), //ADB devices returns: %d device(s)
    PreferencesTestMessageIdevicePath("driftinspector.preferences.test.message.idevice.path"), //iMobileDevice folder defined at path: %s
    PreferencesTestMessageListingAdbDevices("driftinspector.preferences.test.message.listing.adb.devices"), //Listing devices
    PreferencesTestMessageStartingAdb("driftinspector.preferences.test.message.starting.adb"), //Starting adb server
    PreferencesTestMessageTestingAdbTitle("driftinspector.preferences.test.message.testing.adb.title"), //Testing ADB\n=======================================
    PreferencesTestMessageTestingIdeviceTitle("driftinspector.preferences.test.message.testing.idevice.title"), //\nTesting iDevice\n=======================================
    StatusConnected("driftinspector.status.connected"), //Connected
    StatusConnectedTo("driftinspector.status.connected.to"), //to
    StatusDisconnected("driftinspector.status.disconnected"), //Disconnected
    ViewSession("driftinspector.view.session"), //Session
    ViewStartingAdb("driftinspector.view.starting.adb"); //Starting adb

    fun tr(vararg arguments: Any) : String {
        val raw = Localization.getString(key)
        if (arguments.isEmpty()) return raw
        return String.format(raw, *arguments)
    }
}