package sv.edu.autorepuestos

data class CategoriaAdmin(
    val id_categoria: Int,
    val nombre: String
)

data class ProductoAdminRequest(
    val id_categoria: Int,
    val nombre: String,
    val marca: String?,
    val descripcion: String?,
    val precio: String,
    val stock: Int,
    val stock_minimo: Int
)

data class ActivoRequest(
    val activo: Int
)

data class EstadoPedidoRequest(
    val estado: String
)

data class RespuestaAdmin(
    val message: String?,
    val error: String?,
    val id_producto: Int?
)

data class PedidoAdmin(
    val id_pedido: Int,
    val fecha_pedido: String,
    val total: String,
    val estado: String,
    val cliente: String,
    val correo: String,
    val productos: List<DetalleHistorialPedido>
)