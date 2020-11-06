package com.chimerapps.moorinspector.ui.view

import com.chimerapps.moorinspector.client.protocol.MoorInspectorColumn
import com.chimerapps.moorinspector.client.protocol.MoorInspectorTable
import com.chimerapps.moorinspector.ui.util.ensureMain
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.TableView
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import java.awt.BorderLayout
import java.util.*
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.JTableHeader


class MoorInspectorTableView(private val helper: MoorInspectorTableQueryHelper) : JPanel(BorderLayout()) {

    private val table = TableView<TableRow>().also {
        val header: JTableHeader = it.tableHeader
//        header.addMouseMotionListener(object : MouseMotionAdapter() {
//            override fun mouseMoved(e: MouseEvent) {
//                updateTooltip(e)
//            }
//        })
//        header.addMouseListener(object : MouseAdapter() {
//            override fun mouseEntered(e: MouseEvent) {
//                updateTooltip(e)
//            }
//        })
        header.reorderingAllowed = false

        it.rowHeight = PlatformIcons.CLASS_ICON.iconHeight
        it.preferredScrollableViewportSize = JBUI.size(-1, 150)
        it.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

    }

    private var currentRequestId: String? = null
    private var currentDbId: String? = null
    private var currentTable: MoorInspectorTable? = null

    init {
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)
    }

    fun update(dbId: String, table: MoorInspectorTable) {
        currentDbId = dbId
        currentTable = table
        this.table.setModelAndUpdateColumns(
            ListTableModel(
                table.columns.map { TableViewColumnInfo(it, table) }.toTypedArray(),
                listOf(TableRow(emptyMap())),
                0
            )
        )
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
            helper.updateItem(currentRequestId!!, databaseId, query, listOf(table.sqlName))
        }
    }

    private fun assignmentForKey(keyData: String?, table: MoorInspectorTable, column: String): String {
        if (keyData == null || keyData.isEmpty()) return "NULL"

        val type = table.columns.find { it.name == column }?.type
            ?: throw IllegalStateException("Could create statement for column, column not found")

        when (type.toLowerCase(Locale.getDefault())) {
            "bit", "tinyint", "smallint", "int", "bigint", "decimal", "numeric", "float", "real", "integer" -> return "$keyData"
            "char", "varchar", "text", "nchar", "nvarchar", "ntext" -> return "'$keyData'"
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
            "char", "varchar", "text", "nchar", "nvarchar", "ntext" -> return "= '$keyData'"
            "date", "time", "datetime", "timestamp", "year" -> return "= $keyData"
        }
        throw IllegalStateException("Don't know how to match type: $type")
    }

    fun refresh() {
        currentDbId?.let { dbId ->
            currentTable?.let { table ->
                currentRequestId = UUID.randomUUID().toString()
                if (table.withoutRowId)
                    helper.query(currentRequestId!!, dbId, "SELECT * FROM ${table.sqlName}")
                else
                    helper.query(currentRequestId!!, dbId, "SELECT rowid,* FROM ${table.sqlName}")
            }
        }
    }

    fun onQueryResults(requestId: String, data: List<Map<String, Any?>>) {
        if (currentRequestId != requestId) return

        ensureMain {
            @Suppress("UNCHECKED_CAST")
            (table.model as ListTableModel<TableRow>).items = data.map { TableRow(it) }
        }
    }

    fun onUpdateComplete(requestId: String) {
        if (currentRequestId != requestId) return
        refresh()
    }

    private inner class TableViewColumnInfo(val column: MoorInspectorColumn, private val table: MoorInspectorTable) :
        ColumnInfo<TableRow, String>(column.name) {

        override fun valueOf(item: TableRow?): String? = item?.data?.get(column.name)?.toString()

        override fun isCellEditable(item: TableRow): Boolean {
            return column.name.toLowerCase(Locale.getDefault()) != "rowid"
        }

        override fun setValue(item: TableRow, value: String?) {
            buildUpdateQuery(table, column, item, value)
        }
    }

}

data class TableRow(val data: Map<String, Any?>)

interface MoorInspectorTableQueryHelper {

    fun query(requestId: String, databaseId: String, query: String)

    fun updateItem(requestId: String, databaseId: String, query: String, affectedTables: List<String>)

}