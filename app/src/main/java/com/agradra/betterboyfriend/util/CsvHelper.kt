package com.agradra.betterboyfriend.util

import com.agradra.betterboyfriend.data.model.RuleItem
import com.agradra.betterboyfriend.data.model.RuleType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object CsvHelper {

    private const val CSV_HEADER = "id,타입,내용,중요도,카테고리,날짜,이유"

    /**
     * Exports rule items to CSV with UTF-8 BOM so that Excel on Windows opens it cleanly without garbled Korean.
     */
    fun exportToCsv(items: List<RuleItem>, outputStream: OutputStream) {
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            // Write UTF-8 BOM
            writer.write("\uFEFF")
            writer.write(CSV_HEADER)
            writer.write("\r\n")

            for (item in items) {
                val row = listOf(
                    item.id.toString(),
                    escapeCsv(item.type.displayName),
                    escapeCsv(item.title),
                    item.priority.toString(),
                    escapeCsv(item.category),
                    escapeCsv(item.date),
                    escapeCsv(item.reason)
                ).joinToString(",")
                writer.write(row)
                writer.write("\r\n")
            }
            writer.flush()
        }
    }

    /**
     * Parses an input stream of CSV file into a list of RuleItem.
     */
    fun importFromCsv(inputStream: InputStream): List<RuleItem> {
        val result = mutableListOf<RuleItem>()
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
            val allLines = readAllCsvRecords(reader)
            if (allLines.isEmpty()) return emptyList()

            // Header inspection
            val firstLine = allLines.first()
            var startIndex = 0
            var idIdx = -1
            var typeIdx = 1
            var titleIdx = 2
            var priorityIdx = 3
            var categoryIdx = 4
            var dateIdx = 5
            var reasonIdx = 6

            val headerLower = firstLine.map { it.trim().lowercase() }
            if (headerLower.any { it.contains("타입") || it.contains("type") || it.contains("내용") || it.contains("title") }) {
                startIndex = 1
                headerLower.forEachIndexed { index, col ->
                    when {
                        col.contains("id") -> idIdx = index
                        col.contains("타입") || col.contains("type") -> typeIdx = index
                        col.contains("내용") || col.contains("title") || col.contains("행동") -> titleIdx = index
                        col.contains("중요도") || col.contains("priority") -> priorityIdx = index
                        col.contains("카테고리") || col.contains("category") -> categoryIdx = index
                        col.contains("날짜") || col.contains("date") -> dateIdx = index
                        col.contains("이유") || col.contains("reason") || col.contains("비고") -> reasonIdx = index
                    }
                }
            }

            for (i in startIndex until allLines.size) {
                val record = allLines[i]
                if (record.isEmpty() || (record.size == 1 && record[0].isBlank())) continue

                val id = if (idIdx in record.indices) record[idIdx].toLongOrNull() ?: 0L else 0L
                val typeStr = if (typeIdx in record.indices) record[typeIdx].trim() else ""
                val title = if (titleIdx in record.indices) record[titleIdx].trim() else ""
                val priority = if (priorityIdx in record.indices) {
                    record[priorityIdx].toIntOrNull()?.coerceIn(1, 3) ?: 1
                } else 1
                val category = if (categoryIdx in record.indices) record[categoryIdx].trim() else "일반"
                val date = if (dateIdx in record.indices) record[dateIdx].trim() else ""
                val reason = if (reasonIdx in record.indices) record[reasonIdx].trim() else ""

                if (title.isBlank()) continue

                val ruleType = when {
                    typeStr.contains("안 되는") || typeStr.contains("안되는") || typeStr.equals("dont", true) || typeStr.contains("금기") -> RuleType.DONT
                    typeStr.contains("물어볼") || typeStr.equals("ask", true) || typeStr.contains("확인") -> RuleType.ASK
                    else -> RuleType.DO
                }

                result.add(
                    RuleItem(
                        id = id,
                        type = ruleType,
                        title = title,
                        priority = priority,
                        category = category.ifBlank { "일반" },
                        date = date,
                        reason = reason
                    )
                )
            }
        }
        return result
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Parses RFC 4180 CSV records, correctly handling quotes and multiline cells.
     */
    private fun readAllCsvRecords(reader: BufferedReader): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentField = StringBuilder()
        val currentRecord = mutableListOf<String>()
        var inQuotes = false

        var charCode: Int
        var prevChar: Char? = null

        while (reader.read().also { charCode = it } != -1) {
            val c = charCode.toChar()

            if (inQuotes) {
                if (c == '"') {
                    // Check if next char is also quote (escaped quote)
                    reader.mark(1)
                    val nextCode = reader.read()
                    if (nextCode != -1 && nextCode.toChar() == '"') {
                        currentField.append('"')
                    } else {
                        reader.reset()
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRecord.add(currentField.toString())
                        currentField.clear()
                    }
                    '\r' -> {
                        // ignore or handle with \n
                    }
                    '\n' -> {
                        currentRecord.add(currentField.toString())
                        currentField.clear()
                        records.add(ArrayList(currentRecord))
                        currentRecord.clear()
                    }
                    else -> currentField.append(c)
                }
            }
            prevChar = c
        }

        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString())
            records.add(currentRecord)
        }

        return records
    }
}
