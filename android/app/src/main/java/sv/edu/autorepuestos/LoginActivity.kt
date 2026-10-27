package sv.edu.autorepuestos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Por favor completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (!task.isSuccessful) {
                        btnLogin.isEnabled = true

                        Toast.makeText(
                            this,
                            "Error de autenticación: " +
                                    (task.exception?.localizedMessage
                                        ?: "Error desconocido"),
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnCompleteListener
                    }

                    val firebaseUser = auth.currentUser

                    if (firebaseUser == null) {
                        btnLogin.isEnabled = true

                        Toast.makeText(
                            this,
                            "No se pudo obtener el usuario de Firebase",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnCompleteListener
                    }

                    val uid = firebaseUser.uid
                    val correo = firebaseUser.email ?: email

                    lifecycleScope.launch {
                        try {

                            val usuario = UsuarioRequest(
                                firebase_uid = uid,
                                nombre = firebaseUser.displayName
                                    ?: "Usuario AutoParts",
                                correo = correo,
                                rol = "cliente"
                            )

                            // 1. Sincronizar usuario con MySQL
                            val respuestaRegistro =
                                withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService
                                        .registrarUsuario(usuario)
                                }

                            if (!respuestaRegistro.isSuccessful) {
                                Log.e(
                                    "LOGIN_MYSQL",
                                    "Error al sincronizar: ${respuestaRegistro.code()}"
                                )

                                Toast.makeText(
                                    this@LoginActivity,
                                    "No se pudo sincronizar con MySQL. Código: ${respuestaRegistro.code()}",
                                    Toast.LENGTH_LONG
                                ).show()

                                return@launch
                            }

                            // 2. Consultar rol en MySQL
                            val respuestaRol =
                                withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService
                                        .obtenerRolUsuario(uid)
                                }

                            if (!respuestaRol.isSuccessful) {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "No se pudo consultar el rol. Código: ${respuestaRol.code()}",
                                    Toast.LENGTH_LONG
                                ).show()

                                return@launch
                            }

                            val rol = respuestaRol.body()
                                ?.rol
                                ?.trim()
                                ?.lowercase()

                            // 3. Redirigir según el rol
                            val destino = when (rol) {
                                "cliente" -> MainActivity::class.java
                                "admin", "administrador" ->
                                    AdminActivity::class.java

                                else -> {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Rol de usuario no válido",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return@launch
                                }
                            }

                            Toast.makeText(
                                this@LoginActivity,
                                "¡Bienvenido a AutoParts!",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(
                                this@LoginActivity,
                                destino
                            )

                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK

                            startActivity(intent)
                            finish()

                        } catch (e: Exception) {

                            Log.e(
                                "LOGIN_MYSQL",
                                "Error al conectar con el backend",
                                e
                            )

                            Toast.makeText(
                                this@LoginActivity,
                                "Error de conexión con el servidor: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()

                        } finally {
                            btnLogin.isEnabled = true
                        }
                    }
                }
        }

        tvRegister.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }
}