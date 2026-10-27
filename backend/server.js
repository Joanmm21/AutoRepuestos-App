const express = require('express');
const cors = require('cors');
const db = require('./db');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const app = express();

app.use(express.json());
app.use(cors());

/*
 * AUTOPARTS - BACKEND NODE.JS
 *
 * Proyecto académico local.
 *
 */

// CONFIGURACIÓN DE FOTOGRAFÍAS
const carpetaUploads = path.join(__dirname, 'uploads');

fs.mkdirSync(carpetaUploads, {
    recursive: true
});

// Permitir consultar las fotografías desde Android.
app.use(
    '/uploads',
    express.static(carpetaUploads)
);

// Configurar almacenamiento.
const almacenamientoImagenes = multer.diskStorage({

    destination: (req, file, cb) => {
        cb(null, carpetaUploads);
    },

    filename: (req, file, cb) => {

        const extensiones = {
            'image/jpeg': '.jpg',
            'image/png': '.png',
            'image/webp': '.webp'
        };

        const extension = extensiones[file.mimetype];

        if (!extension) {
            return cb(
                new Error('Formato de imagen no permitido')
            );
        }

        const nombreArchivo =
            crypto.randomUUID() + extension;

        cb(null, nombreArchivo);
    }
});

const subirImagen = multer({

    storage: almacenamientoImagenes,

    limits: {
        fileSize: 5 * 1024 * 1024,
        files: 1
    },

    fileFilter: (req, file, cb) => {

        const formatosPermitidos = [
            'image/jpeg',
            'image/png',
            'image/webp'
        ];

        if (!formatosPermitidos.includes(file.mimetype)) {

            return cb(
                new Error(
                    'Solo se permiten imágenes JPG, PNG o WebP'
                )
            );
        }

        cb(null, true);
    }
});

// CONTROL DE ADMINISTRADOR LOCAL
async function adminLocal(req, res, next) {

    try {

        const uid = req.get('x-firebase-uid');

        if (!uid) {

            return res.status(401).json({
                error: 'Falta identificador de sesión'
            });
        }

        const [usuarios] = await db.query(
            `SELECT rol
             FROM usuarios
             WHERE firebase_uid = ?`,
            [uid]
        );

        if (
            usuarios.length === 0 ||
            !['admin', 'administrador'].includes(
                usuarios[0].rol
            )
        ) {

            return res.status(403).json({
                error: 'Acceso restringido a administradores'
            });
        }

        next();

    } catch (error) {

        console.error(
            'Error al comprobar rol:',
            error
        );

        res.status(500).json({
            error: 'Error al comprobar rol'
        });
    }
}

// 1. REGISTRAR O SINCRONIZAR USUARIO
app.post('/api/usuarios', async (req, res) => {

    try {

        const {
            firebase_uid,
            nombre,
            correo
        } = req.body || {};

        if (
            ![firebase_uid, nombre, correo].every(
                x =>
                    typeof x === 'string' &&
                    x.trim()
            )
        ) {

            return res.status(400).json({
                error: 'Faltan datos del usuario'
            });
        }

        const uid = firebase_uid.trim();
        const email = correo.trim();

        const [usuarios] = await db.query(
            `SELECT *
             FROM usuarios
             WHERE firebase_uid = ? OR correo = ?`,
            [uid, email]
        );

        if (usuarios.length > 0) {

            const existente = usuarios.find(
                u => u.firebase_uid === uid
            );

            if (!existente) {

                return res.status(409).json({
                    error: 'Correo asociado a otra cuenta'
                });
            }

            return res.json({
                message: 'Usuario existente',
                usuario: existente
            });
        }

        // Los usuarios nuevos siempre son clientes.
        const [resultado] = await db.query(
            `INSERT INTO usuarios
                (firebase_uid, nombre, correo, rol)
             VALUES (?, ?, ?, 'cliente')`,
            [
                uid,
                nombre.trim(),
                email
            ]
        );

        const [creados] = await db.query(
            `SELECT *
             FROM usuarios
             WHERE id_usuario = ?`,
            [resultado.insertId]
        );

        res.status(201).json({
            message: 'Usuario registrado',
            usuario: creados[0]
        });

    } catch (error) {

        console.error(
            'Error al registrar usuario:',
            error
        );

        res.status(500).json({
            error: 'Error al registrar usuario'
        });
    }
});

