package com.example.usure_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val url = "http://192.168.1.153:5169/uSure"
        saveURL(url)
        val usernameEditText = findViewById<EditText>(R.id.loginUsername)
        val passwordEditText = findViewById<EditText>(R.id.loginPassword)
        val submitButton = findViewById<Button>(R.id.button)
        val noConexionButton = findViewById<Button>(R.id.noConexionButton)
        val registerTextView = findViewById<TextView>(R.id.textView3)

        noConexionButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, LogedActivityLocal::class.java))
        }

        submitButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json = JSONObject().apply {
                put("nombre", username)
                put("password", password)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$url/Login")
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    launch(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val jsonResponse = JSONObject(responseBody)
                            val token = jsonResponse.getString("token")
                            saveToken(token)
                            Toast.makeText(this@MainActivity, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MainActivity, LogedActivity::class.java))
                        } else {
                            Toast.makeText(this@MainActivity, "Error en el inicio de sesión: $responseBody", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: IOException) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error en la solicitud: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        registerTextView.setOnClickListener {
            startActivity(Intent(this@MainActivity, RegisterActivity::class.java))
        }
    }

    private fun saveToken(token: String) {
        val sharedPreferences = getSharedPreferences("uSurePrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("user_token", token)
            apply()
        }

        // Mostrar el token guardado en un Toast para verificación (objetivo solo para debugear borrar mas tarde)!!
        val savedToken = sharedPreferences.getString("user_token", null)
       // Toast.makeText(this, "Token guardado: $savedToken", Toast.LENGTH_LONG).show()
    }

    private fun saveURL(url: String) {
        val sharedPreferences = getSharedPreferences("uSurePrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("user_URL", url)
            apply()
        }
    }

}
