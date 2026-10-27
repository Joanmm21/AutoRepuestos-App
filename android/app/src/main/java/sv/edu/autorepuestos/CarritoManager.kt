package sv.edu.autorepuestos

import java.math.BigDecimal

data class ItemCarrito(
    val producto: Producto,
    var cantidad: Int
)

object CarritoManager {

    private val items = mutableListOf<ItemCarrito>()

    fun agregarProducto(producto: Producto) {
        val existente = items.find {
            it.producto.id_producto == producto.id_producto
        }

        if (existente != null) {
            existente.cantidad++
        } else {
            items.add(ItemCarrito(producto, 1))
        }
    }

    fun obtenerItems(): List<ItemCarrito> {
        return items.map { it.copy() }
    }

    fun cambiarCantidad(idProducto: Int, cantidad: Int) {
        val item = items.find {
            it.producto.id_producto == idProducto
        } ?: return

        if (cantidad <= 0) {
            items.remove(item)
        } else {
            item.cantidad = cantidad
        }
    }

    fun eliminarProducto(idProducto: Int) {
        items.removeAll {
            it.producto.id_producto == idProducto
        }
    }

    fun calcularTotal(): BigDecimal {
        return items.fold(BigDecimal.ZERO) { total, item ->
            total.add(
                BigDecimal(item.producto.precio)
                    .multiply(BigDecimal(item.cantidad))
            )
        }
    }

    fun vaciarCarrito() {
        items.clear()
    }
}