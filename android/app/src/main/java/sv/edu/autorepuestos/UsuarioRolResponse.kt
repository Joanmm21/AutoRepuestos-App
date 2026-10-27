package sv.edu.autorepuestos

data class UsuarioRolResponse(
    val id_usuario: Int,
    val nombre: String,
    val correo: String,
    val rol: String
)