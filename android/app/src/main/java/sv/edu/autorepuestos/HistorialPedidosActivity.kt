package sv.edu.autorepuestos

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HistorialPedidosActivity : AppCompatActivity() {

    private lateinit var contenedor: LinearLayout
    private lateinit var tvVacio: TextView
    private lateinit var progreso: ProgressBar

    private val colorBlanco = 0xFFFFFFFF.toInt()
    private val colorGris = 0xFFA7A7AD.toInt()
    private val colorNaranja = 0xFFFF791F.toInt()
    private val colorTarjeta = 0xFF22242A.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_pedidos)

        contenedor = findViewById(R.id.contenedorHistorial)
        tvVacio = findViewById(R.id.tvHistorialVacio)
        progreso = findViewById(R.id.progresoHistorial)

        findViewById<TextView>(R.id.tvVolverHistorial)
            .setOnClickListener {
                finish()
            }

        cargarHistorial()
    }

    private fun cargarHistorial() {

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            mostrarMensaje("Debes iniciar sesión para ver tus pedidos")
            return
        }

        progreso.visibility = View.VISIBLE
        tvVacio.visibility = View.GONE
        contenedor.removeAllViews()

        lifecycleScope.launch {

            try {

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.obtenerHistorialPedidos(
                        usuario.uid
                    )
                }

                if (response.isSuccessful) {

                    val pedidos = response.body().orEmpty()

                    if (pedidos.isEmpty()) {
                        mostrarMensaje(
                            "Todavía no tienes pedidos registrados"
                        )
                    } else {
                        mostrarPedidos(pedidos)
                    }

                } else {

                    mostrarMensaje(
                        "No se pudo consultar el historial. " +
                                "Código: ${response.code()}"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "HISTORIAL",
                    "Error al consultar historial",
                    e
                )

                mostrarMensaje(
                    "Error de conexión. Comprueba que Node.js esté funcionando."
                )

            } finally {
                progreso.visibility = View.GONE
            }
        }
    }

    private fun mostrarPedidos(pedidos: List<HistorialPedido>) {

        contenedor.removeAllViews()
        tvVacio.visibility = View.GONE

        for (pedido in pedidos) {

            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18.dp(), 18.dp(), 18.dp(), 18.dp())
                setBackgroundColor(colorTarjeta)
            }

            val encabezado = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val titulo = crearTexto(
                "Pedido #${pedido.id_pedido}",
                18f,
                colorBlanco,
                true
            )

            val estado = crearTexto(
                pedido.estado.replaceFirstChar {
                    it.uppercase()
                },
                14f,
                colorNaranja,
                true
            )

            encabezado.addView(
                titulo,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            encabezado.addView(estado)
            tarjeta.addView(encabezado)

            val fecha = crearTexto(
                formatearFecha(pedido.fecha_pedido),
                13f,
                colorGris
            )

            fecha.setPadding(0, 8.dp(), 0, 16.dp())
            tarjeta.addView(fecha)

            // Productos del pedido
            for (detalle in pedido.productos) {

                val fila = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 8.dp(), 0, 8.dp())
                }

                val informacion = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }

                val nombreProducto = crearTexto(
                    detalle.producto,
                    14f,
                    colorBlanco,
                    true
                )

                val cantidad = crearTexto(
                    "Cantidad: ${detalle.cantidad}",
                    13f,
                    colorGris
                )

                informacion.addView(nombreProducto)
                informacion.addView(cantidad)

                fila.addView(
                    informacion,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                )

                val subtotal = crearTexto(
                    formatearPrecio(detalle.subtotal),
                    14f,
                    colorBlanco,
                    true
                )

                subtotal.setPadding(10.dp(), 0, 0, 0)
                fila.addView(subtotal)

                tarjeta.addView(fila)
            }

            // Separador
            val separador = View(this).apply {
                setBackgroundColor(0xFF41434A.toInt())
            }

            tarjeta.addView(
                separador,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.dp()
                ).apply {
                    topMargin = 14.dp()
                    bottomMargin = 14.dp()
                }
            )

            // Total
            val filaTotal = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val etiquetaTotal = crearTexto(
                "TOTAL",
                15f,
                colorBlanco,
                true
            )

            filaTotal.addView(
                etiquetaTotal,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            val valorTotal = crearTexto(
                formatearPrecio(pedido.total),
                19f,
                colorNaranja,
                true
            )

            filaTotal.addView(valorTotal)
            tarjeta.addView(filaTotal)

            // Separación entre pedidos
            val parametros = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16.dp()
            }

            contenedor.addView(tarjeta, parametros)
        }
    }

    private fun mostrarMensaje(mensaje: String) {
        contenedor.removeAllViews()
        tvVacio.text = mensaje
        tvVacio.visibility = View.VISIBLE
    }

    private fun crearTexto(
        contenido: String,
        tamano: Float,
        color: Int,
        negrita: Boolean = false
    ): TextView {

        return TextView(this).apply {
            text = contenido
            textSize = tamano
            setTextColor(color)

            if (negrita) {
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
            }
        }
    }

    private fun formatearPrecio(precio: String): String {

        return try {
            val valor = BigDecimal(precio)
                .setScale(2, java.math.RoundingMode.HALF_UP)

            "$$valor"
        } catch (e: Exception) {
            "$$precio"
        }
    }

    private fun formatearFecha(fechaOriginal: String): String {

        return try {

            val formatoEntrada = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }

            val fecha: Date = formatoEntrada.parse(fechaOriginal)
                ?: return fechaOriginal

            val formatoSalida = SimpleDateFormat(
                "dd/MM/yyyy - hh:mm a",
                Locale.getDefault()
            )

            formatoSalida.format(fecha)

        } catch (e: Exception) {
            fechaOriginal
        }
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}