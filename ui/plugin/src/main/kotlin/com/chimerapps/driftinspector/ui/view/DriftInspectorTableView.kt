package com.chimerapps.driftinspector.ui.view

import com.chimerapps.driftinspector.client.protocol.DriftInspectorColumn
import com.chimerapps.driftinspector.client.protocol.DriftInspectorDatabase
import com.chimerapps.driftinspector.client.protocol.DriftInspectorTable
import com.chimerapps.driftinspector.client.protocol.ExportResponse
import com.chimerapps.driftinspector.export.sql.SqlExportHandler
import com.chimerapps.driftinspector.ui.actions.RefreshAction
import com.chimerapps.driftinspector.ui.util.NotificationUtil
import com.chimerapps.driftinspector.ui.util.ensureMain
import com.chimerapps.driftinspector.ui.util.list.DiffUtilComparator
import com.chimerapps.driftinspector.ui.util.list.ListUpdateHelper
import com.chimerapps.driftinspector.ui.util.localization.Tr
import com.chimerapps.driftinspector.ui.util.mapNotNull
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.ui.AnActionButton
import com.intellij.ui.SearchTextField
import com.intellij.ui.ToolbarDecorator
import net.sf.jsqlparser.parser.CCJSqlParserManager
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.update.Update
import java.awt.BorderLayout
import java.io.File
import java.io.StringReader
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.JPanel
import javax.swing.JTable