// 2. CONSULTAR ROL DEL USUARIO
app.get(
    '/api/usuarios/rol/:firebase_uid',
    async (req, res) => {

        try {

            const [usuarios] = await db.query(
                `SELECT
                    id_usuario,
                    nombre,
                    correo,
                    rol
                 FROM usuarios
                 WHERE firebase_uid = ?`,
                [req.params.firebase_uid]
            );

            if (!usuarios.length) {

                return res.status(404).json({
                    error: 'Usuario no encontrado'
                });
            }

            res.json(usuarios[0]);

        } catch (error) {

            console.error(
                'Error al consultar rol:',
                error
            );

            res.status(500).json({
                error: 'Error al consultar rol'
            });
        }
    }
);

// 3. CATÁLOGO DEL CLIENTE
app.get('/api/productos', async (req, res) => {

    try {

        const [productos] = await db.query(
            `SELECT
                p.id_producto,
                p.id_categoria,
                c.nombre AS categoria,
                p.nombre,
                p.marca,
                p.descripcion,
                p.precio,
                p.imagen_url,
                p.activo
             FROM productos p
             INNER JOIN categorias c
                ON c.id_categoria = p.id_categoria
             WHERE p.activo = 1
             ORDER BY p.id_producto DESC`
        );

        res.json(productos);

    } catch (error) {

        console.error(
            'Error al consultar productos:',
            error
        );

        res.status(500).json({
            error: 'Error al consultar productos'
        });
    }
});

