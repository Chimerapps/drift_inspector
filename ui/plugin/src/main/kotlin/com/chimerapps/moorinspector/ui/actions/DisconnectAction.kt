package com.chimerapps.moorinspector.ui.actions

import com.chimerapps.moorinspector.ui.ConnectionMode
import com.chimerapps.moorinspector.ui.InspectorSessionWindow
import com.chimerapps.moorinspector.ui.util.localization.Tr
import com.intellij.icons.AllIcons

class DisconnectAction(private val window: InspectorSessionWindow, listener: () -> Unit) : DisableableAction(
    Tr.ActionDisconnect.tr(),
    Tr.ActionDisconnectDescription.tr(),
    AllIcons.Actions.Suspend,
    listener
) {

    override val isEnabled: Boolean
        get() = window.connectionMode == ConnectionMode.MODE_CONNECTED

}