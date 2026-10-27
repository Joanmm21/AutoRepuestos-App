package sv.edu.autorepuestos

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AdminPedidosActivity : AppCompatActivity() {

    private val uid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private lateinit var lista: LinearLayout
    private lateinit var estado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_pedidos)

        lista = findViewById(R.id.listaPedidosAdmin)
        estado = findViewById(R.id.tvEstadoPedidosAdmin)

        // Regresar al panel principal
        findViewById<Button>(R.id.btnVolverPedidosAdmin)
            .setOnClickListener {
                finish()
            }
    }

    override fun onResume() {
        super.onResume()
        cargarPedidos()
    }

    private fun cargarPedidos() {

        estado.text = "Cargando pedidos..."

        lifecycleScope.launch {
            try {
                val respuesta =
                    RetrofitClient.apiService.obtenerPedidosAdmin(uid)

                if (!respuesta.isSuccessful) {
                    estado.text =
                        "Error al consultar pedidos: HTTP ${respuesta.code()}"
                    return@launch
                }

                val pedidos = respuesta.body().orEmpty()

                lista.removeAllViews()

                estado.text =
                    "${pedidos.size} pedidos registrados"

                if (pedidos.isEmpty()) {
                    estado.text = "No hay pedidos registrados"
                    return@launch
                }

                pedidos.forEach { pedido ->

                    val caja = LinearLayout(
                        this@AdminPedidosActivity
                    ).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(14, 12, 14, 12)
                        setBackgroundColor(
                            android.graphics.Color.rgb(30, 31, 35)
                        )
                    }

                    val detalles = pedido.productos.joinToString("\n") {
                        "${it.cantidad} × ${it.producto}"
                    }

                    val informacion = TextView(
                        this@AdminPedidosActivity
                    ).apply {
                        text = buildString {
                            append("Pedido #")
                            append(pedido.id_pedido)
                            append("\nEstado: ")
                            append(pedido.estado)
                            append("\nCliente: ")
                            append(pedido.cliente)
                            append("\n")
                            append(pedido.correo)
                            append("\n")
                            append(detalles)
                            append("\nTotal: $")
                            append(pedido.total)
                        }

                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 15f
                    }

                    caja.addView(informacion)

                    if (pedido.estado == "pendiente") {

                        val botones = LinearLayout(
                            this@AdminPedidosActivity
                        ).apply {
                            orientation = LinearLayout.HORIZONTAL
                        }

                        val btnCompletar = Button(
                            this@AdminPedidosActivity
                        ).apply {
                            text = "Completar"

                            setOnClickListener {
                                confirmarCambio(
                                    pedido.id_pedido,
                                    "completado"
                                )
                            }
                        }

                        val btnCancelar = Button(
                            this@AdminPedidosActivity
                        ).apply {
                            text = "Cancelar"

                            setOnClickListener {
                                confirmarCambio(
                                    pedido.id_pedido,
                                    "cancelado"
                                )
                            }
                        }

                        botones.addView(btnCompletar)
                        botones.addView(btnCancelar)

                        caja.addView(botones)
                    }

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

    private fun confirmarCambio(
        idPedido: Int,
        nuevoEstado: String
    ) {

        val mensaje = if (nuevoEstado == "cancelado") {
            "¿Cancelar este pedido y devolver sus unidades al inventario?"
        } else {
            "¿Marcar este pedido como completado?"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar pedido #$idPedido")
            .setMessage(mensaje)
            .setNegativeButton("Volver", null)
            .setPositiveButton("Confirmar") { _, _ ->

                lifecycleScope.launch {
                    try {
                        val respuesta =
                            RetrofitClient.apiService
                                .cambiarEstadoPedidoAdmin(
                                    uid,
                                    idPedido,
                                    EstadoPedidoRequest(nuevoEstado)
                                )

                        if (respuesta.isSuccessful) {

                            Toast.makeText(
                                this@AdminPedidosActivity,
                                "Pedido actualizado correctamente",
                                Toast.LENGTH_SHORT
                            ).show()

                            cargarPedidos()

                        } else {

                            Toast.makeText(
                                this@AdminPedidosActivity,
                                "Error HTTP ${respuesta.code()}: " +
                                        respuesta.errorBody()
                                            ?.string()
                                            .orEmpty(),
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {

                        Toast.makeText(
                            this@AdminPedidosActivity,
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }
}