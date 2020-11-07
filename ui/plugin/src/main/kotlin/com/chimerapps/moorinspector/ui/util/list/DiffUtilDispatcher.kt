package com.chimerapps.moorinspector.ui.util.list

import com.chimerapps.moorinspector.ui.util.dispatchMain
import com.intellij.util.ui.TableViewModel
import javax.swing.table.AbstractTableModel
import kotlin.concurrent.thread

class ListUpdateHelper<T>(
    private val model: TableViewModel<T>,
    private val compare: Comparator<T> = Comparator { o1, o2 -> if (o1 == o2) 0 else -1 }
) {

    private var hasInit = false
    private val internalListData = mutableListOf<T>()
    private var oldRunThread: Thread? = null

    fun onListUpdated(
        newListData: List<T>,
        comparator: Comparator<T> = compare,
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
                        return comparator.compare(internalListData[oldItemPosition], newListData[newItemPosition]) == 0
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        if (Thread.interrupted()) throw InterruptedException()
                        return comparator.compare(internalListData[oldItemPosition], newListData[newItemPosition]) == 0
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