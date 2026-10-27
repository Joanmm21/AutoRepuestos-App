package sv.edu.autorepuestos

data class InventarioAdmin(
    val id_producto: Int,
    val id_categoria: Int,
    val categoria: String,
    val nombre: String,
    val marca: String?,
    val descripcion: String?,
    val precio: String,
    val imagen_url: String?,
    val activo: Int,
    val stock: Int,
    val stock_minimo: Int
)