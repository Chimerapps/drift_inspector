package com.chimerapps.moorinspector.ui.actions

import com.chimerapps.moorinspector.ui.InspectorToolWindow
import com.chimerapps.moorinspector.ui.util.localization.Tr
import com.intellij.icons.AllIcons

class NewSessionAction(private val window: InspectorToolWindow, actionListener: () -> Unit)
    : DisableableAction(text = Tr.ActionNewSession.tr(), description = Tr.ActionNewSessionDescription.tr(),
        icon = AllIcons.General.Add, actionListener = actionListener) {

    override val isEnabled: Boolean
        get() = window.isReady

}