const mysql = require('mysql2');

const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',          // Tu usuario de MySQL (por defecto es root)
    password: '*',      // Tu contraseña de MySQL (déjala en '' si no tienes)
    database: 'autorepuestos_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// Probar la conexión
pool.getConnection((err, connection) => {
    if (err) {
        console.error('❌ Error al conectar a la base de datos MySQL:', err.message);
    } else {
        console.log('✅ Conexión exitosa a la base de datos MySQL (autorepuestos_db)');
        connection.release();
    }
});

module.exports = pool.promise();