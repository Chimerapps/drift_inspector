package com.chimerapps.moorinspector.ui.view

import com.chimerapps.moorinspector.client.protocol.MoorInspectorColumn
import com.chimerapps.moorinspector.client.protocol.MoorInspectorTable
import com.chimerapps.moorinspector.ui.actions.RefreshAction
import com.chimerapps.moorinspector.ui.util.NotificationUtil
import com.chimerapps.moorinspector.ui.util.ensureMain
import com.chimerapps.moorinspector.ui.util.list.DiffUtilComparator
import com.chimerapps.moorinspector.ui.util.list.ListUpdateHelper
import com.chimerapps.moorinspector.ui.util.mapNotNull
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.table.TableView
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import net.sf.jsqlparser.parser.CCJSqlParserManager
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.update.Update
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.io.StringReader
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.DefaultCellEditor
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellEditor


class MoorInspectorTableView(
    private val helper: MoorInspectorTableQueryHelper,
    private val project: Project
) : JPanel(BorderLayout()) {

    private val table = object : TableView<TableRow>() {
        override fun prepareEditor(editor: TableCellEditor?, row: Int, column: Int): Component {
            (editor as? DefaultCellEditor)?.clickCountToStart = 2
            return super.prepareEditor(editor, row, column)
        }
    }.also {
        val header: JTableHeader = it.tableHeader

        header.reorderingAllowed = false

        it.rowHeight = PlatformIcons.CLASS_ICON.iconHeight * 2
        it.preferredScrollableViewportSize = JBUI.size(-1, 150)
        it.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)

        it.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                super.keyPressed(e)
                currentDbId?.let { dbId ->
                    currentTable?.let { currentTable ->
                        listUpdateHelper?.let { list ->
                            if (e.keyCode == KeyEvent.VK_DELETE || e.keyCode == KeyEvent.VK_BACK_SPACE) {
                                val queriesToExecute = it.selectedRows.mapNotNull { row ->
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
                }
            }
        })
    }

    private val rawQuery = object : SearchTextField(true, "moor_inspector_query") {
        override fun onFocusLost() {
            //Don't save
        }
    }

    private var currentRequestId: String? = null
    private var currentDbId: String? = null
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

        contentPanel.add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)

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

    fun update(dbId: String, table: MoorInspectorTable) {
        currentDbId = dbId
        currentTable = table
        rawQuery.text = ""
        currentConfirmedSelectStatement = null

        val model = ListTableModel(
            table.columns.map { TableViewColumnInfo(it, table) }.toTypedArray(),
            listOf(TableRow(emptyMap())),
            0
        )

        listUpdateHelper = ListUpdateHelper(model, TableRowComparator(table))

        this.table.setModelAndUpdateColumns(model)
        this.table.updateColumnSizes()

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

    private inner class TableViewColumnInfo(val column: MoorInspectorColumn, private val table: MoorInspectorTable) :
        ColumnInfo<TableRow, String>(column.name) {

        override fun valueOf(item: TableRow?): String? {
            val raw = item?.data?.get(column.name) ?: return null
            @Suppress("UNCHECKED_CAST")
            when (column.type.toLowerCase(Locale.getDefault())) {
                "bit", "tinyint", "smallint", "int", "bigint", "integer" -> return (raw as Number).toLong().toString()
                "float", "real", "double" -> return (raw as Number).toDouble().toString()
                "char", "varchar", "text", "nchar", "nvarchar", "ntext" -> return raw.toString()
                "date", "time", "datetime", "timestamp", "year" -> return DateTimeFormatter.ISO_INSTANT.format(
                    Instant.ofEpochMilli(
                        (raw as Number).toLong()
                    )
                )
                "blob" -> return (raw as List<Int>).joinToString()
            }
            return raw.toString()
        }

        override fun isCellEditable(item: TableRow): Boolean {
            return column.name.toLowerCase(Locale.getDefault()) != "rowid"
        }

        override fun setValue(item: TableRow, value: String?) {
            try {
                if (!isSame(item.data[column.name], value)) {
                    sendUpdateQuery(table, column, item, value)
                }
            } catch (e: Throwable) {
                NotificationUtil.error("Update failed", "Failed to update: ${e.message}", project)
            }
        }

        private fun isSame(original: Any?, value: String?): Boolean {
            if (original == null && value == null) return true
            else if (original != null && value == null) return false
            else if (original == null && value != null) return false

            @Suppress("UNCHECKED_CAST")
            when (column.type.toLowerCase(Locale.getDefault())) {
                "bit", "tinyint", "smallint", "int", "bigint", "integer" -> return (original as? Number)?.toLong() == value?.toLong()
                "float", "real", "double" -> return (original as? Number)?.toDouble() == value?.toDouble()
                "char", "varchar", "text", "nchar", "nvarchar", "ntext" -> return original == value
                "date", "time", "datetime", "timestamp", "year" -> {
                    val newInstant = if (value != null) {
                        value.toLongOrNull()
                            ?: (DateTimeFormatter.ISO_INSTANT.parse(value) as? Instant)?.toEpochMilli()
                    } else null
                    val old = (original as? Number)?.toLong()

                    return newInstant == old
                }
                "blob" -> return (original as? List<Int>) == value?.split(',')?.map { it.trim().toInt() }
            }
            throw IllegalStateException("Could create statement for column, type not supported: ${column.type}")
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

}

data class MoorInspectorVariable(val type: String, val data: Any?)

data class BulkActionData(
    val query: String,
    val affectedTables: List<String>,
    val variables: List<MoorInspectorVariable>
)