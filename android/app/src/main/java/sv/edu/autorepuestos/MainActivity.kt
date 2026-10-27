package sv.edu.autorepuestos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var productoAdapter: ProductoAdapter

    private var listaProductos: List<Producto> = emptyList()
    private var categoriaSeleccionada = "Todos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()

        // Elementos de la pantalla
        val tvSaludo = findViewById<TextView>(R.id.tvSaludo)
        val etBuscar = findViewById<EditText>(R.id.etBuscarProducto)
        val recycler = findViewById<RecyclerView>(R.id.recyclerProductos)

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnAbrirCarrito = findViewById<Button>(R.id.btnAbrirCarrito)

        // NUEVO: Botón del historial de compras
        val btnMisPedidos = findViewById<Button>(R.id.btnMisPedidos)

        // Mostrar el nombre del usuario
        val nombre = auth.currentUser?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: "Cliente"

        tvSaludo.text = "Hola, $nombre"

        // Configurar catálogo en dos columnas
        productoAdapter = ProductoAdapter(emptyList())

        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = productoAdapter

        // Filtros por categoría
        findViewById<Button>(R.id.btnTodos).setOnClickListener {
            seleccionarCategoria("Todos")
        }

        findViewById<Button>(R.id.btnFrenos).setOnClickListener {
            seleccionarCategoria("Frenos")
        }

        findViewById<Button>(R.id.btnMotor).setOnClickListener {
            seleccionarCategoria("Motor")
        }

        findViewById<Button>(R.id.btnSuspension).setOnClickListener {
            seleccionarCategoria("Suspensión")
        }

        findViewById<Button>(R.id.btnElectrico).setOnClickListener {
            seleccionarCategoria("Eléctrico")
        }

        // Buscar productos mientras se escribe
        etBuscar.addTextChangedListener(object : TextWatcher {

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
                filtrarProductos()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        // Abrir pantalla del carrito
        btnAbrirCarrito.setOnClickListener {

            val intent = Intent(
                this,
                CarritoActivity::class.java
            )

            startActivity(intent)
        }

        // NUEVO: Abrir historial de compras
        btnMisPedidos.setOnClickListener {

            val intent = Intent(
                this,
                HistorialPedidosActivity::class.java
            )

            startActivity(intent)
        }

        // Cerrar sesión
        btnLogout.setOnClickListener {

            // Evitar compartir el carrito entre usuarios
            CarritoManager.vaciarCarrito()

            auth.signOut()

            val intent = Intent(
                this,
                LoginActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }

        // Consultar productos reales desde MySQL
        cargarProductos()
    }

    private fun seleccionarCategoria(categoria: String) {
        categoriaSeleccionada = categoria
        filtrarProductos()
    }

    private fun filtrarProductos() {

        val texto = findViewById<EditText>(
            R.id.etBuscarProducto
        ).text.toString().trim()

        val productosFiltrados = listaProductos.filter { producto ->

            val coincideCategoria =
                categoriaSeleccionada == "Todos" ||
                        producto.categoria.equals(
                            categoriaSeleccionada,
                            ignoreCase = true
                        )

            val coincideBusqueda =
                producto.nombre.contains(
                    texto,
                    ignoreCase = true
                ) ||
                        (producto.marca?.contains(
                            texto,
                            ignoreCase = true
                        ) == true) ||
                        producto.categoria.contains(
                            texto,
                            ignoreCase = true
                        )

            coincideCategoria && coincideBusqueda
        }

        productoAdapter.actualizarProductos(productosFiltrados)
    }

    private fun cargarProductos() {

        lifecycleScope.launch {

            try {

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.obtenerProductos()
                }

                if (response.isSuccessful) {

                    listaProductos = response.body() ?: emptyList()

                    filtrarProductos()

                } else {

                    Toast.makeText(
                        this@MainActivity,
                        "Error al cargar productos: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    "PRODUCTOS",
                    "Error al consultar productos",
                    e
                )

                Toast.makeText(
                    this@MainActivity,
                    "No se pudo conectar con el servidor",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}