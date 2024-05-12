package com.example.usure_app

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

            val json = JSONObject().apply {
                put("nombre", username)
                put("email", email)
                put("password", password)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("http://192.168.178.45:5169/uSure/Register")
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, LogedActivity::class.java))
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
