package com.chimerapps.driftinspector.ui.util

import com.intellij.openapi.application.ApplicationManager

fun dispatchMain(toExecute: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(toExecute)
}

fun ensureMain(toExecute: () -> Unit) {
    if (ApplicationManager.getApplication().isDispatchThread)
        toExecute()
    else
        dispatchMain(toExecute)
}

inline fun <T> IntArray.mapNotNull(block: (Int) -> T?): List<T> {
    val result = mutableListOf<T>()
    forEach {
        block(it)?.let { calculationResult -> result += calculationResult }
    }
    return result
}