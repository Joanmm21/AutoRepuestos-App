package sv.edu.autorepuestos

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AdminCatalogoActivity : AppCompatActivity() {

    private val uid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private lateinit var lista: LinearLayout
    private lateinit var estado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_catalogo)

        lista = findViewById(R.id.listaCatalogoAdmin)
        estado = findViewById(R.id.tvEstadoCatalogo)

        // Regresar al panel principal del administrador
        findViewById<Button>(R.id.btnVolverCatalogo)
            .setOnClickListener {
                finish()
            }

        // Abrir formulario para registrar un producto
        findViewById<Button>(R.id.btnNuevoCatalogo)
            .setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        AdminProductoFormActivity::class.java
                    )
                )
            }
    }

    override fun onResume() {
        super.onResume()
        cargarCatalogo()
    }

    private fun cargarCatalogo() {
        estado.text = "Cargando catálogo..."

        lifecycleScope.launch {
            try {
                val respuesta =
                    RetrofitClient.apiService.obtenerInventarioAdmin(uid)

                if (!respuesta.isSuccessful) {
                    estado.text =
                        "Error al consultar catálogo: HTTP ${respuesta.code()}"
                    return@launch
                }

                val productos = respuesta.body().orEmpty()

                lista.removeAllViews()
                estado.text = "${productos.size} productos registrados"

                if (productos.isEmpty()) {
                    estado.text = "No hay productos registrados"
                    return@launch
                }

                productos.forEach { producto ->

                    val caja = LinearLayout(
                        this@AdminCatalogoActivity
                    ).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(14, 12, 14, 12)
                        setBackgroundColor(
                            android.graphics.Color.rgb(30, 31, 35)
                        )
                    }

                    val informacion = TextView(
                        this@AdminCatalogoActivity
                    ).apply {
                        text = buildString {
                            append(producto.nombre)
                            append("\n")
                            append(producto.categoria)
                            append(" · $")
                            append(producto.precio)
                            append(" · Stock: ")
                            append(producto.stock)
                            append("\n")
                            append(
                                if (producto.activo == 1) {
                                    "Activo"
                                } else {
                                    "Desactivado"
                                }
                            )
                        }

                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 14f
                    }

                    caja.addView(informacion)

                    val acciones = LinearLayout(
                        this@AdminCatalogoActivity
                    ).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }

                    val btnEditar = Button(
                        this@AdminCatalogoActivity
                    ).apply {
                        text = "Editar"

                        setOnClickListener {
                            val intent = Intent(
                                this@AdminCatalogoActivity,
                                AdminProductoFormActivity::class.java
                            )

                            intent.putExtra(
                                "id_producto",
                                producto.id_producto
                            )

                            startActivity(intent)
                        }
                    }

                    val btnEstado = Button(
                        this@AdminCatalogoActivity
                    ).apply {
                        text = if (producto.activo == 1) {
                            "Desactivar"
                        } else {
                            "Activar"
                        }

                        setOnClickListener {
                            confirmarCambio(producto)
                        }
                    }

                    acciones.addView(btnEditar)
                    acciones.addView(btnEstado)

                    caja.addView(acciones)

                    lista.addView(
                        caja,
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 10
                        }
                    )
                }

            } catch (e: Exception) {
                estado.text =
                    "Error de conexión: ${e.localizedMessage}"
            }
        }
    }

    private fun confirmarCambio(producto: InventarioAdmin) {

        val nuevoActivo = if (producto.activo == 1) 0 else 1

        val accion = if (nuevoActivo == 1) {
            "activar"
        } else {
            "desactivar"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar cambio")
            .setMessage(
                "¿Deseas $accion el producto ${producto.nombre}?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->

                lifecycleScope.launch {
                    try {
                        val respuesta =
                            RetrofitClient.apiService.cambiarActivoAdmin(
                                uid,
                                producto.id_producto,
                                ActivoRequest(nuevoActivo)
                            )

                        if (respuesta.isSuccessful) {

                            Toast.makeText(
                                this@AdminCatalogoActivity,
                                "Producto actualizado correctamente",
                                Toast.LENGTH_SHORT
                            ).show()

                            cargarCatalogo()

                        } else {
                            Toast.makeText(
                                this@AdminCatalogoActivity,
                                "Error HTTP ${respuesta.code()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@AdminCatalogoActivity,
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }
}