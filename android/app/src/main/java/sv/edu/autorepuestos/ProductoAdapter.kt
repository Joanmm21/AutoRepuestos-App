package sv.edu.autorepuestos

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    private var productos: List<Producto>
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imagen: ImageView =
            itemView.findViewById(R.id.imgProducto)

        val categoria: TextView =
            itemView.findViewById(R.id.tvCategoriaProducto)

        val nombre: TextView =
            itemView.findViewById(R.id.tvNombreProducto)

        val precio: TextView =
            itemView.findViewById(R.id.tvPrecioProducto)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductoViewHolder {

        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)

        return ProductoViewHolder(vista)
    }

    override fun onBindViewHolder(
        holder: ProductoViewHolder,
        position: Int
    ) {

        val producto = productos[position]

        holder.categoria.text = producto.categoria.uppercase()
        holder.nombre.text = producto.nombre
        holder.precio.text = "$${producto.precio}"

        // Fotografías locales de los cinco productos.
        val imagenProducto = when (producto.id_producto) {
            1 -> R.drawable.pastillasfreno
            2 -> R.drawable.filtroaceite
            3 -> R.drawable.bujias
            4 -> R.drawable.alternador
            5 -> R.drawable.aceitedemotor
            else -> android.R.drawable.ic_menu_gallery
        }

        holder.imagen.setImageResource(imagenProducto)

        // Abrir la pantalla de detalle.
        holder.itemView.setOnClickListener {

            val context = holder.itemView.context

            val intent = Intent(
                context,
                DetalleProductoActivity::class.java
            )

            intent.putExtra(
                "id_producto",
                producto.id_producto
            )

            intent.putExtra(
                "nombre",
                producto.nombre
            )

            intent.putExtra(
                "categoria",
                producto.categoria
            )

            intent.putExtra(
                "marca",
                producto.marca ?: "Sin marca"
            )

            intent.putExtra(
                "precio",
                producto.precio
            )

            intent.putExtra(
                "descripcion",
                producto.descripcion ?: "Sin descripción"
            )

            intent.putExtra(
                "imagen_url",
                producto.imagen_url
            )

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = productos.size

    fun actualizarProductos(nuevaLista: List<Producto>) {
        productos = nuevaLista
        notifyDataSetChanged()
    }
}