class DriftInspectorTableView(
    private val helper: DriftInspectorTableQueryHelper,
    private val project: Project
) : JPanel(BorderLayout()) {

    private val table = CustomDataTableView(project, ::doRemoveSelectedRows, ::sendUpdateQuery)

    private val rawQuery = object : SearchTextField(true, "drift_inspector_query") {
        override fun onFocusLost() {
            //Don't save
        }
    }

    private var currentRequestId: String? = null
    private var currentDbId: String? = null
    private var currentDatabase: DriftInspectorDatabase? = null
    private var currentTable: DriftInspectorTable? = null
    private var listUpdateHelper: ListUpdateHelper<TableRow>? = null
    private var currentConfirmedSelectStatement: String? = null
    private val toolbar: ActionToolbar
    private val refreshAction: RefreshAction

    init {
        val actionGroup = DefaultActionGroup()

        refreshAction = RefreshAction("Refresh", "Refresh") {
            refresh()
        }
        actionGroup.addAction(refreshAction)

        toolbar = ActionManager.getInstance().createActionToolbar("Drifte Inspector", actionGroup, false)

        rawQuery.textEditor.addActionListener {
            checkAndExecuteRawQuery(rawQuery.text.trim())
        }

        val contentPanel = JPanel(BorderLayout())
        contentPanel.add(rawQuery, BorderLayout.NORTH)

        val decorator = ToolbarDecorator.createDecorator(table)
        decorator.disableUpDownActions()
        decorator.disableAddAction() //TODO re-enable
        decorator.setRemoveAction { doRemoveSelectedRows() }
        decorator.addExtraAction(object : AnActionButton("Export", AllIcons.Actions.Menu_saveall) {
            override fun actionPerformed(e: AnActionEvent?) {
                //TODO
                currentDatabase?.let { database ->
                    val names = database.structure.tables.map { it.sqlName }
                    val requestId = UUID.randomUUID().toString()
                    helper.export(requestId, database.id, names)
                }
            }
        })

        table.autoResizeMode = JTable.AUTO_RESIZE_OFF

        contentPanel.add(decorator.createPanel(), BorderLayout.CENTER)

        add(toolbar.component, BorderLayout.WEST)
        add(contentPanel, BorderLayout.CENTER)
    }

    private fun checkAndExecuteRawQuery(rawQuery: String) {
        val dbId = currentDbId ?: return
        currentConfirmedSelectStatement = null
        if (rawQuery.isBlank()) {
            refresh()
            return
        }

        val tables = getAffectedTables(rawQuery, updateActive = true)

        currentRequestId = UUID.randomUUID().toString()
        markRefreshing()
        if (rawQuery.startsWith("SELECT"))
            helper.query(currentRequestId!!, dbId, rawQuery)
        else
            helper.updateItem(currentRequestId!!, dbId, rawQuery, tables.orEmpty(), emptyList())
        if (tables != null)
            this.rawQuery.addCurrentTextToHistory()
    }

    fun update(dbId: String, database: DriftInspectorDatabase, databaseName: String, table: DriftInspectorTable) {
        currentDbId = dbId
        currentDatabase = database
        currentTable = table
        rawQuery.text = ""
        currentConfirmedSelectStatement = null

        this.table.updateModel(databaseName, table)

        listUpdateHelper = ListUpdateHelper(this.table.internalModel, TableRowComparator(table))

        refresh()
    }

    private fun sendUpdateQuery(
        table: DriftInspectorTable,
        column: DriftInspectorColumn,
        data: TableRow,
        newValue: String?
    ) {
        val variables = mutableListOf<DriftInspectorVariable>()
        val query = buildString {
            append("UPDATE ${table.sqlName} SET ")

            val name = column.name
            append(name)
            append("=? ")

            variables += makeVariable(column, newValue)

            append(createMatch(data, table, variables))
        }

        currentDbId?.let { databaseId ->
            currentRequestId = UUID.randomUUID().toString()
            refreshAction.refreshing = true
            helper.updateItem(currentRequestId!!, databaseId, query, listOf(table.sqlName), variables)
        }
    }

    private fun makeVariable(
        column: DriftInspectorColumn,
        newValue: String?,
        rawValue: Any? = null
    ): DriftInspectorVariable {
        if (newValue == null && rawValue == null) {
            return DriftInspectorVariable("int", newValue)
        }
        @Suppress("UNCHECKED_CAST")
        when (column.type.lowercase(Locale.getDefault())) {
            "bit", "tinyint", "smallint", "int", "bigint", "integer" -> return DriftInspectorVariable(
                "int",
                (rawValue as? Number)?.toLong() ?: newValue?.toLong()
            )
            "float", "real", "double" -> return DriftInspectorVariable(
                "real",
                (rawValue as? Number)?.toDouble() ?: newValue?.toDouble()
            )
            "char", "varchar", "text", "nchar", "nvarchar", "ntext" -> return DriftInspectorVariable(
                "string",
                rawValue ?: newValue
            )
            "date", "time", "datetime", "timestamp", "year" -> return DriftInspectorVariable(
                "datetime",
                (rawValue as? Number)?.toLong() ?: (rawValue as? Date)?.time ?: newValue?.toLongOrNull()
                ?: (DateTimeFormatter.ISO_INSTANT.parse(newValue) as? Instant)?.toEpochMilli()
            )
            "blob" -> return DriftInspectorVariable(
                "blob",
                (rawValue as? List<Int>) ?: newValue?.split(',')?.map { it.trim().toInt() })
        }
        throw IllegalStateException("Could create statement for column, type not supported: ${column.type}")
    }

    private fun equalsForKey(
        keyData: Any?,
        table: DriftInspectorTable,
        column: String,
        variables: MutableList<DriftInspectorVariable>
    ): String {
        if (keyData == null) return "IS NULL"
        val tableColumn = table.columns.find { it.name == column }
            ?: throw IllegalStateException("Could create statement for column, column not found")

        variables += makeVariable(tableColumn, newValue = null, rawValue = keyData)

        return "=?"
    }

    private fun executeBulkQuery(queriesToExecute: List<Pair<String, MutableList<DriftInspectorVariable>>>) {
        currentDbId?.let { dbId ->
            currentRequestId = UUID.randomUUID().toString()
            markRefreshing()
            helper.bulkUpdate(currentRequestId!!, dbId, queriesToExecute.map { (query, variables) ->
                BulkActionData(query, variables = variables, affectedTables = getAffectedTables(query, false).orEmpty())
            })
        }
    }

    private fun refresh() {
        currentDbId?.let { dbId ->
            currentTable?.let { table ->
                currentRequestId = UUID.randomUUID().toString()
                val activeSelect = currentConfirmedSelectStatement
                markRefreshing()
                if (activeSelect != null) {
                    helper.query(currentRequestId!!, dbId, activeSelect)
                } else {
                    helper.query(currentRequestId!!, dbId, "SELECT * FROM ${table.sqlName}")
                }
            }
        }
    }

    fun onQueryResults(requestId: String, data: List<Map<String, Any?>>, columns: List<String>) {
        ensureMain {
            if (currentRequestId != requestId) return@ensureMain

            currentTable?.let { table ->
                refreshAction.refreshing = false
                toolbar.updateActionsImmediately()
                if (this.table.ensureColumns(columns)) {
                    listUpdateHelper = ListUpdateHelper(this.table.internalModel, TableRowComparator(table))
                }
                listUpdateHelper?.onListUpdated(data.map { TableRow(it) })
            }
        }
    }

    fun onUpdateComplete(requestId: String) {
        ensureMain {
            if (currentRequestId != requestId) return@ensureMain
            refresh()
        }
    }

    private fun markRefreshing() {
        refreshAction.refreshing = true
        toolbar.updateActionsImmediately()
    }

    fun onError(requestId: String) {
        ensureMain {
            if (currentRequestId != requestId) return@ensureMain
            refreshAction.refreshing = false
            toolbar.updateActionsImmediately()
        }
    }

    private fun createMatch(
        data: TableRow,
        table: DriftInspectorTable,
        variables: MutableList<DriftInspectorVariable>
    ): String {
        val primaryKeys = table.primaryKey
        return buildString {
            if (primaryKeys != null) {
                append("WHERE ")
                primaryKeys.forEachIndexed { index, column ->
                    if (index > 0)
                        append(" AND ")
                    val keyData = data.data[column]
                    append(column)
                    append(" ")
                    append(equalsForKey(keyData, table, column, variables))
                }
            } else if (!table.withoutRowId && data.data.entries.any {
                    it.key.equals(
                        "rowid",
                        ignoreCase = true
                    )
                }) {
                append(
                    "WHERE rowid = ${
                        data.data.entries.find {
                            it.key.equals(
                                "rowid",
                                ignoreCase = true
                            )
                        }?.value
                    }"
                )
            } else {
                append("WHERE ")
                data.data.entries.forEachIndexed { index, (column, data) ->
                    if (index > 0)
                        append(" AND ")
                    append(column)
                    append(" ")
                    append(equalsForKey(data, table, column, variables))
                }
            }
        }
    }

    private fun getAffectedTables(query: String, updateActive: Boolean): List<String>? {
        try {
            when (val statement = CCJSqlParserManager().parse(StringReader(query))) {
                is Select -> {
                    if (updateActive) currentConfirmedSelectStatement = query.trim()
                    return emptyList()
                }
                is Update -> {
                    return listOf(statement.table.name)
                }
                is Delete -> {
                    return listOf(statement.table.name)
                }
                is Insert -> {
                    return listOf(statement.table.name)
                }
                else -> {
                    return emptyList()
                }
            }
        } catch (e: Throwable) {
            NotificationUtil.error("Invalid sql", "Failed to parse sql statement: ${e.message}", project)
            return null
        }
    }

    private fun doRemoveSelectedRows() {
        currentTable?.let { currentTable ->
            listUpdateHelper?.let { list ->

                val queriesToExecute = table.selectedRows.mapNotNull { row ->
                    val data = list.dataAtRow(row) ?: return@mapNotNull null
                    val variables = mutableListOf<DriftInspectorVariable>()
                    val query = buildString {
                        append("DELETE FROM ${currentTable.sqlName} ")
                        append(createMatch(data, currentTable, variables))
                    }
                    query to variables
                }
                executeBulkQuery(queriesToExecute)
            }
        }
    }

    fun onExportResult(databaseId: String, requestId: String, exportResponse: ExportResponse, file: File) {
        if (currentDbId != databaseId) return

        val database = currentDatabase ?: return

        ApplicationManager.getApplication().invokeLater {
            runWriteAction {
                val handler = SqlExportHandler(file)
                handler.handle(exportResponse, database)
                NotificationUtil.info(
                    Tr.ActionExportCompleteTitle.tr(),
                    Tr.ActionExportCompleteBody.tr(file.absolutePath, file.name),
                    project
                )
            }
        }
    }
}

