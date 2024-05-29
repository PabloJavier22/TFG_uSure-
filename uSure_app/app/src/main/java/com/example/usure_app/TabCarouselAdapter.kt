package com.example.usure_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

//Aqui lo que tenemos es un adaptador para nuestro ReciclerView, ReciclerView es la vista de la lista
class TabCarouselAdapter(
    private var tables: List<TableLayout>, //Lista de Tablas
    private var tableNames: List<String> //Lista de nombres de Tablas
) : RecyclerView.Adapter<TabCarouselAdapter.TabViewHolder>() {

    inner class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tableLayout: TableLayout = itemView.findViewById(R.id.tableLayout) //Aquí recogemos el diseño de las tablas
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tab_item_layout, parent, false)
        return TabViewHolder(view)
    }
    fun updateTables(newTables: List<TableLayout>, newTableNames: List<String>) {
        tables = newTables
        tableNames = newTableNames
    }
    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val table = tables[position]
        holder.tableLayout.removeAllViews()
        for (i in 0 until table.childCount) {
            val row = table.getChildAt(i) as TableRow

            val newRow = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.row_item, holder.tableLayout, false) as TableRow

            val text1 = newRow.findViewById<TextView>(R.id.text1)
            val text2 = newRow.findViewById<TextView>(R.id.text2)
            text1.text = (row.getChildAt(0) as TextView).text
            text2.text = (row.getChildAt(1) as TextView).text

            holder.tableLayout.addView(newRow)

            newRow.setOnClickListener { view ->
                showPopupMenu(view, position, i)
            }
        }
    }

    override fun getItemCount(): Int {
        return tables.size
    }

    private fun showPopupMenu(view: View, tablePosition: Int, rowPosition: Int) {
        val popup = PopupMenu(view.context, view)
        popup.inflate(R.menu.popup_menu)
        popup.setOnMenuItemClickListener { menuItem ->
            val table = tables[tablePosition]
            val row = table.getChildAt(rowPosition) as TableRow
            val text2 = row.findViewById<TextView>(R.id.text2)

            try {
                when (menuItem.itemId) {
                    R.id.action_sumar -> {
                        val cantidad = text2.text.toString().toInt()
                        text2.text = (cantidad + 1).toString()
                        row.contentDescription = "Row with product ${(row.getChildAt(0) as TextView).text} and quantity ${text2.text}"
                        Snackbar.make(view, "Added one unit. New quantity: ${text2.text}", Snackbar.LENGTH_SHORT).show()
                        TableUtils.guardarTablasEnArchivo(view.context, tables, tableNames, "tables_data.txt")
                        notifyDataSetChanged()
                        true
                    }
                    R.id.action_restar -> {
                        val cantidad = text2.text.toString().toInt()
                        if (cantidad > 0) {
                            text2.text = (cantidad - 1).toString()
                            row.contentDescription = "Row with product ${(row.getChildAt(0) as TextView).text} and quantity ${text2.text}"
                            Snackbar.make(view, "Removed one unit. New quantity: ${text2.text}", Snackbar.LENGTH_SHORT).show()
                            TableUtils.guardarTablasEnArchivo(view.context, tables, tableNames, "tables_data.txt")
                            notifyDataSetChanged()
                        } else {
                            Snackbar.make(view, "Quantity cannot be less than zero", Snackbar.LENGTH_SHORT).show()
                        }
                        true
                    }
                    R.id.action_eliminar -> {
                        table.removeViewAt(rowPosition)
                        TableUtils.guardarTablasEnArchivo(view.context, tables, tableNames, "tables_data.txt")
                        notifyDataSetChanged()
                        Snackbar.make(view, "Product removed", Snackbar.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                Snackbar.make(view, "No puedes modificar la cabecera", Snackbar.LENGTH_SHORT).show()
                false
            }
        }
        popup.show()
    }
}
