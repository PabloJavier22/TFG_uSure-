package com.example.usure_app

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class LogedActivityLocal : ComponentActivity() {

    private val fileName = "tables_data.txt"
    private lateinit var viewPager: ViewPager2
    private lateinit var tableNameTextView: TextView
    private lateinit var tables: MutableList<TableLayout>
    private lateinit var tableNames: MutableList<String>
    private lateinit var adapter: TabCarouselAdapter
    private lateinit var groupsSpinner: Spinner
    private lateinit var groupIds: List<String>
    private lateinit var tableIds: List<String>
    private var actualTable = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loged)
        initViewComponents()

        val sharedPreferences = getSharedPreferences("uSurePrefs", Context.MODE_PRIVATE)
        val savedURL = sharedPreferences.getString("user_URL", null)
        val token = sharedPreferences.getString("user_token", null)

        if (token != null && savedURL != null) {
            fetchGroups(savedURL, token)
        } else {
            Toast.makeText(this, "Algo ha salido mal!!", Toast.LENGTH_SHORT).show()
        }

        setupViewPager()
        setupSearchBar()
        setupGroupsSpinner(savedURL, token)
    }

    private fun initViewComponents() {
        viewPager = findViewById(R.id.viewPager2)
        tableNameTextView = findViewById(R.id.textTableName)
        groupsSpinner = findViewById(R.id.groupsSpinner)
        val button = findViewById<Button>(R.id.button)
        val hiddenButton = findViewById<Button>(R.id.hiddenButton)
        val hiddenButton2 = findViewById<Button>(R.id.hiddenButton2)
        val hiddenButton3 = findViewById<Button>(R.id.hiddenButton3)
        val searchBar = findViewById<EditText>(R.id.search_bar)
        setupButtonAnimations(button, hiddenButton, hiddenButton2, hiddenButton3)
        val (loadedTables, loadedTableNames) = TableUtils.obtenerTablasDeArchivo(this, fileName)

        tables = loadedTables.toMutableList()
        tableNames = loadedTableNames.toMutableList()
        adapter = TabCarouselAdapter(tables, tableNames)
        viewPager.adapter = adapter
    }

    private fun setupViewPager() {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tableNameTextView.text = tableNames[position]
                actualTable = position
                //refreshTable(position)
            }
        })
    }

    private fun setupSearchBar() {
        val searchBar = findViewById<EditText>(R.id.search_bar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTables(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupGroupsSpinner(savedURL: String?, token: String?) {
        groupsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (groupIds.isNotEmpty()) {
                    val selectedGroup = parent?.getItemAtPosition(position).toString()
                    val selectedGroupId = groupIds[position]
                    tableNameTextView.text = tableNames[actualTable]
                    if (token != null && savedURL != null) {
                        loadTablesForGroup(this@LogedActivityLocal, savedURL, token, selectedGroup)
                    } else {
                        Toast.makeText(this@LogedActivityLocal, "No se encontró el token o la URL", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LogedActivityLocal, "No hay grupos disponibles.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Manejar el caso en el que no se haya seleccionado ningún grupo
            }
        }
    }

    private fun fetchGroups(savedURL: String, token: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$savedURL/UserGroups")
            .header("Authorization", "Bearer $token")
            .build()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                launch(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        val groupNames = parseGroupNames(responseBody)
                        groupIds = parseGroupID(responseBody)
                        updateSpinner(groupNames)
                    } else {
                        Toast.makeText(this@LogedActivityLocal, "Hubo un problema con los grupos, pruebe de nuevo más tarde", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@LogedActivityLocal, "Error en la solicitud: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun loadTablesForGroup(context: Context, savedURL: String, token: String, groupName: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$savedURL/Groups/$groupName/Tables")
            .header("Authorization", "Bearer $token")
            .build()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                launch(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        val tableNames = parseTableNames(responseBody)
                        tableIds = parseTableIds(responseBody)
                        loadTableContents(context, savedURL, token, groupName, tableNames)
                    } else {
                        Toast.makeText(context, "Error en la solicitud: $responseBody", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Error en la solicitud: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun loadTableContents(context: Context, savedURL: String, token: String, groupName: String, tableNames: List<String>) {
        val client = OkHttpClient()

        tables.clear()
        this.tableNames.clear()
        this.tableNames.addAll(tableNames)

        for (tableName in tableNames) {
            val request = Request.Builder()
                .url("$savedURL/Groups/$groupName/Tables/$tableName/ProductList")
                .header("Authorization", "Bearer $token")
                .build()

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    launch(Dispatchers.Main) {
                        if (response.isSuccessful && responseBody != null) {
                            val updatedTables = parseProductList(context, responseBody)
                            tables.add(updatedTables)
                            adapter.updateTables(tables, this@LogedActivityLocal.tableNames)
                            adapter.notifyDataSetChanged()
                        } else {
                            Toast.makeText(context, "Error en la solicitud: ${response.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: IOException) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Error en la solicitud: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun parseGroupNames(responseBody: String): List<String> {
        val groupNames = mutableListOf<String>()
        val jsonResponse = JSONObject(responseBody)
        val valuesArray: JSONArray = jsonResponse.getJSONArray("\$values")
        for (i in 0 until valuesArray.length()) {
            val group = valuesArray.getJSONObject(i)
            groupNames.add(group.getString("Nombre"))
        }
        return groupNames
    }

    private fun parseGroupID(responseBody: String): List<String> {
        val groupID = mutableListOf<String>()
        val jsonResponse = JSONObject(responseBody)
        val valuesArray: JSONArray = jsonResponse.getJSONArray("\$values")
        for (i in 0 until valuesArray.length()) {
            val group = valuesArray.getJSONObject(i)
            groupID.add(group.getString("ID"))
        }
        return groupID
    }

    private fun parseTableNames(responseBody: String): List<String> {
        val tableNames = mutableListOf<String>()
        val jsonResponse = JSONObject(responseBody)
        val valuesArray: JSONArray = jsonResponse.getJSONArray("\$values")
        for (i in 0 until valuesArray.length()) {
            val table = valuesArray.getJSONObject(i)
            tableNames.add(table.getString("Nombre"))
        }
        return tableNames
    }

    private fun parseTableIds(responseBody: String): List<String> {
        val tableIds = mutableListOf<String>()
        val jsonResponse = JSONObject(responseBody)
        val valuesArray: JSONArray = jsonResponse.getJSONArray("\$values")
        for (i in 0 until valuesArray.length()) {
            val table = valuesArray.getJSONObject(i)
            tableIds.add(table.getString("ID"))
        }
        return tableIds
    }

    private fun parseProductList(context: Context, responseBody: String): TableLayout {
        val tableLayout = TableLayout(context)
        tableLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        tableLayout.orientation = TableLayout.VERTICAL

        try {
            val jsonResponse = JSONObject(responseBody)
            val valuesArray: JSONArray = jsonResponse.getJSONArray("\$values")

            val headerRow = TableRow(context)
            val headers = listOf("Nombre", "Cantidad", "Categoría", "Grupo")

            headers.forEach { header ->
                val headerTextView = TextView(context)
                headerTextView.text = header
                headerTextView.setPadding(16, 16, 16, 16)
                headerRow.addView(headerTextView)
            }
            tableLayout.addView(headerRow)

            for (i in 0 until valuesArray.length()) {
                val row = TableRow(context)
                val product = valuesArray.getJSONObject(i)

                val nombreTextView = TextView(context)
                nombreTextView.text = product.getString("nombre")
                nombreTextView.setPadding(16, 16, 16, 16)
                row.addView(nombreTextView)

                val cantidadTextView = TextView(context)
                cantidadTextView.text = product.getInt("cantidad").toString()
                cantidadTextView.setPadding(16, 16, 16, 16)
                row.addView(cantidadTextView)

                val categoriaTextView = TextView(context)
                categoriaTextView.text = product.getString("idCategoria")
                categoriaTextView.setPadding(16, 16, 16, 16)
                row.addView(categoriaTextView)

                // Para "Grupo", necesitas manejar cómo obtienes este dato desde tu JSON, ya que no está presente en el ejemplo proporcionado.

                tableLayout.addView(row)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return tableLayout
    }


    private fun updateSpinner(groups: List<String>) {
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groups)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        groupsSpinner.adapter = spinnerAdapter
    }

// <--tableRelatedFunctions-->

    private fun deleteTable(currentItem: Int) {
        if (tables.size > 1 && currentItem < tables.size) {
            val tableIDToDelete = tableIds[currentItem]

            // Eliminar la tabla de la lista local
            tables.removeAt(currentItem)
            tableNames.removeAt(currentItem)

            // Guardar los cambios en el archivo local
            TableUtils.guardarTablasEnArchivo(this, tables, tableNames, fileName)
            adapter.updateTables(tables, tableNames)
            adapter.notifyDataSetChanged()

            // Mostrar un Snackbar
            Snackbar.make(findViewById(android.R.id.content), "Table deleted", Snackbar.LENGTH_SHORT).show()

            // Llamar a la función para eliminar en el servidor
            deleteFromServer(tableIDToDelete)
        } else {
            // Mostrar un mensaje indicando que no se puede eliminar la última tabla
            Snackbar.make(findViewById(android.R.id.content), "Cannot delete last table", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun updateGroupsSpinner(savedURL: String?, token: String?) {


        // Notificar al Adapter del Spinner
        (groupsSpinner.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    private fun deleteFromServer(tableID: String) {
        val sharedPreferences = getSharedPreferences("uSurePrefs", Context.MODE_PRIVATE)
        val savedURL = sharedPreferences.getString("user_URL", null)
        val token = sharedPreferences.getString("user_token", null)

        if (savedURL != null && token != null) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$savedURL/DeleteTable/$tableID") // Aquí se usa el nombre de la tabla como ID, ajusta según sea necesario
                .delete()
                .header("Authorization", "Bearer $token")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@LogedActivityLocal, "Error deleting table: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@LogedActivityLocal, "Table deleted successfully", Toast.LENGTH_SHORT).show()
                            updateGroupsSpinner(savedURL, token)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@LogedActivityLocal, "Error deleting table: ${response.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        } else {
            Toast.makeText(this, "No se encontró el token o la URL", Toast.LENGTH_SHORT).show()
        }
    }


    private fun createNewTable() {
        TableUtils.crearTabla(this) { newTable, providedName ->
            val sharedPreferences = getSharedPreferences("uSurePrefs", Context.MODE_PRIVATE)
            val savedURL = sharedPreferences.getString("user_URL", null)
            val token = sharedPreferences.getString("user_token", null)

            // Add the new table and its name to the existing lists
            tables.add(newTable)
            tableNames.add(providedName)

            // Save the updated tables to the file
            TableUtils.guardarTablasEnArchivo(this, tables, tableNames, fileName)

            // Update the adapter and the UI
            adapter.updateTables(tables, tableNames)
            adapter.notifyDataSetChanged()

            // Move to the newly created table
            viewPager.setCurrentItem(tables.size - 1, true)

            // Get the currently selected group ID
            val selectedPosition = groupsSpinner.selectedItemPosition
            if (selectedPosition >= 0 && selectedPosition < groupIds.size) {
                val selectedGroupId = groupIds[selectedPosition]
                // Send the create table request with the provided name and selected group ID
                sendCreateTableRequest(providedName, selectedGroupId)
                setupGroupsSpinner(savedURL, token)


            } else {
                Toast.makeText(this, "Error: Grupo seleccionado no válido", Toast.LENGTH_SHORT).show()
            }
        }


    }

    // Función para enviar la solicitud HTTP a la URL específica
    private fun sendCreateTableRequest(nombre: String, idGrupo: String) {
        val sharedPreferences = getSharedPreferences("uSurePrefs", Context.MODE_PRIVATE)
        val savedURL = sharedPreferences.getString("user_URL", null)
        val url = "$savedURL/CreateTable"

        // Datos a enviar en el cuerpo de la solicitud
        val jsonBody = JSONObject()
        jsonBody.put("nombre", nombre)
        jsonBody.put("idGrupo", idGrupo)

        val requestBody = jsonBody.toString()

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody))
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Manejar la falla de la petición aquí
                e.printStackTrace()
                Log.e("HTTP Request", "Failed to execute request: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                // Manejar la respuesta de la petición aquí
                response.use {
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code $response")
                    }
                    // Procesar la respuesta si es necesario
                    val responseData = response.body?.string()
                    Log.d("HTTP Request", "Response: $responseData")
                }
            }
        })
    }
    //Función para eliminar las mayusculas y los espacios
    private fun normalizeString(input: String): String {
        return input.trim().replace("\\s".toRegex(), "").lowercase()
    }

    //Refresca las tablas lo utilizo al cambiar de tabla, nuevo item, o borrar/crear nueva tabla
    /** private fun refreshTable(position: Int) {
    // Obtener las tablas actualizadas y los nombres de tabla del archivo local
    val (updatedTables, updatedTableNames) = TableUtils.obtenerTablasDeArchivo(this, fileName)
    // Actualizar las listas de tablas y nombres de tabla con los datos obtenidos del archivo
    tables = updatedTables.toMutableList()
    tableNames = updatedTableNames.toMutableList()

    // Actualizar el adaptador con las tablas y nombres de tabla actualizados
    adapter.updateTables(tables, tableNames)

    // Notificar al adaptador que los datos han cambiado para que actualice la interfaz de usuario
    adapter.notifyDataSetChanged()
    }**/

    // <--filteredSearch-->
    private fun filterTables(query: String) {
        val filteredTables = tables.map { table ->
            val filteredTable = TableLayout(this)
            // Add the header row to the filtered table if it exists
            if (table.childCount > 0) {
                val headerRow = table.getChildAt(0) as TableRow
                val newHeaderRow = TableUtils.crearFila(this, (headerRow.getChildAt(0) as TextView).text.toString(), (headerRow.getChildAt(1) as TextView).text.toString())
                filteredTable.addView(newHeaderRow)
            }
            // Filter and add the matching rows
            for (i in 1 until table.childCount) {
                val row = table.getChildAt(i) as TableRow
                val productNameTextView = row.getChildAt(0) as TextView
                if (productNameTextView.text.toString().contains(query, ignoreCase = true)) {
                    // Create a new row instead of reusing the existing one
                    val newRow = TableUtils.crearFila(this, productNameTextView.text.toString(), (row.getChildAt(1) as TextView).text.toString())
                    filteredTable.addView(newRow)
                }
            }
            filteredTable
        }
        adapter.updateTables(filteredTables, tableNames)
        adapter.notifyDataSetChanged()
    }


    // <--Animations-->
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

            showCreateProductDialog()
        }

        hiddenButton2.setOnClickListener {
            val currentItem = viewPager.currentItem
            showDeleteTableDialog(currentItem)
        }

        hiddenButton3.setOnClickListener {
            createNewTable()
            loadTablesForGroup(current)
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

    // <--Dialogs-->
    private fun showCreateProductDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_product, null)
        val nombreEditText = dialogView.findViewById<EditText>(R.id.editTextProduct)
        val cantidadEditText = dialogView.findViewById<EditText>(R.id.editTextQuantity)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Crear Producto")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = nombreEditText.text.toString()
                val cantidad = cantidadEditText.text.toString().toIntOrNull() ?: 0
                val idCategoria = tableIds[actualTable]  // Obtener ID de la tabla actual
                val idGrupo = groupIds[groupsSpinner.selectedItemPosition]  // Obtener ID del grupo seleccionado

                createProduct(nombre, cantidad, idCategoria, idGrupo)
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun createProduct(nombre: String, cantidad: Int, idCategoria: String, idGrupo: String) {
        val sharedPreferences = getSharedPreferences("uSurePrefs", Context.MODE_PRIVATE)
        val savedURL = sharedPreferences.getString("user_URL", null)
        val token = sharedPreferences.getString("user_token", null)

        if (savedURL != null && token != null) {
            val url = "$savedURL/createProduct"

            // Crear el cuerpo de la solicitud
            val grupoProductosArray = JSONArray()
            val grupoProductosObject = JSONObject().put("idGrupo", idGrupo)
            grupoProductosArray.put(grupoProductosObject)

            val jsonBody = JSONObject()
            jsonBody.put("nombre", nombre)
            jsonBody.put("cantidad", cantidad)
            jsonBody.put("idCategoria", idCategoria)
            jsonBody.put("grupoProductos", grupoProductosArray)

            // Aquí agregamos un registro del contenido del cuerpo JSON
            Log.d("REQUEST_BODY", "JSON Body: $jsonBody")

            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Authorization", "Bearer $token")
                .build()

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@LogedActivityLocal, "Error en la solicitud: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@LogedActivityLocal, "Producto creado exitosamente", Toast.LENGTH_SHORT).show()
                            setupGroupsSpinner(savedURL, token)
                        } else {
                            Toast.makeText(this@LogedActivityLocal, "Error en la solicitud: ${response.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        } else {
            Toast.makeText(this, "No se encontró el token o la URL", Toast.LENGTH_SHORT).show()
        }
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
}