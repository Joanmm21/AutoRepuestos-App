package sv.edu.autorepuestos

data class DetalleHistorialPedido(
    val id_producto: Int,
    val producto: String,
    val cantidad: Int,
    val precio_unitario: String,
    val subtotal: String
)

data class HistorialPedido(
    val id_pedido: Int,
    val fecha_pedido: String,
    val total: String,
    val estado: String,
    val productos: List<DetalleHistorialPedido>
)