package com.chimerapps.moorinspector.ui.actions

import com.chimerapps.moorinspector.ui.ConnectionMode
import com.chimerapps.moorinspector.ui.InspectorSessionWindow
import com.chimerapps.moorinspector.ui.util.localization.Tr
import com.intellij.icons.AllIcons

class ConnectAction(private val window: InspectorSessionWindow, listener: () -> Unit) :
    DisableableAction(Tr.ActionConnect.tr(), Tr.ActionConnectDescription.tr(), AllIcons.Actions.Execute, listener) {

    override val isEnabled: Boolean
        get() = window.connectionMode == ConnectionMode.MODE_DISCONNECTED

}