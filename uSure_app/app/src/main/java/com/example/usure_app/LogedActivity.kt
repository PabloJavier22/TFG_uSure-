package com.example.usure_app

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar

class LogedActivity : ComponentActivity() {

    private val fileName = "tables_data.txt"
    private lateinit var viewPager: ViewPager2
    private lateinit var tableNameTextView: TextView
    private lateinit var tables: MutableList<TableLayout>
    private lateinit var tableNames: MutableList<String>
    private lateinit var adapter: TabCarouselAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loged)

        viewPager = findViewById(R.id.viewPager2)
        tableNameTextView = findViewById(R.id.textTableName)

        val (loadedTables, loadedTableNames) = TableUtils.obtenerTablasDeArchivo(this, fileName)
        tables = loadedTables.toMutableList()
        tableNames = loadedTableNames.toMutableList()

        val button = findViewById<Button>(R.id.button)
        val hiddenButton = findViewById<Button>(R.id.hiddenButton)
        val hiddenButton2 = findViewById<Button>(R.id.hiddenButton2)
        val hiddenButton3 = findViewById<Button>(R.id.hiddenButton3)

        adapter = TabCarouselAdapter(tables, tableNames)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tableNameTextView.text = tableNames[position]
                refreshTable(position)
            }
        })

        setupButtonAnimations(button, hiddenButton, hiddenButton2, hiddenButton3)
    }

    private fun setupButtonAnimations(
        button: Button,
        hiddenButton: Button,
        hiddenButton2: Button,
        hiddenButton3: Button
    ) {
        var isRotated = false
        var isHiddenButtonVisible = false
        var isAnimationRunning = false

        button.setOnClickListener {
            if (isAnimationRunning) return@setOnClickListener

            val rotateAnimation = if (isRotated) {
                ObjectAnimator.ofFloat(button, "rotation", 45f, 0f)
            } else {
                ObjectAnimator.ofFloat(button, "rotation", 0f, 45f)
            }
            rotateAnimation.duration = 500
            rotateAnimation.interpolator = AccelerateDecelerateInterpolator()

            rotateAnimation.start()

            if (!isHiddenButtonVisible) {
                showHiddenButtons(hiddenButton, hiddenButton2, hiddenButton3)
            } else {
                hideHiddenButtons(hiddenButton, hiddenButton2, hiddenButton3)
            }

            isHiddenButtonVisible = !isHiddenButtonVisible
            isRotated = !isRotated
        }

        hiddenButton.setOnClickListener {
            val currentItem = viewPager.currentItem
            val currentTable = tables[currentItem]

            showAddProductDialog(currentTable, tableNames[currentItem], currentItem, tables, tableNames)
        }

        hiddenButton2.setOnClickListener {
            val currentItem = viewPager.currentItem
            showDeleteTableDialog(currentItem)
        }

        hiddenButton3.setOnClickListener {
            createNewTable()
        }
    }

    private fun showHiddenButtons(hiddenButton: Button, hiddenButton2: Button, hiddenButton3: Button) {
        hiddenButton.visibility = View.VISIBLE
        hiddenButton2.visibility = View.VISIBLE
        hiddenButton3.visibility = View.VISIBLE
        hiddenButton.animate().translationYBy(-350f).setDuration(500).start()
        hiddenButton2.animate().translationYBy(-175f).translationXBy(350f).setDuration(500).start()
        hiddenButton3.animate().translationYBy(-175f).translationXBy(-350f).setDuration(500).start()
    }

    private fun hideHiddenButtons(hiddenButton: Button, hiddenButton2: Button, hiddenButton3: Button) {
        hiddenButton.animate().translationY(0f).setDuration(500).withEndAction { hiddenButton.visibility = View.INVISIBLE }.start()
        hiddenButton2.animate().translationY(0f).translationX(0f).setDuration(500).withEndAction { hiddenButton2.visibility = View.INVISIBLE }.start()
        hiddenButton3.animate().translationY(0f).translationX(0f).setDuration(500).withEndAction { hiddenButton3.visibility = View.INVISIBLE }.start()
    }

    private fun showAddProductDialog(
        currentTable: TableLayout,
        tableName: String,
        currentItem: Int,
        tables: List<TableLayout>,
        tableNames: List<String>
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_product, null)
        val editTextProduct = dialogView.findViewById<EditText>(R.id.editTextProduct)
        val editTextQuantity = dialogView.findViewById<EditText>(R.id.editTextQuantity)

        AlertDialog.Builder(this)
            .setTitle("Add New Product")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val product = editTextProduct.text.toString()
                val quantity = editTextQuantity.text.toString().toIntOrNull()
                //Revisa si has rellenado los datos correctamente
                if (product.isNotBlank() && quantity != null) {
                    var productExists = false
                    //Elimina espacios y mayusculas
                    val normalizedProduct = normalizeString(product)
                    for (i in 0 until currentTable.childCount) {
                        val row = currentTable.getChildAt(i) as TableRow
                        val productNameTextView = row.getChildAt(0) as TextView
                        val productQuantityTextView = row.getChildAt(1) as TextView

                        // Comparara el nombre normalizado del producto con el nombre normalizado en cada fila de la tabla
                        if (normalizeString(productNameTextView.text.toString()) == normalizedProduct) {
                            // Si el producto ya existe, actualizar la cantidad sumando la cantidad ingresada por el usuario
                            val currentQuantity = productQuantityTextView.text.toString().toInt()
                            productQuantityTextView.text = (currentQuantity + quantity).toString()
                            productExists = true
                            break
                        }
                    }
                    //Si el producto no existe lo crea y tambie la nueva fila
                    if (!productExists) {
                        val newRow = TableUtils.crearFila(this, product, quantity.toString())
                        currentTable.addView(newRow)
                    }
                    //Guardamos los datos en el archivo local
                    TableUtils.guardarTablasEnArchivo(this, tables, tableNames, fileName)
                    //Refrescamos la tabla acutal
                    refreshTable(currentItem)
                    //Mensaje de confirmación
                    Snackbar.make(findViewById(android.R.id.content), "Product added/updated", Snackbar.LENGTH_SHORT).show()
                } else {
                    //Mensaje de que no has rellenado correctamente el formulario
                    Snackbar.make(findViewById(android.R.id.content), "Please fill all fields correctly", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //Confirmacion de si quieres borrar la tabla
    private fun showDeleteTableDialog(currentItem: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Table")
            .setMessage("Are you sure you want to delete this table?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTable(currentItem)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //Función para borrar la tabla en la que te encuentras actualmente
    private fun deleteTable(currentItem: Int) {
        if (currentItem < tables.size) {
            tables.removeAt(currentItem)
            tableNames.removeAt(currentItem)
            //Guarda los cambios en el archivo local
            TableUtils.guardarTablasEnArchivo(this, tables, tableNames, fileName)
            adapter.updateTables(tables, tableNames)
            adapter.notifyDataSetChanged()
            Snackbar.make(findViewById(android.R.id.content), "Table deleted", Snackbar.LENGTH_SHORT).show()
        }
    }
    //Funcion para crear una tabla
    private fun createNewTable() {
        TableUtils.crearTabla(this) { newTable, providedName ->
            // Agrega la nueva tabla y su nombre a las listas existentes
            tables.add(newTable)
            tableNames.add(providedName)

            // Guarda las tablas actualizadas en el archivo
            TableUtils.guardarTablasEnArchivo(this, tables, tableNames, fileName)

            // Actualiza el adaptador y la interfaz de usuario
            adapter.updateTables(tables, tableNames)
            adapter.notifyDataSetChanged()
             // Desplazamos a la tabla recien creada
        }
    }


    //Función para eliminar las mayusculas y los espacios
    private fun normalizeString(input: String): String {
        return input.trim().replace("\\s".toRegex(), "").lowercase()
    }

    //Refresca las tablas lo utilizo al cambiar de tabla, nuevo item, o borrar/crear nueva tabla
    private fun refreshTable(position: Int) {
        // Obtener las tablas actualizadas y los nombres de tabla del archivo local
        val (updatedTables, updatedTableNames) = TableUtils.obtenerTablasDeArchivo(this, fileName)

        // Actualizar las listas de tablas y nombres de tabla con los datos obtenidos del archivo
        tables = updatedTables.toMutableList()
        tableNames = updatedTableNames.toMutableList()

        // Actualizar el adaptador con las tablas y nombres de tabla actualizados
        adapter.updateTables(tables, tableNames)

        // Notificar al adaptador que los datos han cambiado para que actualice la interfaz de usuario
        adapter.notifyDataSetChanged()
    }

}