data class TableRow(val data: Map<String, Any?>)

private class TableRowComparator(table: DriftInspectorTable) : DiffUtilComparator<TableRow> {

    private val primaryKeys = table.primaryKey

    override fun representSameItem(left: TableRow, right: TableRow): Boolean {
        if (primaryKeys == null || primaryKeys.isEmpty()) {
            val leftRowId = left.data.entries.find { it.key.equals("rowid", ignoreCase = true) }?.value
            val rightRowId = right.data.entries.find { it.key.equals("rowid", ignoreCase = true) }?.value
            if (leftRowId != null && rightRowId != null)
                return leftRowId == rightRowId
            return false //No rowId for one or the other -> bail
        }

        return left.data.filter { it.key in primaryKeys } == right.data.filter { it.key in primaryKeys }
    }

}

interface DriftInspectorTableQueryHelper {

    fun query(requestId: String, databaseId: String, query: String)

    fun updateItem(
        requestId: String,
        databaseId: String,
        query: String,
        affectedTables: List<String>,
        variables: List<DriftInspectorVariable>
    )

    fun bulkUpdate(requestId: String, databaseId: String, data: List<BulkActionData>)

    fun export(requestId: String, databaseId: String, tableNames: List<String>)

}

data class DriftInspectorVariable(val type: String, val data: Any?)

data class BulkActionData(
    val query: String,
    val affectedTables: List<String>,
    val variables: List<DriftInspectorVariable>
)