// 4. REGISTRAR PEDIDO DEL CLIENTE
app.post('/api/pedidos', async (req, res) => {

    const {
        firebase_uid,
        productos
    } = req.body || {};

    if (
        typeof firebase_uid !== 'string' ||
        !firebase_uid.trim() ||
        !Array.isArray(productos) ||
        productos.length === 0
    ) {

        return res.status(400).json({
            error: 'Datos del pedido inválidos'
        });
    }

    const cantidades = new Map();

    for (const item of productos) {

        if (
            !item ||
            !Number.isSafeInteger(item.id_producto) ||
            item.id_producto <= 0 ||
            !Number.isSafeInteger(item.cantidad) ||
            item.cantidad <= 0
        ) {

            return res.status(400).json({
                error: 'Producto o cantidad inválidos'
            });
        }

        const cantidad =
            (cantidades.get(item.id_producto) || 0) +
            item.cantidad;

        if (!Number.isSafeInteger(cantidad)) {

            return res.status(400).json({
                error: 'Cantidad fuera de rango'
            });
        }

        cantidades.set(
            item.id_producto,
            cantidad
        );
    }

    let conexion;

    try {

        conexion = await db.getConnection();

        await conexion.beginTransaction();

        const [usuarios] = await conexion.query(
            `SELECT id_usuario
             FROM usuarios
             WHERE firebase_uid = ?`,
            [firebase_uid.trim()]
        );

        if (!usuarios.length) {

            await conexion.rollback();

            return res.status(404).json({
                error: 'Usuario no encontrado'
            });
        }

        const detalles = [];

        let totalCentavos = 0;

        const ids = [...cantidades.keys()].sort(
            (a, b) => a - b
        );

        for (const idProducto of ids) {

            const [filas] = await conexion.query(
                `SELECT
                    p.nombre,
                    p.precio,
                    p.activo,
                    i.stock
                 FROM productos p
                 INNER JOIN inventario i
                    ON i.id_producto = p.id_producto
                 WHERE p.id_producto = ?
                 FOR UPDATE`,
                [idProducto]
            );

            if (
                !filas.length ||
                Number(filas[0].activo) !== 1
            ) {

                await conexion.rollback();

                return res.status(400).json({
                    error:
                        `Producto ${idProducto} no disponible`
                });
            }

            const producto = filas[0];

            const cantidad = cantidades.get(
                idProducto
            );

            if (
                Number(producto.stock) < cantidad
            ) {

                await conexion.rollback();

                return res.status(409).json({
                    error:
                        `Stock insuficiente para ${producto.nombre}`
                });
            }

            const partes =
                String(producto.precio).split('.');

            const precioCentavos =
                Number(partes[0]) * 100 +
                Number(
                    (partes[1] || '').padEnd(2, '0')
                );

            const subtotalCentavos =
                precioCentavos * cantidad;

            totalCentavos += subtotalCentavos;

            if (
                !Number.isSafeInteger(precioCentavos) ||
                !Number.isSafeInteger(subtotalCentavos) ||
                !Number.isSafeInteger(totalCentavos)
            ) {

                throw new Error(
                    'Total fuera de rango'
                );
            }

            detalles.push({
                id_producto: idProducto,
                cantidad,
                precio:
                    (precioCentavos / 100).toFixed(2)
            });
        }

        const total =
            (totalCentavos / 100).toFixed(2);

        const [resultado] = await conexion.query(
            `INSERT INTO pedidos
                (id_usuario, total, estado)
             VALUES (?, ?, 'pendiente')`,
            [
                usuarios[0].id_usuario,
                total
            ]
        );

        for (const detalle of detalles) {

            await conexion.query(
                `INSERT INTO detalle_pedidos
                    (
                        id_pedido,
                        id_producto,
                        cantidad,
                        precio_unitario
                    )
                 VALUES (?, ?, ?, ?)`,
                [
                    resultado.insertId,
                    detalle.id_producto,
                    detalle.cantidad,
                    detalle.precio
                ]
            );

            await conexion.query(
                `UPDATE inventario
                 SET stock = stock - ?
                 WHERE id_producto = ?`,
                [
                    detalle.cantidad,
                    detalle.id_producto
                ]
            );
        }

        await conexion.commit();

        res.status(201).json({
            message:
                'Pedido registrado correctamente',
            id_pedido: resultado.insertId,
            total,
            estado: 'pendiente'
        });

    } catch (error) {

        if (conexion) {

            try {
                await conexion.rollback();
            } catch (_) {}
        }

        console.error(
            'Error al registrar pedido:',
            error
        );

        res.status(500).json({
            error: 'No se pudo registrar el pedido'
        });

    } finally {

        if (conexion) {
            conexion.release();
        }
    }
});

// 5. HISTORIAL DE PEDIDOS DEL CLIENTE
app.get(
    '/api/pedidos/usuario/:firebase_uid',
    async (req, res) => {

        try {

            const [usuarios] = await db.query(
                `SELECT id_usuario
                 FROM usuarios
                 WHERE firebase_uid = ?`,
                [req.params.firebase_uid]
            );

            if (!usuarios.length) {

                return res.status(404).json({
                    error: 'Usuario no encontrado'
                });
            }

            const [pedidos] = await db.query(
                `SELECT
                    id_pedido,
                    fecha_pedido,
                    total,
                    estado
                 FROM pedidos
                 WHERE id_usuario = ?
                 ORDER BY
                    fecha_pedido DESC,
                    id_pedido DESC`,
                [usuarios[0].id_usuario]
            );

            const historial = [];

            for (const pedido of pedidos) {

                const [detalles] = await db.query(
                    `SELECT
                        d.id_producto,
                        p.nombre AS producto,
                        d.cantidad,
                        d.precio_unitario,
                        d.subtotal
                     FROM detalle_pedidos d
                     INNER JOIN productos p
                        ON p.id_producto = d.id_producto
                     WHERE d.id_pedido = ?
                     ORDER BY d.id_detalle`,
                    [pedido.id_pedido]
                );

                historial.push({
                    ...pedido,
                    productos: detalles
                });
            }

            res.json(historial);

        } catch (error) {

            console.error(
                'Error al consultar historial:',
                error
            );

            res.status(500).json({
                error: 'Error al consultar historial'
            });
        }
    }
);

