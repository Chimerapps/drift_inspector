package com.chimerapps.moorinspector.ui.view

import com.chimerapps.moorinspector.client.protocol.MoorInspectorColumn
import com.chimerapps.moorinspector.client.protocol.MoorInspectorTable
import com.chimerapps.moorinspector.ui.actions.RefreshAction
import com.chimerapps.moorinspector.ui.util.NotificationUtil
import com.chimerapps.moorinspector.ui.util.ensureMain
import com.chimerapps.moorinspector.ui.util.list.ListUpdateHelper
import com.chimerapps.moorinspector.ui.util.sql.SqlUtil
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
                    helper.updateItem(currentRequestId!!, dbId, rawQuery, listOf(statement.table.name))
                }
                is Delete -> {
                    currentRequestId = UUID.randomUUID().toString()
                    markRefreshing()
                    helper.updateItem(currentRequestId!!, dbId, rawQuery, listOf(statement.table.name))
                }
                is Insert -> {
                    currentRequestId = UUID.randomUUID().toString()
                    markRefreshing()
                    helper.updateItem(currentRequestId!!, dbId, rawQuery, listOf(statement.table.name))
                }
                else -> {
                    currentRequestId = UUID.randomUUID().toString()
                    markRefreshing()
                    helper.updateItem(currentRequestId!!, dbId, rawQuery, listOf())
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

        listUpdateHelper = ListUpdateHelper(model) { o1, o2 ->
            val o1data = o1.data
            val o2data = o2.data
            if (o1data == o2data) 0 else -1
        }

        this.table.setModelAndUpdateColumns(model)
        this.table.updateColumnSizes()

        refresh()
    }

    private fun buildUpdateQuery(
        table: MoorInspectorTable,
        column: MoorInspectorColumn,
        data: TableRow,
        newValue: String?
    ) {
        val query = buildString {
            append("UPDATE ${table.sqlName} SET ")

            val name = column.name
            append(name)
            append(" = ")
            append(assignmentForKey(newValue, table, name))
            append(" ")

            val primaryKeys = table.primaryKey
            if (primaryKeys != null) {
                append("WHERE ")
                primaryKeys.forEachIndexed { index, column ->
                    if (index > 0)
                        append(" AND ")
                    val keyData = data.data[column]
                    append(column)
                    append(" ")
                    append(equalsForKey(keyData, table, column))
                }
            } else if (!table.withoutRowId && data.data["rowid"] != null) {
                append("WHERE rowid = ${data.data["rowid"]}")
            }
        }

        currentDbId?.let { databaseId ->
            currentRequestId = UUID.randomUUID().toString()
            refreshAction.refreshing = true
            helper.updateItem(currentRequestId!!, databaseId, query, listOf(table.sqlName))
        }
    }

    private fun assignmentForKey(keyData: String?, table: MoorInspectorTable, column: String): String {
        if (keyData == null || keyData.isEmpty()) return "NULL"

        val type = table.columns.find { it.name == column }?.type
            ?: throw IllegalStateException("Could create statement for column, column not found")

        when (type.toLowerCase(Locale.getDefault())) {
            "bit", "tinyint", "smallint", "int", "bigint", "decimal", "numeric", "float", "real", "integer" -> return "$keyData"
            "char", "varchar", "text", "nchar", "nvarchar", "ntext" -> return "'${SqlUtil.escape(keyData.toString())}'"
            "date", "time", "datetime", "timestamp", "year" -> return "$keyData"
        }
        throw IllegalStateException("Don't know how to match type: $type")
    }

    private fun equalsForKey(keyData: Any?, table: MoorInspectorTable, column: String): String {
        if (keyData == null) return "IS NULL"
        val type = table.columns.find { it.name == column }?.type
            ?: throw IllegalStateException("Could create statement for column, column not found")


        when (type.toLowerCase(Locale.getDefault())) {
            "bit", "tinyint", "smallint", "int", "bigint", "decimal", "numeric", "float", "real", "integer" -> return "= $keyData"
            "char", "varchar", "text", "nchar", "nvarchar", "ntext" -> return "= '${SqlUtil.escape(keyData.toString())}'"
            "date", "time", "datetime", "timestamp", "year" -> return "= $keyData"
        }
        throw IllegalStateException("Don't know how to match type: $type")
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

    private inner class TableViewColumnInfo(val column: MoorInspectorColumn, private val table: MoorInspectorTable) :
        ColumnInfo<TableRow, String>(column.name) {

        override fun valueOf(item: TableRow?): String? = item?.data?.get(column.name)?.toString()

        override fun isCellEditable(item: TableRow): Boolean {
            return column.name.toLowerCase(Locale.getDefault()) != "rowid"
        }

        override fun setValue(item: TableRow, value: String?) {
            if (item.data[column.name]?.toString() != value)
                buildUpdateQuery(table, column, item, value)
        }
    }

}

data class TableRow(val data: Map<String, Any?>)

interface MoorInspectorTableQueryHelper {

    fun query(requestId: String, databaseId: String, query: String)

    fun updateItem(requestId: String, databaseId: String, query: String, affectedTables: List<String>)

}