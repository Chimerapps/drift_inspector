package com.chimerapps.driftinspector.ui.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class ApplicationConfigurationProvider : Configurable {

    private var settingsForm: SettingsFormWrapper? = null

    override fun isModified(): Boolean = settingsForm?.isModified ?: false

    override fun getDisplayName(): String = "Drift Inspector"

    override fun apply() {
        settingsForm?.save()
    }

    override fun reset() {
        settingsForm?.reset()
    }

    override fun disposeUIResources() {
        super.disposeUIResources()
        settingsForm = null
    }

    override fun createComponent(): JComponent? {
        val form = settingsForm ?: SettingsFormWrapper(DriftInspectorSettings.instance).also {
            it.initUI()
        }
        settingsForm = form
        return form.component
    }

}