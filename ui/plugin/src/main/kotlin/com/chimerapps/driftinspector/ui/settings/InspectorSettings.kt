package com.chimerapps.driftinspector.ui.settings

import com.google.gsonpackaged.Gson
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.OptionTag

@State(name = "DriftInspectorSettings", storages = [Storage("driftinspector.xml")])
class DriftInspectorSettings : PersistentStateComponent<DriftInspectorSettingsData> {

    companion object {
        val instance: DriftInspectorSettings
            get() = ServiceManager.getService(DriftInspectorSettings::class.java)
    }

    private var settings: DriftInspectorSettingsData = DriftInspectorSettingsData()

    override fun getState(): DriftInspectorSettingsData = settings

    override fun loadState(state: DriftInspectorSettingsData) {
        settings = state
    }

}

data class DriftInspectorSettingsData(
    var adbPath: String? = null,
    var iDeviceBinariesPath: String? = null
)

@State(name = "DriftInspectorState", storages = [Storage("driftinspector.xml")], reloadable = true)
class DriftProjectSettings : PersistentStateComponent<DriftInspectorState> {

    companion object {
        fun instance(project: Project): DriftProjectSettings {
            return ServiceManager.getService(project, DriftProjectSettings::class.java)
        }
    }

    private var state: DriftInspectorState = DriftInspectorState()

    fun updateState(oldStateModifier: DriftInspectorState.() -> DriftInspectorState) {
        state = state.oldStateModifier()
    }

    override fun getState(): DriftInspectorState = state

    override fun loadState(state: DriftInspectorState) {
        this.state = DriftInspectorState()
        XmlSerializerUtil.copyBean(state, this.state)
    }

}

data class DriftInspectorState(
    @OptionTag(converter = ColumnConfigurationConverter::class) val columnConfiguration: DatabaseConfiguration? = null
) {

    fun updateColumnConfiguration(oldStateModifier: DatabaseConfiguration.() -> DatabaseConfiguration): DatabaseConfiguration {
        return (columnConfiguration ?: DatabaseConfiguration(emptyList())).oldStateModifier()
    }

}

data class DatabaseConfiguration(
    val databases: List<SingleDatabaseConfiguration>
) {

    fun updateDatabase(
        databaseName: String,
        modifier: SingleDatabaseConfiguration.() -> SingleDatabaseConfiguration
    ): DatabaseConfiguration {
        val index = databases.indexOfFirst { it.databaseName == databaseName }

        val mutable = databases.toMutableList()
        if (index >= 0) {
            mutable[index] = mutable[index].modifier()
        } else {
            mutable += SingleDatabaseConfiguration(databaseName, emptyList()).modifier()
        }

        return DatabaseConfiguration(mutable)
    }

}

data class SingleDatabaseConfiguration(
    val databaseName: String,
    val configuration: List<TableConfiguration>
) {

    fun updateTable(
        tableName: String,
        modifier: TableConfiguration.() -> TableConfiguration
    ): SingleDatabaseConfiguration {
        val index = configuration.indexOfFirst { it.tableName == tableName }

        val mutable = configuration.toMutableList()
        if (index >= 0) {
            mutable[index] = mutable[index].modifier()
        } else {
            mutable += TableConfiguration(tableName, emptyList()).modifier()
        }

        return SingleDatabaseConfiguration(databaseName, mutable)
    }

}

data class TableConfiguration(
    val tableName: String,
    val columns: List<ColumnConfiguration>
) {

    fun updateColumn(name: String, modifier: ColumnConfiguration.() -> ColumnConfiguration): TableConfiguration {
        val index = columns.indexOfFirst { it.columnName == name }

        val mutable = columns.toMutableList()
        if (index >= 0) {
            mutable[index] = mutable[index].modifier()
        } else {
            mutable += ColumnConfiguration(name, visible = true, width = -1).modifier()
        }

        return TableConfiguration(tableName, mutable)
    }

}

data class ColumnConfiguration(
    val columnName: String,
    val visible: Boolean,
    val width: Int
)

class ColumnConfigurationConverter : Converter<DatabaseConfiguration>() {
    private val gson = Gson()

    override fun toString(value: DatabaseConfiguration): String? {
        return gson.toJson(value)
    }

    override fun fromString(value: String): DatabaseConfiguration? {
        return gson.fromJson(value, DatabaseConfiguration::class.java)
    }

}