// 6. INVENTARIO DEL ADMINISTRADOR
app.get(
    '/api/admin/inventario',
    adminLocal,
    async (req, res) => {

        try {

            const [inventario] = await db.query(
                `SELECT
                    p.id_producto,
                    p.id_categoria,
                    c.nombre AS categoria,
                    p.nombre,
                    p.marca,
                    p.descripcion,
                    p.precio,
                    p.imagen_url,
                    p.activo,
                    COALESCE(i.stock, 0) AS stock,
                    COALESCE(
                        i.stock_minimo,
                        5
                    ) AS stock_minimo
                 FROM productos p
                 INNER JOIN categorias c
                    ON c.id_categoria = p.id_categoria
                 LEFT JOIN inventario i
                    ON i.id_producto = p.id_producto
                 ORDER BY p.id_producto DESC`
            );

            res.json(inventario);

        } catch (error) {

            console.error(
                'Error al consultar inventario:',
                error
            );

            res.status(500).json({
                error: 'Error al consultar inventario'
            });
        }
    }
);

// 7. CATEGORÍAS DISPONIBLES
app.get(
    '/api/admin/categorias',
    adminLocal,
    async (req, res) => {

        try {

            const [categorias] = await db.query(
                `SELECT
                    id_categoria,
                    nombre
                 FROM categorias
                 ORDER BY nombre`
            );

            res.json(categorias);

        } catch (error) {

            console.error(
                'Error al consultar categorías:',
                error
            );

            res.status(500).json({
                error: 'Error al consultar categorías'
            });
        }
    }
);

// VALIDACIÓN DE DATOS DE PRODUCTOS
function validarProducto(body) {

    const {
        nombre,
        marca,
        descripcion,
        precio,
        id_categoria,
        stock,
        stock_minimo
    } = body || {};

    if (
        typeof nombre !== 'string' ||
        !nombre.trim() ||
        nombre.length > 150 ||

        !Number.isSafeInteger(id_categoria) ||
        id_categoria <= 0 ||

        !Number.isSafeInteger(stock) ||
        stock < 0 ||

        !Number.isSafeInteger(stock_minimo) ||
        stock_minimo < 0 ||

        typeof precio !== 'string' ||

        !/^(?:0|[1-9]\d{0,7})\.\d{2}$/.test(precio) ||

        Number(precio) <= 0 ||

        (
            marca != null &&
            (
                typeof marca !== 'string' ||
                marca.length > 100
            )
        ) ||

        (
            descripcion != null &&
            typeof descripcion !== 'string'
        )
    ) {

        return false;
    }

    return true;
}

