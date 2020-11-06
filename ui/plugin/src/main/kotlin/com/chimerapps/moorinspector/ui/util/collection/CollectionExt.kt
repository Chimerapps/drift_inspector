package com.chimerapps.moorinspector.ui.util.collection

import java.util.*

fun <T> List<T>.enumerate(): Enumeration<T> {
    val it = iterator()
    return object : Enumeration<T> {
        override fun hasMoreElements(): Boolean = it.hasNext()

        override fun nextElement(): T = it.next()
    }
}