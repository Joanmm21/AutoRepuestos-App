package sv.edu.autorepuestos

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DetalleProductoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_detalle_producto)

        // Elementos de la pantalla
        val tvVolver = findViewById<TextView>(R.id.tvVolver)
        val imgProducto = findViewById<ImageView>(R.id.imgDetalleProducto)
        val tvCategoria = findViewById<TextView>(R.id.tvDetalleCategoria)
        val tvNombre = findViewById<TextView>(R.id.tvDetalleNombre)
        val tvMarca = findViewById<TextView>(R.id.tvDetalleMarca)
        val tvPrecio = findViewById<TextView>(R.id.tvDetallePrecio)
        val tvDescripcion =
            findViewById<TextView>(R.id.tvDetalleDescripcion)
        val btnCarrito = findViewById<Button>(R.id.btnAgregarCarrito)

        // Datos recibidos desde ProductoAdapter
        val idProducto = intent.getIntExtra("id_producto", -1)
        val nombre = intent.getStringExtra("nombre") ?: "Producto"
        val categoria = intent.getStringExtra("categoria") ?: ""
        val marca = intent.getStringExtra("marca") ?: "Sin marca"
        val precio = intent.getStringExtra("precio") ?: "0.00"
        val descripcion =
            intent.getStringExtra("descripcion") ?: "Sin descripción"

        // Conservar la ruta, aunque ahora utilicemos fotos locales.
        val imagenUrl = intent.getStringExtra("imagen_url")

        // Mostrar información del producto
        tvCategoria.text = categoria.uppercase()
        tvNombre.text = nombre
        tvMarca.text = "Marca: $marca"
        tvPrecio.text = "$$precio"
        tvDescripcion.text = descripcion

        // Mostrar la fotografía correspondiente al producto.
        val imagenProducto = when (idProducto) {
            1 -> R.drawable.pastillasfreno
            2 -> R.drawable.filtroaceite
            3 -> R.drawable.bujias
            4 -> R.drawable.alternador
            5 -> R.drawable.aceitedemotor
            else -> android.R.drawable.ic_menu_gallery
        }

        imgProducto.setImageResource(imagenProducto)

        // Volver al catálogo
        tvVolver.setOnClickListener {
            finish()
        }

        // Agregar producto al carrito
        btnCarrito.setOnClickListener {

            if (idProducto == -1) {
                Toast.makeText(
                    this,
                    "No se pudo identificar el producto",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val producto = Producto(
                id_producto = idProducto,
                id_categoria = 0,
                categoria = categoria,
                nombre = nombre,
                marca = marca,
                descripcion = descripcion,
                precio = precio,
                imagen_url = imagenUrl,
                activo = 1
            )

            CarritoManager.agregarProducto(producto)

            Toast.makeText(
                this,
                "Producto agregado al carrito",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}