package com.example.caniscare.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    // Auth
    object Login : Screen("login", "Iniciar Sesión")
    object Register : Screen("register", "Registro")

    // Barra Principal
    object Calculator : Screen("calculator", "Calculadora", Icons.Default.Calculate)
    object Marketplace : Screen("marketplace", "Tienda", Icons.Default.Storefront)
    object VetMap : Screen("vetmap", "Veterinarias", Icons.Default.LocationOn)
    object AIChat : Screen("aichat", "CanisBot IA", Icons.Default.SmartToy)
    object UserChat : Screen("userchat", "Comunidad", Icons.Default.Chat)
    object Profile : Screen("profile", "Mi Perfil", Icons.Default.Person)

    // Flujos Secundarios
    object Cart : Screen("cart", "Carrito")
    object OrderHistory : Screen("order_history", "Historial de Pedidos")
    object MyDogs : Screen("my_dogs", "Mis Perros")
    object ChatConversation : Screen("chat_conversation/{conversacionId}/{otherUserName}", "Chat") {
        fun createRoute(conversacionId: String, otherUserName: String) = "chat_conversation/$conversacionId/$otherUserName"
    }

    companion object {
        val bottomNavItems = listOf(
            Calculator,
            Marketplace,
            VetMap,
            AIChat,
            UserChat,
            Profile
        )
    }
}
