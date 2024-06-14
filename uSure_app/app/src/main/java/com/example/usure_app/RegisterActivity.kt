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

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameEditText = findViewById<EditText>(R.id.editTextText)
        val emailEditText = findViewById<EditText>(R.id.editTextTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextTextPassword)
        val repeatPasswordEditText = findViewById<EditText>(R.id.editTextTextRepeatPassword)
        val submitButton = findViewById<Button>(R.id.button)
        val loginTextView = findViewById<TextView>(R.id.textView3)

        submitButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val repeatPassword = repeatPasswordEditText.text.toString()
            val sharedPreferences = getSharedPreferences("uSurePrefs", Context.MODE_PRIVATE)
            val savedURL = sharedPreferences.getString("user_URL", null)
            if (password != repeatPassword) {
                Toast.makeText(this@RegisterActivity, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener  // Salir del listener sin hacer la solicitud
            }

            val json = JSONObject().apply {
                put("nombre", username)
                put("email", email)
                put("password", password)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$savedURL/Register")
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "Error en el registro", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: IOException) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@RegisterActivity, "Error en la solicitud", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        loginTextView.setOnClickListener {
            startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
        }
    }
}