// 8. AGREGAR PRODUCTO
app.post(
    '/api/admin/productos',
    adminLocal,
    async (req, res) => {

        if (!validarProducto(req.body)) {

            return res.status(400).json({
                error: 'Datos del producto inválidos'
            });
        }

        let conexion;

        try {

            conexion = await db.getConnection();

            await conexion.beginTransaction();

            const producto = req.body;

            const [categorias] = await conexion.query(
                `SELECT id_categoria
                 FROM categorias
                 WHERE id_categoria = ?`,
                [producto.id_categoria]
            );

            if (!categorias.length) {

                await conexion.rollback();

                return res.status(400).json({
                    error: 'Categoría inexistente'
                });
            }

            const [resultado] = await conexion.query(
                `INSERT INTO productos
                    (
                        id_categoria,
                        nombre,
                        marca,
                        descripcion,
                        precio,
                        imagen_url,
                        activo
                    )
                 VALUES (?, ?, ?, ?, ?, NULL, 1)`,
                [
                    producto.id_categoria,
                    producto.nombre.trim(),
                    producto.marca || null,
                    producto.descripcion || null,
                    producto.precio
                ]
            );

            await conexion.query(
                `INSERT INTO inventario
                    (
                        id_producto,
                        stock,
                        stock_minimo
                    )
                 VALUES (?, ?, ?)`,
                [
                    resultado.insertId,
                    producto.stock,
                    producto.stock_minimo
                ]
            );

            await conexion.commit();

            res.status(201).json({
                message: 'Producto creado',
                id_producto: resultado.insertId
            });

        } catch (error) {

            if (conexion) {

                try {
                    await conexion.rollback();
                } catch (_) {}
            }

            console.error(
                'Error al crear producto:',
                error
            );

            res.status(500).json({
                error: 'No se pudo crear el producto'
            });

        } finally {

            if (conexion) {
                conexion.release();
            }
        }
    }
);

// 9. EDITAR PRODUCTO Y EXISTENCIAS
app.put(
    '/api/admin/productos/:id',
    adminLocal,
    async (req, res) => {

        const id = Number(req.params.id);

        if (
            !Number.isSafeInteger(id) ||
            id <= 0 ||
            !validarProducto(req.body)
        ) {

            return res.status(400).json({
                error: 'Datos inválidos'
            });
        }

        let conexion;

        try {

            conexion = await db.getConnection();

            await conexion.beginTransaction();

            const [productos] = await conexion.query(
                `SELECT id_producto
                 FROM productos
                 WHERE id_producto = ?
                 FOR UPDATE`,
                [id]
            );

            if (!productos.length) {

                await conexion.rollback();

                return res.status(404).json({
                    error: 'Producto inexistente'
                });
            }

            const producto = req.body;

            const [categorias] = await conexion.query(
                `SELECT id_categoria
                 FROM categorias
                 WHERE id_categoria = ?`,
                [producto.id_categoria]
            );

            if (!categorias.length) {

                await conexion.rollback();

                return res.status(400).json({
                    error: 'Categoría inexistente'
                });
            }

            await conexion.query(
                `UPDATE productos
                 SET
                    id_categoria = ?,
                    nombre = ?,
                    marca = ?,
                    descripcion = ?,
                    precio = ?
                 WHERE id_producto = ?`,
                [
                    producto.id_categoria,
                    producto.nombre.trim(),
                    producto.marca || null,
                    producto.descripcion || null,
                    producto.precio,
                    id
                ]
            );

            await conexion.query(
                `INSERT INTO inventario
                    (
                        id_producto,
                        stock,
                        stock_minimo
                    )
                 VALUES (?, ?, ?)
                 ON DUPLICATE KEY UPDATE
                    stock = VALUES(stock),
                    stock_minimo = VALUES(stock_minimo)`,
                [
                    id,
                    producto.stock,
                    producto.stock_minimo
                ]
            );

            await conexion.commit();

            res.json({
                message: 'Producto actualizado'
            });

        } catch (error) {

            if (conexion) {

                try {
                    await conexion.rollback();
                } catch (_) {}
            }

            console.error(
                'Error al editar producto:',
                error
            );

            res.status(500).json({
                error: 'No se pudo editar el producto'
            });

        } finally {

            if (conexion) {
                conexion.release();
            }
        }
    }
);

