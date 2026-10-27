package sv.edu.autorepuestos

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class AdminProductoFormActivity : AppCompatActivity() {

    private val uid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private var idProducto = 0
    private var categorias = emptyList<CategoriaAdmin>()

    private lateinit var nombre: EditText
    private lateinit var marca: EditText
    private lateinit var descripcion: EditText
    private lateinit var precio: EditText
    private lateinit var stock: EditText
    private lateinit var minimo: EditText
    private lateinit var categoria: Spinner
    private lateinit var estado: TextView
    private lateinit var guardar: Button
    private lateinit var cancelar: Button

    private lateinit var vistaPrevia: ImageView
    private lateinit var estadoImagen: TextView
    private lateinit var seleccionarImagen: Button

    private var guardando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_admin_producto_form)

        idProducto = intent.getIntExtra("id_producto", 0)

        nombre = findViewById(R.id.etAdminNombre)
        marca = findViewById(R.id.etAdminMarca)
        descripcion = findViewById(R.id.etAdminDescripcion)
        precio = findViewById(R.id.etAdminPrecio)
        stock = findViewById(R.id.etAdminStock)
        minimo = findViewById(R.id.etAdminMinimo)
        categoria = findViewById(R.id.spAdminCategoria)
        estado = findViewById(R.id.tvAdminFormEstado)
        guardar = findViewById(R.id.btnGuardarProducto)
        cancelar = findViewById(R.id.btnCancelarProducto)

        vistaPrevia = findViewById(R.id.imgVistaPreviaProducto)
        estadoImagen = findViewById(R.id.tvEstadoImagenProducto)
        seleccionarImagen = findViewById(R.id.btnSeleccionarImagen)

        guardar.isEnabled = false

        findViewById<TextView>(
            R.id.tvTituloFormulario
        ).text = if (idProducto == 0) {
            "Agregar producto"
        } else {
            "Editar producto"
        }

        // Las fotografías de los cinco productos
        // están guardadas directamente en drawable.
        mostrarFotografiaLocal()

        // Ya no utilizaremos el selector de galería
        // en esta versión local de la aplicación.
        seleccionarImagen.visibility = android.view.View.GONE

        cancelar.setOnClickListener {
            if (!guardando) {
                finish()
            }
        }

        guardar.setOnClickListener {
            guardarProducto()
        }

        cargarDatos()
    }

    private fun mostrarFotografiaLocal() {

        val imagen = when (idProducto) {
            1 -> R.drawable.pastillasfreno
            2 -> R.drawable.filtroaceite
            3 -> R.drawable.bujias
            4 -> R.drawable.alternador
            5 -> R.drawable.aceitedemotor
            else -> android.R.drawable.ic_menu_gallery
        }

        vistaPrevia.setImageResource(imagen)

        estadoImagen.text = if (idProducto in 1..5) {
            "Fotografía del producto"
        } else {
            "Este producto todavía no tiene fotografía local."
        }
    }

    private fun cargarDatos() {

        estado.text = "Cargando datos..."

        lifecycleScope.launch {

            try {

                val respuestaCategorias =
                    RetrofitClient.apiService.obtenerCategoriasAdmin(uid)

                if (!respuestaCategorias.isSuccessful) {
                    estado.text =
                        "Error al consultar categorías: HTTP ${respuestaCategorias.code()}"
                    return@launch
                }

                categorias = respuestaCategorias.body().orEmpty()

                if (categorias.isEmpty()) {
                    estado.text = "No hay categorías disponibles"
                    return@launch
                }

                val adaptador = ArrayAdapter(
                    this@AdminProductoFormActivity,
                    android.R.layout.simple_spinner_item,
                    categorias.map { it.nombre }
                )

                adaptador.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                categoria.adapter = adaptador

                if (idProducto != 0) {

                    val respuestaInventario =
                        RetrofitClient.apiService.obtenerInventarioAdmin(uid)

                    if (!respuestaInventario.isSuccessful) {
                        estado.text =
                            "Error al consultar producto: HTTP ${respuestaInventario.code()}"
                        return@launch
                    }

                    val producto = respuestaInventario.body()
                        ?.find { it.id_producto == idProducto }

                    if (producto == null) {
                        estado.text = "Producto no encontrado"
                        return@launch
                    }

                    nombre.setText(producto.nombre)
                    marca.setText(producto.marca.orEmpty())
                    descripcion.setText(producto.descripcion.orEmpty())
                    precio.setText(producto.precio)
                    stock.setText(producto.stock.toString())
                    minimo.setText(producto.stock_minimo.toString())

                    val indice = categorias.indexOfFirst {
                        it.id_categoria == producto.id_categoria
                    }

                    categoria.setSelection(indice.coerceAtLeast(0))

                } else {
                    stock.setText("0")
                    minimo.setText("5")
                }

                estado.text = ""
                guardar.isEnabled = true

            } catch (e: Exception) {
                estado.text =
                    "Error al cargar datos: ${e.localizedMessage}"
            }
        }
    }

    private fun guardarProducto() {

        if (guardando) return

        val nombreTexto = nombre.text.toString().trim()
        val precioTexto = precio.text.toString().trim()

        val cantidad = stock.text.toString().toIntOrNull()
        val cantidadMinima = minimo.text.toString().toIntOrNull()

        val dinero = try {
            BigDecimal(precioTexto).setScale(
                2,
                RoundingMode.UNNECESSARY
            )
        } catch (e: Exception) {
            null
        }

        if (
            nombreTexto.isBlank() ||
            nombreTexto.length > 150 ||
            dinero == null ||
            dinero <= BigDecimal.ZERO ||
            dinero >= BigDecimal("100000000") ||
            cantidad == null ||
            cantidad < 0 ||
            cantidadMinima == null ||
            cantidadMinima < 0 ||
            categorias.isEmpty() ||
            categoria.selectedItemPosition !in categorias.indices
        ) {
            estado.text =
                "Revisa el nombre, precio, stock y stock mínimo"
            return
        }

        val producto = ProductoAdminRequest(
            id_categoria =
                categorias[categoria.selectedItemPosition].id_categoria,
            nombre = nombreTexto,
            marca = marca.text.toString().trim(),
            descripcion = descripcion.text.toString().trim(),
            precio = dinero.toPlainString(),
            stock = cantidad,
            stock_minimo = cantidadMinima
        )

        guardando = true
        guardar.isEnabled = false
        cancelar.isEnabled = false

        estado.text = "Guardando producto..."

        lifecycleScope.launch {

            try {

                val respuesta = if (idProducto == 0) {

                    RetrofitClient.apiService.crearProductoAdmin(
                        uid,
                        producto
                    )

                } else {

                    RetrofitClient.apiService.editarProductoAdmin(
                        uid,
                        idProducto,
                        producto
                    )
                }

                if (respuesta.isSuccessful) {

                    Toast.makeText(
                        this@AdminProductoFormActivity,
                        "Producto guardado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                } else {

                    estado.text =
                        "No se pudo guardar. HTTP ${respuesta.code()}: " +
                                respuesta.errorBody()
                                    ?.string()
                                    .orEmpty()
                }

            } catch (e: Exception) {

                estado.text =
                    "Error al guardar: ${e.localizedMessage}"

            } finally {

                guardando = false
                guardar.isEnabled = true
                cancelar.isEnabled = true
            }
        }
    }
}