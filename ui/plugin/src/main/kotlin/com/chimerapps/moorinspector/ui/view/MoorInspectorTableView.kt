package com.chimerapps.moorinspector.ui.view

import com.chimerapps.moorinspector.client.protocol.ExportResponse
import com.chimerapps.moorinspector.client.protocol.MoorInspectorColumn
import com.chimerapps.moorinspector.client.protocol.MoorInspectorDatabase
import com.chimerapps.moorinspector.client.protocol.MoorInspectorTable
import com.chimerapps.moorinspector.export.sql.SqlExportHandler
import com.chimerapps.moorinspector.ui.actions.RefreshAction
import com.chimerapps.moorinspector.ui.util.NotificationUtil
import com.chimerapps.moorinspector.ui.util.ensureMain
import com.chimerapps.moorinspector.ui.util.list.DiffUtilComparator
import com.chimerapps.moorinspector.ui.util.list.ListUpdateHelper
import com.chimerapps.moorinspector.ui.util.mapNotNull
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
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


class MoorInspectorTableView(
    private val helper: MoorInspectorTableQueryHelper,
    private val project: Project
) : JPanel(BorderLayout()) {

    private val table = CustomDataTableView(project, ::doRemoveSelectedRows, ::sendUpdateQuery)

    private val rawQuery = object : SearchTextField(true, "moor_inspector_query") {
        override fun onFocusLost() {
            //Don't save
        }
    }

    private var currentRequestId: String? = null
    private var currentDbId: String? = null
    private var currentDatabase: MoorInspectorDatabase? = null
    private var currentTable: MoorInspectorTable? = null
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

        toolbar = ActionManager.getInstance().createActionToolbar("Moore Inspector", actionGroup, false)

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

    fun update(dbId: String, database: MoorInspectorDatabase, databaseName: String, table: MoorInspectorTable) {
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
        table: MoorInspectorTable,
        column: MoorInspectorColumn,
        data: TableRow,
        newValue: String?
    ) {
        val variables = mutableListOf<MoorInspectorVariable>()
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
        column: MoorInspectorColumn,
        newValue: String?,
        rawValue: Any? = null
    ): MoorInspectorVariable {
        if (newValue == null && rawValue == null) {
            return MoorInspectorVariable("int", newValue)
        }
        @Suppress("UNCHECKED_CAST")
        when (column.type.toLowerCase(Locale.getDefault())) {
            "bit", "tinyint", "smallint", "int", "bigint", "integer" -> return MoorInspectorVariable(
                "int",
                (rawValue as? Number)?.toLong() ?: newValue?.toLong()
            )
            "float", "real", "double" -> return MoorInspectorVariable(
                "real",
                (rawValue as? Number)?.toDouble() ?: newValue?.toDouble()
            )
            "char", "varchar", "text", "nchar", "nvarchar", "ntext" -> return MoorInspectorVariable(
                "string",
                rawValue ?: newValue
            )
            "date", "time", "datetime", "timestamp", "year" -> return MoorInspectorVariable(
                "datetime",
                (rawValue as? Number)?.toLong() ?: (rawValue as? Date)?.time ?: newValue?.toLongOrNull()
                ?: (DateTimeFormatter.ISO_INSTANT.parse(newValue) as? Instant)?.toEpochMilli()
            )
            "blob" -> return MoorInspectorVariable(
                "blob",
                (rawValue as? List<Int>) ?: newValue?.split(',')?.map { it.trim().toInt() })
        }
        throw IllegalStateException("Could create statement for column, type not supported: ${column.type}")
    }

    private fun equalsForKey(
        keyData: Any?,
        table: MoorInspectorTable,
        column: String,
        variables: MutableList<MoorInspectorVariable>
    ): String {
        if (keyData == null) return "IS NULL"
        val tableColumn = table.columns.find { it.name == column }
            ?: throw IllegalStateException("Could create statement for column, column not found")

        variables += makeVariable(tableColumn, newValue = null, rawValue = keyData)

        return "=?"
    }

    private fun executeBulkQuery(queriesToExecute: List<Pair<String, MutableList<MoorInspectorVariable>>>) {
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

    fun onQueryResults(requestId: String, data: List<Map<String, Any?>>) {
        ensureMain {
            if (currentRequestId != requestId) return@ensureMain

            refreshAction.refreshing = false
            toolbar.updateActionsImmediately()
            listUpdateHelper?.onListUpdated(data.map { TableRow(it) })
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
        table: MoorInspectorTable,
        variables: MutableList<MoorInspectorVariable>
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
                    val variables = mutableListOf<MoorInspectorVariable>()
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

    fun onExportResult(databaseId: String, requestId: String, exportResponse: ExportResponse) {
        if (currentDbId != databaseId) return

        val database = currentDatabase ?: return

        val handler = SqlExportHandler(File("/Users/nicolaverbeeck/Work/Chimerapps/Tools/moor_inspector/ui/test.db").also { it.delete() })
        handler.handle(exportResponse, database)
    }
}

data class TableRow(val data: Map<String, Any?>)

private class TableRowComparator(table: MoorInspectorTable) : DiffUtilComparator<TableRow> {

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

interface MoorInspectorTableQueryHelper {

    fun query(requestId: String, databaseId: String, query: String)

    fun updateItem(
        requestId: String,
        databaseId: String,
        query: String,
        affectedTables: List<String>,
        variables: List<MoorInspectorVariable>
    )

    fun bulkUpdate(requestId: String, databaseId: String, data: List<BulkActionData>)

    fun export(requestId: String, databaseId: String, tableNames: List<String>)

}

data class MoorInspectorVariable(val type: String, val data: Any?)

data class BulkActionData(
    val query: String,
    val affectedTables: List<String>,
    val variables: List<MoorInspectorVariable>
)