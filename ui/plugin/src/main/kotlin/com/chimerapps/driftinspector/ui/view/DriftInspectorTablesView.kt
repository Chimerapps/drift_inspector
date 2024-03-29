package com.chimerapps.driftinspector.ui.view

import com.chimerapps.driftinspector.client.DriftInspectorMessageListener
import com.chimerapps.driftinspector.client.protocol.DriftInspectorDatabase
import com.chimerapps.driftinspector.client.protocol.DriftInspectorServerInfo
import com.chimerapps.driftinspector.client.protocol.DriftInspectorTable
import com.chimerapps.driftinspector.ui.util.collection.enumerate
import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.util.*
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreeSelectionModel

class DriftInspectorTablesView(private val onTableSelectionChanged: (DriftInspectorDatabase, DriftInspectorTable) -> Unit) :
    JPanel(BorderLayout()), DriftInspectorMessageListener {

    private val model = DriftInspectorTreeModel()
    private val tree = Tree(model).also {
        it.showsRootHandles = true
        it.isRootVisible = false
        it.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        it.cellRenderer = TreeCellRenderer()
        it.selectionModel.addTreeSelectionListener { _ ->
            (it.lastSelectedPathComponent as? TableNode)?.let { node ->
                onTableSelectionChanged(node.parent.database, node.table)
            }
        }
    }

    init {
        add(tree, BorderLayout.CENTER)
    }

    override fun onServerInfo(serverInfo: DriftInspectorServerInfo) {
        val root = ServerInfoRootNode(serverInfo)

        model.setRoot(root)
    }
}

private class ServerInfoRootNode(serverInfo: DriftInspectorServerInfo) : TreeNode {

    private val childNodes = serverInfo.databases.map { DatabaseNode(it, this) }

    override fun getChildAt(childIndex: Int): TreeNode = childNodes[childIndex]

    override fun getChildCount(): Int = childNodes.size

    override fun getParent(): TreeNode? = null

    override fun getIndex(node: TreeNode?): Int = childNodes.indexOf(node)

    override fun getAllowsChildren(): Boolean = true

    override fun isLeaf(): Boolean = childNodes.isEmpty()

    override fun children(): Enumeration<*> = childNodes.enumerate()
}

private class DatabaseNode(val database: DriftInspectorDatabase, private val parentNode: TreeNode) : TreeNode {

    private val childNodes = database.structure.tables.map { TableNode(it, this) }

    override fun getChildAt(childIndex: Int): TableNode = childNodes[childIndex]

    override fun getChildCount(): Int = childNodes.size

    override fun getParent(): TreeNode = parentNode

    override fun getIndex(node: TreeNode?): Int = childNodes.indexOf(node)

    override fun getAllowsChildren(): Boolean = true

    override fun isLeaf(): Boolean = childNodes.isEmpty()

    override fun children(): Enumeration<*> = childNodes.enumerate()

    override fun toString(): String {
        return database.name
    }

}

private class TableNode(val table: DriftInspectorTable, private val parentNode: DatabaseNode) : TreeNode {

    override fun getChildAt(childIndex: Int): TreeNode = throw IllegalStateException("No children")

    override fun getChildCount(): Int = 0

    override fun getParent(): DatabaseNode = parentNode

    override fun getIndex(node: TreeNode?): Int = -1

    override fun getAllowsChildren(): Boolean = false

    override fun isLeaf(): Boolean = true

    override fun children(): Enumeration<*> = throw IllegalStateException("No children")

    override fun toString(): String {
        return table.sqlName
    }
}

private class DriftInspectorTreeModel() : DefaultTreeModel(DefaultMutableTreeNode()) {
}

private class TreeCellRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        when (value) {
            is DatabaseNode -> {
                icon = AllIcons.Debugger.Db_db_object
                append(value.database.name)
                append(" V${value.database.structure.version}")
            }
            is TableNode -> {
                icon = AllIcons.Debugger.Value
                append(value.table.sqlName)
            }
        }
    }

}