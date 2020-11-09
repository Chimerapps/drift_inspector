package com.chimerapps.moorinspector.ui.view

import com.chimerapps.moorinspector.client.protocol.MoorInspectorColumn
import com.chimerapps.moorinspector.client.protocol.MoorInspectorTable
import com.chimerapps.moorinspector.ui.actions.RefreshAction
import com.chimerapps.moorinspector.ui.util.NotificationUtil
import com.chimerapps.moorinspector.ui.util.ensureMain
import com.chimerapps.moorinspector.ui.util.list.DiffUtilComparator
import com.chimerapps.moorinspector.ui.util.list.ListUpdateHelper
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
import java.io.StringReader
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.JTableHeader


class MoorInspectorTableView(
    private val helper: MoorInspectorTableQueryHelper,
    private val project: Project
) : JPanel(BorderLayout()) {

    private val table = TableView<TableRow>().also {
        val header: JTableHeader = it.tableHeader

        header.reorderingAllowed = false

        it.rowHeight = PlatformIcons.CLASS_ICON.iconHeight * 2
        it.preferredScrollableViewportSize = JBUI.size(-1, 150)
        it.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

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

        try {
            when (val statement = CCJSqlParserManager().parse(StringReader(rawQuery))) {
                is Select -> {
                    currentConfirmedSelectStatement = rawQuery.trim()
                    currentRequestId = UUID.randomUUID().toString()
                    helper.query(currentRequestId!!, dbId, rawQuery)
                }
                is Update -> {
                    currentRequestId = UUID.randomUUID().toString()
                    markRefreshing()
                    helper.updateItem(currentRequestId!!, dbId, rawQuery, listOf(statement.table.name), emptyList())
                }
                is Delete -> {
                    currentRequestId = UUID.randomUUID().toString()
                    markRefreshing()
                    helper.updateItem(currentRequestId!!, dbId, rawQuery, listOf(statement.table.name), emptyList())
                }
                is Insert -> {
                    currentRequestId = UUID.randomUUID().toString()
                    markRefreshing()
                    helper.updateItem(currentRequestId!!, dbId, rawQuery, listOf(statement.table.name), emptyList())
                }
                else -> {
                    currentRequestId = UUID.randomUUID().toString()
                    markRefreshing()
                    helper.updateItem(currentRequestId!!, dbId, rawQuery, listOf(), emptyList())
                }
            }
            this.rawQuery.addCurrentTextToHistory()
            //TODO
        } catch (e: Throwable) {
            NotificationUtil.error("Invalid sql", "Failed to parse sql statement: ${e.message}", project)
            //TODO
        }
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

            val primaryKeys = table.primaryKey
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
            } else if (!table.withoutRowId && data.data.entries.any { it.key.equals("rowid", ignoreCase = true) }) {
                append("WHERE rowid = ${data.data.entries.find { it.key.equals("rowid", ignoreCase = true) }?.value}")
            }
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

    fun refresh() {
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

}

data class MoorInspectorVariable(val type: String, val data: Any?)