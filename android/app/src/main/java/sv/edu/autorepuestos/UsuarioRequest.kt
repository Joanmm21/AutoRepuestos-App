package sv.edu.autorepuestos

data class UsuarioRequest(
    val firebase_uid: String,
    val nombre: String,
    val correo: String,
    val rol: String = "cliente"
)