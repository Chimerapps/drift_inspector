package com.chimerapps.moorinspector.export.sql

import com.chimerapps.moorinspector.client.protocol.ExportResponse
import com.chimerapps.moorinspector.client.protocol.MoorInspectorDatabase
import com.google.gson.GsonBuilder
import java.io.File
import java.sql.DriverManager
import java.util.*

class SqlExportHandler(file: File) {

    private companion object {
        init {
            Class.forName("org.sqlite.JDBC")
        }
    }

    private val connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
    private val gson = GsonBuilder().create()

    fun handle(exportResponse: ExportResponse, database: MoorInspectorDatabase) {
        exportResponse.schemas.forEach {
            exec(it)
        }

        exportResponse.data.forEach {
            val table = it.name
            val rows = it.data

            rows.forEach { rowData ->
                val statement = buildString {
                    append("INSERT INTO $table (")
                    var c = 0
                    rowData.forEach { (key, _) ->
                        if (c++ > 0)
                            append(", ")
                        append(key)
                    }
                    append(") VALUES (")
                    c = 0
                    rowData.forEach { (_, _) ->
                        if (c++ > 0)
                            append(", ")
                        append("?")
                    }
                    append(")")
                }
                connection.prepareStatement(statement).use { preparedStatement ->
                    var c = 0
                    rowData.forEach { (key, value) ->
                        when (value) {
                            is String -> preparedStatement.setString(++c, value)
                            is Int -> preparedStatement.setInt(++c, value)
                            is Float -> preparedStatement.setFloat(++c, value)
                            is Long -> preparedStatement.setLong(++c, value)
                            is Double -> preparedStatement.setDouble(++c, value)
                            is Date -> preparedStatement.setDate(++c, java.sql.Date(value.time))
                            null -> preparedStatement.setNull(++c, getSqlType(table, database, key))
                            else -> print("Not supported -> $value")
                            //TODO blob support
                        }
                    }
                    preparedStatement.execute()
                }
            }
        }
    }

    private fun getSqlType(table: String, database: MoorInspectorDatabase, key: String): Int {
        val actualTable = database.structure.tables.find { it.sqlName == table }!!
        val column = actualTable.columns.find { it.name == key }!!

        when (column.type.toLowerCase()) {
            "bit", "tinyint", "smallint", "int", "bigint", "integer" -> {
                if (column.isBoolean) {
                    return java.sql.Types.BOOLEAN
                }
                return java.sql.Types.INTEGER
            }
            "float", "real", "double" -> return java.sql.Types.REAL
            "char", "varchar", "text", "nchar", "nvarchar", "ntext" -> return java.sql.Types.VARCHAR
            "date", "time", "datetime", "timestamp", "year" -> return java.sql.Types.TIME
            "blob" -> return java.sql.Types.BLOB
        }
        throw IllegalStateException("Unknown column type: ${column.type}")
    }


    private fun exec(sqlStatement: String): Boolean {
        return connection.prepareStatement(sqlStatement).use {
            it.execute()
        }
    }
}