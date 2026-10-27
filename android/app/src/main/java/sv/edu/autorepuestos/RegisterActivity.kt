package sv.edu.autorepuestos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button

    private var registrando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        etName = findViewById(R.id.etRegisterName)
        etEmail = findViewById(R.id.etRegisterEmail)
        etPassword = findViewById(R.id.etRegisterPassword)
        etConfirmPassword =
            findViewById(R.id.etRegisterConfirmPassword)

        btnRegister = findViewById(R.id.btnRegisterUser)

        val tvBackToLogin =
            findViewById<TextView>(R.id.tvBackToLogin)

        // Volver al inicio de sesión
        tvBackToLogin.setOnClickListener {
            if (!registrando) {
                finish()
            }
        }

        // Registrar usuario
        btnRegister.setOnClickListener {
            validarYRegistrar()
        }
    }

    private fun validarYRegistrar() {

        if (registrando) return

        val nombre = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword =
            etConfirmPassword.text.toString()

        // Limpiar errores anteriores
        etName.error = null
        etEmail.error = null
        etPassword.error = null
        etConfirmPassword.error = null

        // Validar nombre
        if (nombre.isBlank()) {
            etName.error = "Ingresa tu nombre"
            etName.requestFocus()
            return
        }

        if (nombre.length > 100) {
            etName.error = "El nombre es demasiado largo"
            etName.requestFocus()
            return
        }

        // Validar correo
        if (email.isBlank()) {
            etEmail.error = "Ingresa tu correo electrónico"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ingresa un correo válido"
            etEmail.requestFocus()
            return
        }

        // Validar contraseña
        if (password.isEmpty()) {
            etPassword.error = "Ingresa una contraseña"
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error =
                "La contraseña debe tener al menos 6 caracteres"
            etPassword.requestFocus()
            return
        }

        // Confirmar contraseña
        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error =
                "Confirma tu contraseña"
            etConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error =
                "Las contraseñas no coinciden"
            etConfirmPassword.requestFocus()
            return
        }

        registrando = true
        btnRegister.isEnabled = false
        btnRegister.text = "CREANDO CUENTA..."

        // Crear cuenta en Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (!task.isSuccessful) {

                    mostrarErrorFirebase(task.exception)
                    habilitarRegistro()
                    return@addOnCompleteListener
                }

                val firebaseUser = auth.currentUser

                if (firebaseUser == null) {

                    mostrarMensaje(
                        "No se pudo obtener el usuario de Firebase"
                    )

                    habilitarRegistro()
                    return@addOnCompleteListener
                }

                // Guardar el nombre en Firebase
                val profileUpdates =
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(nombre)
                        .build()

                firebaseUser.updateProfile(profileUpdates)
                    .addOnCompleteListener { profileTask ->

                        if (!profileTask.isSuccessful) {
                            Log.w(
                                "REGISTRO",
                                "No se pudo actualizar el nombre",
                                profileTask.exception
                            )
                        }

                        // Sincronizar con MySQL
                        lifecycleScope.launch {

                            try {

                                val usuario = UsuarioRequest(
                                    firebase_uid = firebaseUser.uid,
                                    nombre = nombre,
                                    correo = email,
                                    rol = "cliente"
                                )

                                val response =
                                    RetrofitClient.apiService
                                        .registrarUsuario(usuario)

                                if (response.isSuccessful) {

                                    mostrarMensaje(
                                        "¡Cuenta creada correctamente!"
                                    )

                                    val intent = Intent(
                                        this@RegisterActivity,
                                        MainActivity::class.java
                                    ).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }

                                    startActivity(intent)
                                    finish()

                                } else {

                                    Log.e(
                                        "REGISTRO",
                                        "Error HTTP ${response.code()}"
                                    )

                                    mostrarMensaje(
                                        "La cuenta se creó en Firebase, " +
                                                "pero no se pudo sincronizar " +
                                                "con MySQL. Intenta iniciar " +
                                                "sesión nuevamente."
                                    )

                                    // Evitar dejar una sesión activa
                                    // con datos no sincronizados.
                                    auth.signOut()
                                }

                            } catch (e: Exception) {

                                Log.e(
                                    "REGISTRO",
                                    "Error al sincronizar usuario",
                                    e
                                )

                                mostrarMensaje(
                                    "La cuenta se creó en Firebase, " +
                                            "pero no se pudo conectar con " +
                                            "MySQL. Intenta iniciar " +
                                            "sesión nuevamente."
                                )

                                auth.signOut()

                            } finally {

                                habilitarRegistro()
                            }
                        }
                    }
            }
    }

    private fun mostrarErrorFirebase(
        error: Exception?
    ) {

        val mensaje = when (error) {

            is FirebaseAuthUserCollisionException ->
                "Este correo electrónico ya está registrado"

            is FirebaseAuthWeakPasswordException ->
                "La contraseña es demasiado débil"

            is FirebaseAuthInvalidCredentialsException ->
                "El correo electrónico no es válido"

            else ->
                "No se pudo crear la cuenta. " +
                        "Verifica tu conexión e inténtalo nuevamente."
        }

        mostrarMensaje(mensaje)
    }

    private fun habilitarRegistro() {
        registrando = false
        btnRegister.isEnabled = true
        btnRegister.text = "REGISTRARSE"
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }
}