package com.chimerapps.moorinspector.ui.util.list

import com.chimerapps.moorinspector.ui.util.dispatchMain
import com.intellij.util.ui.ListTableModel
import javax.swing.table.AbstractTableModel
import kotlin.concurrent.thread

class ListUpdateHelper<T>(
    private val model: ListTableModel<T>,
    private val comparator: DiffUtilComparator<T>
) {

    private var hasInit = false
    private val internalListData = mutableListOf<T>()
    private var oldRunThread: Thread? = null

    fun onListUpdated(
        newListData: List<T>,
    ) {
        if (!hasInit) {
            hasInit = true
            internalListData.addAll(newListData)
            model.items = internalListData
            return
        }
        oldRunThread?.interrupt()
        oldRunThread?.join()

        oldRunThread = thread(name = "DiffUtil") {
            try {
                val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override val oldListSize: Int =
                        if (Thread.interrupted()) throw InterruptedException() else internalListData.size
                    override val newListSize: Int =
                        if (Thread.interrupted()) throw InterruptedException() else newListData.size

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        if (Thread.interrupted()) throw InterruptedException()
                        return comparator.representSameItem(
                            internalListData[oldItemPosition],
                            newListData[newItemPosition]
                        )
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        if (Thread.interrupted()) throw InterruptedException()
                        return comparator.areItemContentsEqual(
                            internalListData[oldItemPosition],
                            newListData[newItemPosition]
                        )
                    }
                })
                val t = Thread.currentThread()
                dispatchMain {
                    if (!t.isInterrupted) {
                        internalListData.clear()
                        internalListData.addAll(newListData)

                        diff.dispatchUpdatesTo(TableModelDiffUtilDispatcher(model))
                    }
                }
            } catch (e: Throwable) {
                //Ignore
            }
        }
    }

    fun dataAtRow(index: Int): T? {
        return internalListData[index]
    }

}

class TableModelDiffUtilDispatcher(private val model: AbstractTableModel) : ListUpdateCallback {
    override fun onInserted(position: Int, count: Int) {
        model.fireTableRowsInserted(position, position + count)
    }

    override fun onRemoved(position: Int, count: Int) {
        model.fireTableRowsDeleted(position, position + count)
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        model.fireTableRowsUpdated(fromPosition, fromPosition)
        model.fireTableRowsUpdated(toPosition, toPosition)
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        model.fireTableRowsUpdated(position, position + count)
    }
}

interface DiffUtilComparator<T> {
    fun representSameItem(left: T, right: T): Boolean
    fun areItemContentsEqual(left: T, right: T): Boolean = left == right
}