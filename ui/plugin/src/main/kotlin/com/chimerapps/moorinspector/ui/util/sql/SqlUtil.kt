package com.chimerapps.moorinspector.ui.util.sql

object SqlUtil {


    fun escape(string: String): String {
        return string.replace("\\", "\\\\")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\\x1A", "\\Z")
            .replace("\\x00", "\\0")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("%", "\\%")
    }

}