// 10. ACTIVAR O DESACTIVAR PRODUCTO
app.patch(
    '/api/admin/productos/:id/activo',
    adminLocal,
    async (req, res) => {

        const id = Number(req.params.id);

        const activo = req.body?.activo;

        if (
            !Number.isSafeInteger(id) ||
            id <= 0 ||
            ![0, 1].includes(activo)
        ) {

            return res.status(400).json({
                error: 'Datos inválidos'
            });
        }

        try {

            const [resultado] = await db.query(
                `UPDATE productos
                 SET activo = ?
                 WHERE id_producto = ?`,
                [activo, id]
            );

            if (!resultado.affectedRows) {

                return res.status(404).json({
                    error: 'Producto inexistente'
                });
            }

            res.json({
                message: 'Estado actualizado'
            });

        } catch (error) {

            console.error(
                'Error al cambiar estado:',
                error
            );

            res.status(500).json({
                error: 'No se pudo cambiar el estado'
            });
        }
    }
);

// 11. CONSULTAR TODOS LOS PEDIDOS
app.get(
    '/api/admin/pedidos',
    adminLocal,
    async (req, res) => {

        try {

            const [pedidos] = await db.query(
                `SELECT
                    p.id_pedido,
                    p.fecha_pedido,
                    p.total,
                    p.estado,
                    u.nombre AS cliente,
                    u.correo
                 FROM pedidos p
                 INNER JOIN usuarios u
                    ON u.id_usuario = p.id_usuario
                 ORDER BY
                    p.fecha_pedido DESC,
                    p.id_pedido DESC`
            );

            const resultado = [];

            for (const pedido of pedidos) {

                const [productos] = await db.query(
                    `SELECT
                        d.id_producto,
                        p.nombre AS producto,
                        d.cantidad,
                        d.precio_unitario,
                        d.subtotal
                     FROM detalle_pedidos d
                     INNER JOIN productos p
                        ON p.id_producto = d.id_producto
                     WHERE d.id_pedido = ?
                     ORDER BY d.id_detalle`,
                    [pedido.id_pedido]
                );

                resultado.push({
                    ...pedido,
                    productos
                });
            }

            res.json(resultado);

        } catch (error) {

            console.error(
                'Error al consultar pedidos:',
                error
            );

            res.status(500).json({
                error: 'No se pudieron consultar los pedidos'
            });
        }
    }
);

// 12. COMPLETAR O CANCELAR PEDIDO
app.patch(
    '/api/admin/pedidos/:id/estado',
    adminLocal,
    async (req, res) => {

        const id = Number(req.params.id);

        const estado = req.body?.estado;

        if (
            !Number.isSafeInteger(id) ||
            id <= 0 ||
            !['completado', 'cancelado'].includes(estado)
        ) {

            return res.status(400).json({
                error: 'Estado inválido'
            });
        }

        let conexion;

        try {

            conexion = await db.getConnection();

            await conexion.beginTransaction();

            const [pedidos] = await conexion.query(
                `SELECT estado
                 FROM pedidos
                 WHERE id_pedido = ?
                 FOR UPDATE`,
                [id]
            );

            if (!pedidos.length) {

                await conexion.rollback();

                return res.status(404).json({
                    error: 'Pedido no encontrado'
                });
            }

            if (
                pedidos[0].estado !== 'pendiente'
            ) {

                await conexion.rollback();

                return res.status(409).json({
                    error:
                        'Solo se pueden cambiar pedidos pendientes'
                });
            }

            // Devolver existencias si se cancela.
            if (estado === 'cancelado') {

                const [detalles] = await conexion.query(
                    `SELECT
                        id_producto,
                        cantidad
                     FROM detalle_pedidos
                     WHERE id_pedido = ?
                     ORDER BY id_producto`,
                    [id]
                );

                for (const detalle of detalles) {

                    const [resultado] =
                        await conexion.query(
                            `UPDATE inventario
                             SET stock = stock + ?
                             WHERE id_producto = ?`,
                            [
                                detalle.cantidad,
                                detalle.id_producto
                            ]
                        );

                    if (!resultado.affectedRows) {

                        throw new Error(
                            'Inventario faltante'
                        );
                    }
                }
            }

            await conexion.query(
                `UPDATE pedidos
                 SET estado = ?
                 WHERE id_pedido = ?`,
                [estado, id]
            );

            await conexion.commit();

            res.json({
                message: 'Pedido actualizado',
                estado
            });

        } catch (error) {

            if (conexion) {

                try {
                    await conexion.rollback();
                } catch (_) {}
            }

            console.error(
                'Error al cambiar pedido:',
                error
            );

            res.status(500).json({
                error: 'No se pudo cambiar el pedido'
            });

        } finally {

            if (conexion) {
                conexion.release();
            }
        }
    }
);

