package com.chimerapps.moorinspector.ui.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "MoorInspectorSettings", storages = [Storage("moorinspector.xml")])
class MoorInspectorSettings : PersistentStateComponent<MoorInspectorSettings> {

    companion object {
        val instance: MoorInspectorSettings
            get() = ServiceManager.getService(MoorInspectorSettings::class.java)
    }

    var adbPath: String? = null
    var iDeviceBinariesPath: String? = null

    override fun getState(): MoorInspectorSettings? = this

    override fun loadState(state: MoorInspectorSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

}