package sv.edu.autorepuestos

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    @POST("api/usuarios")
    suspend fun registrarUsuario(
        @Body usuario: UsuarioRequest
    ): Response<Any>

    @GET("api/productos")
    suspend fun obtenerProductos(): Response<List<Producto>>

    @POST("api/pedidos")
    suspend fun registrarPedido(
        @Body pedido: PedidoRequest
    ): Response<PedidoResponse>

    @GET("api/pedidos/usuario/{firebase_uid}")
    suspend fun obtenerHistorialPedidos(
        @Path("firebase_uid") firebaseUid: String
    ): Response<List<HistorialPedido>>

    @GET("api/usuarios/rol/{firebase_uid}")
    suspend fun obtenerRolUsuario(
        @Path("firebase_uid") firebaseUid: String
    ): Response<UsuarioRolResponse>

    @GET("api/admin/inventario")
    suspend fun obtenerInventarioAdmin(
        @Header("x-firebase-uid") uid: String
    ): Response<List<InventarioAdmin>>

    @GET("api/admin/categorias")
    suspend fun obtenerCategoriasAdmin(
        @Header("x-firebase-uid") uid: String
    ): Response<List<CategoriaAdmin>>

    @POST("api/admin/productos")
    suspend fun crearProductoAdmin(
        @Header("x-firebase-uid") uid: String,
        @Body producto: ProductoAdminRequest
    ): Response<RespuestaAdmin>

    @PUT("api/admin/productos/{id}")
    suspend fun editarProductoAdmin(
        @Header("x-firebase-uid") uid: String,
        @Path("id") id: Int,
        @Body producto: ProductoAdminRequest
    ): Response<RespuestaAdmin>

    @PATCH("api/admin/productos/{id}/activo")
    suspend fun cambiarActivoAdmin(
        @Header("x-firebase-uid") uid: String,
        @Path("id") id: Int,
        @Body body: ActivoRequest
    ): Response<RespuestaAdmin>

    @GET("api/admin/pedidos")
    suspend fun obtenerPedidosAdmin(
        @Header("x-firebase-uid") uid: String
    ): Response<List<PedidoAdmin>>

    @PATCH("api/admin/pedidos/{id}/estado")
    suspend fun cambiarEstadoPedidoAdmin(
        @Header("x-firebase-uid") uid: String,
        @Path("id") id: Int,
        @Body body: EstadoPedidoRequest
    ): Response<RespuestaAdmin>

    // NUEVO: subir fotografía del producto
    @Multipart
    @POST("api/admin/productos/{id}/imagen")
    suspend fun subirImagenProductoAdmin(
        @Header("x-firebase-uid") uid: String,
        @Path("id") id: Int,
        @Part imagen: MultipartBody.Part
    ): Response<ImagenProductoResponse>
}