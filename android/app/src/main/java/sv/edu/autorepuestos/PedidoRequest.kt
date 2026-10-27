package sv.edu.autorepuestos

data class ProductoPedidoRequest(
    val id_producto: Int,
    val cantidad: Int
)

data class PedidoRequest(
    val firebase_uid: String,
    val productos: List<ProductoPedidoRequest>
)

data class PedidoResponse(
    val message: String?,
    val id_pedido: Int?,
    val total: String?,
    val estado: String?,
    val error: String?
)