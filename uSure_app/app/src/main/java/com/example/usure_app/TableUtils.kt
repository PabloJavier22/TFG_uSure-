package com.example.usure_app

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TableUtils {




    fun obtenerTablasDeArchivo(context: Context, fileName: String): Pair<List<TableLayout>, List<String>> {
        var tablas: MutableList<TableLayout> = mutableListOf()
        var nombresTablas: MutableList<String> = mutableListOf()

        var file = File(context.filesDir, fileName)
        if (file.exists()) {
            val content = file.readText()
            val jsonArray = JSONArray(content)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val tableName = jsonObject.getString("tableName")
                val tableData = jsonObject.getJSONArray("tableData")

                val tableLayout = TableLayout(context)
                tableLayout.addView(crearFila(context, "Product", "Quantity"))

                for (j in 0 until tableData.length()) {
                    val row = tableData.getJSONObject(j)
                    val product = row.getString("product")
                    val quantity = row.getString("quantity")
                    tableLayout.addView(crearFila(context, product, quantity))
                }

                tablas.add(tableLayout)
                nombresTablas.add(tableName)
            }
        } else {
            val (sampleTables, sampleTableNames) = obtenerTablasDeEjemplo(context)
            guardarTablasEnArchivo(context, sampleTables, sampleTableNames, fileName)
            return obtenerTablasDeArchivo(context, fileName)
        }

        return Pair(tablas, nombresTablas)
    }

    fun obtenerTablasDeEjemplo(context: Context): Pair<List<TableLayout>, List<String>> {
        val tablas: MutableList<TableLayout> = mutableListOf()
        val nombresTablas: MutableList<String> = mutableListOf()

        val tabla1 = TableLayout(context).apply {
            addView(crearFila(context, "Product", "Quantity"))
            addView(crearFila(context, "Milk", "5"))
            addView(crearFila(context, "Bread", "3"))
        }
        tablas.add(tabla1)
        nombresTablas.add("Table 1")

        val tabla2 = TableLayout(context).apply {
            addView(crearFila(context, "Product", "Quantity"))
            addView(crearFila(context, "Eggs", "10"))
            addView(crearFila(context, "Cheese", "2"))
        }
        tablas.add(tabla2)
        nombresTablas.add("Table 2")

        val tabla3 = TableLayout(context).apply {
            addView(crearFila(context, "Product", "Quantity"))
            addView(crearFila(context, "Apples", "8"))
            addView(crearFila(context, "Oranges", "6"))
        }
        tablas.add(tabla3)
        nombresTablas.add("Table 3")

        return Pair(tablas, nombresTablas)
    }

    fun crearFila(context: Context, nombre: String, cantidad: String): TableRow {
        val inflater = LayoutInflater.from(context)
        val fila = inflater.inflate(R.layout.row_item, null) as TableRow
        val text1 = fila.findViewById<TextView>(R.id.text1)
        val text2 = fila.findViewById<TextView>(R.id.text2)

        text1.text = nombre
        text2.text = cantidad

        fila.contentDescription = "Row with product $nombre and quantity $cantidad"

        return fila
    }

    fun crearTabla(context: Context, onTableCreated: (TableLayout, String) -> Unit) {
        val inflater = LayoutInflater.from(context)
        val dialogView: View = inflater.inflate(R.layout.dialog_create_table, null)
        val editTextTableName: EditText = dialogView.findViewById(R.id.editTextTableName)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Create New Table")
            .setView(dialogView)
            .setPositiveButton("Create", null) // No establezcas un clic del botón positivo aquí
            .setNegativeButton("Cancel", null)
            .create()

        // Habilitar o deshabilitar el botón de "Create" según si se ha ingresado un nombre de tabla válido
        editTextTableName.doAfterTextChanged {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = it.toString().trim().isNotEmpty()
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val tableName = editTextTableName.text.toString().trim()
                if (tableName.isNotEmpty()) {
                    val newTable = TableLayout(context)
                    onTableCreated(newTable, tableName) // Llama a la función de devolución de llamada con la nueva tabla y el nombre
                    dialog.dismiss() // Cierra el diálogo después de llamar a la función de devolución de llamada
                } else {
                    Toast.makeText(context, "Please enter a table name", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }



    fun guardarTablasEnArchivo(context: Context, tables: List<TableLayout>, tableNames: List<String>, fileName: String) {
        val jsonArray = JSONArray()

        for (i in tables.indices) {
            val tableObject = JSONObject()
            tableObject.put("tableName", tableNames[i])

            val tableLayout = tables[i]
            val rowsArray = JSONArray()

            for (j in 1 until tableLayout.childCount) {
                val row = tableLayout.getChildAt(j) as TableRow
                val product = (row.getChildAt(0) as TextView).text.toString()
                val quantity = (row.getChildAt(1) as TextView).text.toString()

                val rowObject = JSONObject()
                rowObject.put("product", product)
                rowObject.put("quantity", quantity)

                rowsArray.put(rowObject)
            }

            tableObject.put("tableData", rowsArray)
            jsonArray.put(tableObject)
        }

        val file = File(context.filesDir, fileName)
        file.writeText(jsonArray.toString())
    }
}
