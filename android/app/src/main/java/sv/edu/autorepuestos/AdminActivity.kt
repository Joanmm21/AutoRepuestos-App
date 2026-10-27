package sv.edu.autorepuestos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    private val api
        get() = RetrofitClient.apiService

    private val uid
        get() = FirebaseAuth.getInstance()
            .currentUser?.uid.orEmpty()

    private lateinit var lista: LinearLayout
    private lateinit var estado: TextView
    private lateinit var buscar: EditText
    private lateinit var resumen: TextView

    private var productos = emptyList<InventarioAdmin>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_admin)

        lista = findViewById(R.id.listaInventario)
        estado = findViewById(R.id.tvEstadoInventario)
        buscar = findViewById(R.id.etBuscarInventario)
        resumen = findViewById(R.id.tvResumenInventario)

        val nombre = FirebaseAuth.getInstance()
            .currentUser?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: "Administrador"

        findViewById<TextView>(
            R.id.tvAdminBienvenida
        ).text = "Bienvenido, $nombre"

        // Cerrar sesión
        findViewById<Button>(
            R.id.btnAdminLogout
        ).setOnClickListener {

            CarritoManager.vaciarCarrito()
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(
                this,
                LoginActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
        }

        // Agregar producto
        findViewById<Button>(
            R.id.btnAgregarProducto
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AdminProductoFormActivity::class.java
                )
            )
        }

        // Abrir catálogo administrativo
        findViewById<TextView>(
            R.id.btnAdminCatalogo
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AdminCatalogoActivity::class.java
                )
            )
        }

        // Abrir gestión de pedidos
        findViewById<TextView>(
            R.id.btnAdminPedidos
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AdminPedidosActivity::class.java
                )
            )
        }

        // Ir a inventario
        findViewById<TextView>(
            R.id.btnAdminInventario
        ).setOnClickListener {
            buscar.requestFocus()
        }

        // Buscar productos
        buscar.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    mostrarInventario()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        cargarInventario()
    }

    private fun cargarInventario() {

        estado.text = "Cargando inventario..."

        lifecycleScope.launch {

            try {

                val respuesta =
                    api.obtenerInventarioAdmin(uid)

                if (respuesta.isSuccessful) {

                    productos = respuesta.body().orEmpty()

                    resumen.text =
                        "Inventario: ${productos.size} productos"

                    mostrarInventario()

                } else {

                    estado.text =
                        "Error HTTP ${respuesta.code()} " +
                                "al consultar inventario"
                }

            } catch (e: Exception) {

                estado.text =
                    "Error de conexión: ${e.localizedMessage}"
            }
        }
    }

    private fun mostrarInventario() {

        lista.removeAllViews()

        val texto = buscar.text.toString().trim()

        val filtrados = productos.filter { producto ->

            producto.nombre.contains(texto, true) ||
                    producto.categoria.contains(texto, true) ||
                    producto.marca.orEmpty().contains(texto, true)
        }

        estado.text =
            "${filtrados.size} productos encontrados"

        filtrados.forEach { producto ->

            // Fila principal del producto
            val fila = LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                setPadding(
                    8.dp(),
                    10.dp(),
                    8.dp(),
                    10.dp()
                )

                setBackgroundColor(
                    Color.rgb(30, 31, 35)
                )
            }

            // Fotografía del producto
            val imagen = ImageView(this).apply {

                layoutParams = LinearLayout.LayoutParams(
                    65.dp(),
                    65.dp()
                ).apply {
                    marginEnd = 10.dp()
                }

                scaleType = ImageView.ScaleType.FIT_CENTER

                setBackgroundColor(Color.WHITE)

                contentDescription =
                    "Fotografía de ${producto.nombre}"

                setPadding(
                    3.dp(),
                    3.dp(),
                    3.dp(),
                    3.dp()
                )

                setImageResource(
                    obtenerImagenProducto(producto.id_producto)
                )
            }

            // Información del producto
            val informacion = TextView(this).apply {

                text = buildString {

                    append(producto.nombre)
                    append("\n")

                    append(producto.categoria)
                    append(" · $")
                    append(producto.precio)

                    append("\nStock: ")
                    append(producto.stock)

                    if (
                        producto.stock <= producto.stock_minimo
                    ) {
                        append(" (BAJO)")
                    }

                    if (producto.activo == 0) {
                        append(" · Inactivo")
                    }
                }

                setTextColor(Color.WHITE)
                textSize = 12f

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Botón Editar
            val editar = Button(this).apply {

                text = "Editar"
                textSize = 10f

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    45.dp()
                )

                setOnClickListener {

                    startActivity(
                        Intent(
                            this@AdminActivity,
                            AdminProductoFormActivity::class.java
                        ).putExtra(
                            "id_producto",
                            producto.id_producto
                        )
                    )
                }
            }

            // Agregar elementos a la fila
            fila.addView(imagen)
            fila.addView(informacion)
            fila.addView(editar)

            // Agregar fila al inventario
            lista.addView(
                fila,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8.dp()
                }
            )
        }
    }

    // Fotografías guardadas en res/drawable
    private fun obtenerImagenProducto(
        idProducto: Int
    ): Int {

        return when (idProducto) {

            1 -> R.drawable.pastillasfreno

            2 -> R.drawable.filtroaceite

            3 -> R.drawable.bujias

            4 -> R.drawable.alternador

            5 -> R.drawable.aceitedemotor

            else -> R.mipmap.ic_launcher
        }
    }

    // Convertir dp a píxeles
    private fun Int.dp(): Int {
        return (
                this * resources.displayMetrics.density
                ).toInt()
    }
}