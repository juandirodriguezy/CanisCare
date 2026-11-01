package com.example.caniscare.data.model

import com.google.firebase.Timestamp

// 1. Usuarios
data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val rol: String = "cliente", // "admin" o "cliente"
    val fotoLocalPath: String = "",
    val fechaRegistro: Timestamp = Timestamp.now()
)

// 2. Perros Registrados
data class Perro(
    val id: String = "",
    val usuarioId: String = "",
    val nombre: String = "",
    val raza: String = "",
    val edadAnos: Double = 1.0,
    val tamano: String = "Mediano", // Pequeño, Mediano, Grande, Gigante
    val pesoKg: Double = 15.0,
    val fotoLocalPath: String = "",
    val fechaRegistro: Timestamp = Timestamp.now()
)

// 3. Categorías de Alimentos
data class CategoriaAlimento(
    val id: String = "",
    val nombre: String = "", // "Concentrados", "Alimentos Húmedos", "Snacks Caninos"
    val descripcion: String = ""
)

// 4. Productos
data class Producto(
    val id: String = "",
    val categoriaId: String = "",
    val categoriaNombre: String = "",
    val nombre: String = "",
    val marca: String = "",
    val descripcion: String = "",
    val presentacion: String = "", // Ej: "15 kg", "Lata 350g", "Bolsa 200g"
    val precio: Double = 0.0,
    val fotoLocalPath: String = "",
    val stockDisponible: Int = 10
)

// 5. Inventario
data class Inventario(
    val id: String = "",
    val productoId: String = "",
    val stockDisponible: Int = 0,
    val stockMinimo: Int = 2,
    val ultimaActualizacion: Timestamp = Timestamp.now()
)

// 6. Carrito de Compras
data class CarritoItem(
    val id: String = "",
    val usuarioId: String = "",
    val productoId: String = "",
    val nombreProducto: String = "",
    val marca: String = "",
    val presentacion: String = "",
    val precioUnitario: Double = 0.0,
    val cantidad: Int = 1,
    val fotoLocalPath: String = ""
)

// 7. Pedidos
data class Pedido(
    val id: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val fechaPedido: Timestamp = Timestamp.now(),
    val estado: String = "PENDIENTE", // "PENDIENTE", "PAGADO", "ENVIADO"
    val direccionEntrega: String = "",
    val ciudadEntrega: String = "",
    val subtotal: Double = 0.0,
    val costoEnvio: Double = 0.0,
    val total: Double = 0.0
)

// 8. Detalles de Pedidos
data class DetallePedido(
    val id: String = "",
    val pedidoId: String = "",
    val productoId: String = "",
    val nombreProducto: String = "",
    val cantidad: Int = 1,
    val precioUnitario: Double = 0.0,
    val subtotal: Double = 0.0
)

// 9. Pagos
data class Pago(
    val id: String = "",
    val pedidoId: String = "",
    val pasarela: String = "Sandbox Wompi/MercadoPago",
    val transaccionId: String = "",
    val metodoPago: String = "Tarjeta de Crédito (Sandbox)",
    val estado: String = "APROBADO", // "APROBADO", "RECHAZADO", "PENDIENTE"
    val monto: Double = 0.0,
    val fechaPago: Timestamp = Timestamp.now(),
    val ultimosDigitosTarjeta: String = "****"
)

// 10. Conversaciones (Chat P2P)
data class Conversacion(
    val id: String = "",
    val participante1Id: String = "",
    val participante1Nombre: String = "",
    val participante2Id: String = "",
    val participante2Nombre: String = "",
    val ultimoMensaje: String = "",
    val fechaActualizacion: Timestamp = Timestamp.now()
)

// 11. Mensajes (Chat P2P)
data class Mensaje(
    val id: String = "",
    val conversacionId: String = "",
    val remitenteId: String = "",
    val remitenteNombre: String = "",
    val contenido: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val leido: Boolean = false
)
