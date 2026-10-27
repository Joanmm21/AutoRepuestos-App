package sv.edu.autorepuestos

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode

class CarritoActivity : AppCompatActivity() {

    private lateinit var contenedor: LinearLayout
    private lateinit var tvTotal: TextView
    private lateinit var tvVacio: TextView
    private lateinit var btnConfirmar: Button

    private var enviandoPedido = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrito)

        // Manejo del botón o gesto Atrás
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!enviandoPedido) {
                        finish()
                    }
                }
            }
        )

        // Conectar elementos de la pantalla
        contenedor = findViewById(R.id.contenedorCarrito)
        tvTotal = findViewById(R.id.tvTotalCarrito)
        tvVacio = findViewById(R.id.tvCarritoVacio)
        btnConfirmar = findViewById(R.id.btnConfirmarPedido)

        // Volver al catálogo
        findViewById<TextView>(R.id.tvVolverCarrito)
            .setOnClickListener {
                if (!enviandoPedido) {
                    finish()
                }
            }

        // Confirmar pedido
        btnConfirmar.setOnClickListener {
            mostrarConfirmacion()
        }

        actualizarPantalla()
    }

    private fun mostrarConfirmacion() {

        if (enviandoPedido) return

        val items = CarritoManager.obtenerItems()

        // Validar carrito vacío
        if (items.isEmpty()) {
            Toast.makeText(
                this,
                "Tu carrito está vacío. Agrega al menos un producto antes de confirmar.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val total = CarritoManager.calcularTotal()
            .setScale(2, RoundingMode.HALF_UP)

        AlertDialog.Builder(this)
            .setTitle("Confirmar pedido")
            .setMessage(
                "¿Deseas registrar este pedido por $$total?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                registrarPedido()
            }
            .show()
    }

    private fun registrarPedido() {

        if (enviandoPedido) return

        // Obtener usuario autenticado
        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            Toast.makeText(
                this,
                "Debes iniciar sesión para realizar un pedido",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Obtener productos del carrito
        val items = CarritoManager.obtenerItems()

        if (items.isEmpty()) {
            Toast.makeText(
                this,
                "Tu carrito está vacío. Agrega un producto para continuar.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Preparar productos para la API
        val productos = items.map { item ->
            ProductoPedidoRequest(
                id_producto = item.producto.id_producto,
                cantidad = item.cantidad
            )
        }

        val pedido = PedidoRequest(
            firebase_uid = usuario.uid,
            productos = productos
        )

        enviandoPedido = true
        btnConfirmar.isEnabled = false
        btnConfirmar.text = "REGISTRANDO PEDIDO..."

        // Enviar pedido al backend
        lifecycleScope.launch {

            try {

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.registrarPedido(pedido)
                }

                if (response.isSuccessful) {

                    val resultado = response.body()

                    if (resultado?.id_pedido != null) {

                        // Vaciar carrito solo si el servidor confirmó el pedido
                        CarritoManager.vaciarCarrito()

                        actualizarPantalla()

                        AlertDialog.Builder(this@CarritoActivity)
                            .setTitle("¡Pedido registrado!")
                            .setMessage(
                                "Tu pedido #${resultado.id_pedido} " +
                                        "se registró correctamente.\n\n" +
                                        "Total: $${resultado.total}\n" +
                                        "Estado: ${resultado.estado}"
                            )
                            .setPositiveButton("Aceptar") { _, _ ->
                                finish()
                            }
                            .setCancelable(false)
                            .show()

                    } else {

                        Toast.makeText(
                            this@CarritoActivity,
                            "El servidor no devolvió el número del pedido. " +
                                    "Comprueba MySQL antes de volver a intentarlo.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {

                    val mensajeError = when (response.code()) {

                        400 ->
                            "Hay productos no disponibles o datos inválidos"

                        404 ->
                            "No se encontró tu usuario en MySQL"

                        409 ->
                            "No hay existencias suficientes"

                        else ->
                            "No se pudo registrar el pedido: ${response.code()}"
                    }

                    Toast.makeText(
                        this@CarritoActivity,
                        mensajeError,
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    "PEDIDOS",
                    "Error al registrar pedido",
                    e
                )

                Toast.makeText(
                    this@CarritoActivity,
                    "No se pudo confirmar la respuesta del servidor. " +
                            "Comprueba MySQL antes de volver a intentarlo.",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                enviandoPedido = false
                btnConfirmar.text = "CONFIRMAR PEDIDO"

                actualizarPantalla()
            }
        }
    }

    private fun actualizarPantalla() {

        // Limpiar las tarjetas anteriores
        contenedor.removeAllViews()

        val items = CarritoManager.obtenerItems()

        // Mostrar mensaje cuando el carrito esté vacío
        tvVacio.visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE

        // IMPORTANTE:
        // El botón permanece habilitado cuando el carrito está vacío
        // para que pueda mostrar el mensaje de validación.
        // Solo se deshabilita mientras se registra un pedido.
        btnConfirmar.isEnabled = !enviandoPedido

        for (item in items) {

            // Tarjeta de producto
            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
                setBackgroundColor(getColor(R.color.card_dark))
            }

            // Nombre del producto
            val nombre = TextView(this).apply {
                text = item.producto.nombre
                textSize = 16f
                setTextColor(getColor(R.color.text_white))
            }

            // Precio unitario
            val precio = TextView(this).apply {
                text = "Precio unitario: $${item.producto.precio}"
                textSize = 14f
                setTextColor(getColor(R.color.text_muted))
            }

            // Cantidad
            val cantidad = TextView(this).apply {
                text = "Cantidad: ${item.cantidad}"
                textSize = 15f
                setTextColor(getColor(R.color.text_white))
            }

            // Contenedor de botones
            val controles = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            // Disminuir cantidad
            val btnMenos = Button(this).apply {
                text = "−"
                isEnabled = !enviandoPedido

                setOnClickListener {
                    CarritoManager.cambiarCantidad(
                        item.producto.id_producto,
                        item.cantidad - 1
                    )
                    actualizarPantalla()
                }
            }

            // Aumentar cantidad
            val btnMas = Button(this).apply {
                text = "+"
                isEnabled = !enviandoPedido

                setOnClickListener {
                    CarritoManager.cambiarCantidad(
                        item.producto.id_producto,
                        item.cantidad + 1
                    )
                    actualizarPantalla()
                }
            }

            // Eliminar producto
            val btnEliminar = Button(this).apply {
                text = "Eliminar"
                isEnabled = !enviandoPedido

                setOnClickListener {
                    CarritoManager.eliminarProducto(
                        item.producto.id_producto
                    )
                    actualizarPantalla()
                }
            }

            // Agregar botones a la fila
            controles.addView(
                btnMenos,
                LinearLayout.LayoutParams(0, 48.dp(), 1f)
            )

            controles.addView(
                btnMas,
                LinearLayout.LayoutParams(0, 48.dp(), 1f)
            )

            controles.addView(
                btnEliminar,
                LinearLayout.LayoutParams(0, 48.dp(), 2f)
            )

            // Agregar información a la tarjeta
            tarjeta.addView(nombre)
            tarjeta.addView(precio)
            tarjeta.addView(cantidad)
            tarjeta.addView(controles)

            // Separación entre tarjetas
            val parametros = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12.dp()
            }

            contenedor.addView(tarjeta, parametros)
        }

        // Calcular y mostrar el total
        val total = CarritoManager.calcularTotal()
            .setScale(2, RoundingMode.HALF_UP)

        tvTotal.text = "TOTAL: $$total"
    }

    // Convertir dp a píxeles
    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}