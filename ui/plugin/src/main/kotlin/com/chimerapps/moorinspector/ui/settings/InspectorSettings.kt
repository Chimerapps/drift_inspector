package com.chimerapps.moorinspector.ui.settings

import com.google.gsonpackaged.Gson
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.OptionTag

@State(name = "MoorInspectorSettings", storages = [Storage("moorinspector.xml")])
class MoorInspectorSettings : PersistentStateComponent<MoorInspectorSettings> {

    companion object {
        val instance: MoorInspectorSettings
            get() = ServiceManager.getService(MoorInspectorSettings::class.java)
    }

    var adbPath: String? = null
    var iDeviceBinariesPath: String? = null

    override fun getState(): MoorInspectorSettings = this

    override fun loadState(state: MoorInspectorSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

}

@State(name = "MoorInspectorState", storages = [Storage("moorinspector.xml")], reloadable = true)
class MoorProjectSettings : PersistentStateComponent<MoorInspectorState> {

    companion object {
        fun instance(project: Project): MoorProjectSettings {
            return ServiceManager.getService(project, MoorProjectSettings::class.java)
        }
    }

    private var state: MoorInspectorState = MoorInspectorState()

    fun updateState(oldStateModifier: MoorInspectorState.() -> MoorInspectorState) {
        state = state.oldStateModifier()
    }

    override fun getState(): MoorInspectorState = state

    override fun loadState(state: MoorInspectorState) {
        this.state = MoorInspectorState()
        XmlSerializerUtil.copyBean(state, this.state)
    }

}

data class MoorInspectorState(
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