// 13. SUBIR FOTOGRAFÍA DE PRODUCTO
app.post(
    '/api/admin/productos/:id/imagen',
    adminLocal,
    (req, res) => {

        subirImagen.single('imagen')(
            req,
            res,
            async (error) => {

                if (error) {

                    const esTamano =
                        error.code === 'LIMIT_FILE_SIZE';

                    return res.status(400).json({
                        error: esTamano
                            ? 'La imagen no debe superar 5 MB'
                            : error.message
                    });
                }

                const idProducto =
                    Number(req.params.id);

                if (
                    !Number.isSafeInteger(idProducto) ||
                    idProducto <= 0
                ) {

                    if (req.file) {

                        fs.promises.unlink(
                            req.file.path
                        ).catch(console.error);
                    }

                    return res.status(400).json({
                        error:
                            'Identificador de producto inválido'
                    });
                }

                if (!req.file) {

                    return res.status(400).json({
                        error:
                            'Debes seleccionar una imagen'
                    });
                }

                try {

                    // Ruta relativa guardada en MySQL.
                    const imagenUrl =
                        '/uploads/' + req.file.filename;

                    // Consultar imagen anterior para
                    // poder limpiarla después del cambio.
                    const [anteriores] = await db.query(
                        `SELECT imagen_url
                         FROM productos
                         WHERE id_producto = ?`,
                        [idProducto]
                    );

                    if (!anteriores.length) {

                        await fs.promises.unlink(
                            req.file.path
                        );

                        return res.status(404).json({
                            error: 'Producto no encontrado'
                        });
                    }

                    const imagenAnterior =
                        anteriores[0].imagen_url;

                    const [resultado] = await db.query(
                        `UPDATE productos
                         SET imagen_url = ?
                         WHERE id_producto = ?`,
                        [
                            imagenUrl,
                            idProducto
                        ]
                    );

                    if (!resultado.affectedRows) {

                        await fs.promises.unlink(
                            req.file.path
                        );

                        return res.status(404).json({
                            error: 'Producto no encontrado'
                        });
                    }

                    // Eliminar fotografía anterior solo
                    // si pertenece a nuestra carpeta uploads.
                    if (
                        typeof imagenAnterior === 'string' &&
                        /^\/uploads\/[a-zA-Z0-9-]+\.(jpg|png|webp)$/.test(
                            imagenAnterior
                        )
                    ) {

                        const archivoAnterior = path.join(
                            carpetaUploads,
                            path.basename(imagenAnterior)
                        );

                        fs.promises.unlink(
                            archivoAnterior
                        ).catch(() => {});
                    }

                    return res.json({
                        message:
                            'Fotografía guardada correctamente',
                        imagen_url: imagenUrl
                    });

                } catch (error) {

                    console.error(
                        'Error al guardar fotografía:',
                        error
                    );

                    try {

                        await fs.promises.unlink(
                            req.file.path
                        );

                    } catch (_) {}

                    return res.status(500).json({
                        error:
                            'No se pudo guardar la fotografía'
                    });
                }
            }
        );
    }
);

// INICIAR SERVIDOR
const PORT = process.env.PORT || 3000;

 app.listen(PORT, "0.0.0.0", () => {

     console.log(
         `Servidor corriendo en el puerto ${PORT}`
     );

     console.log(
         `Acceso local: http://localhost:${PORT}`
     );

     console.log(
         `Acceso desde la red: http://192.168.1.83:${PORT}`
     );
 });