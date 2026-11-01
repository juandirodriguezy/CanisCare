package com.example.caniscare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.caniscare.data.repository.*
import com.example.caniscare.ui.navigation.Screen
import com.example.caniscare.ui.screens.aichat.DogAIChatScreen
import com.example.caniscare.ui.screens.auth.LoginScreen
import com.example.caniscare.ui.screens.auth.RegisterScreen
import com.example.caniscare.ui.screens.calculator.CalculatorScreen
import com.example.caniscare.ui.screens.cart.CartScreen
import com.example.caniscare.ui.screens.cart.OrderHistoryScreen
import com.example.caniscare.ui.screens.maps.VetMapScreen
import com.example.caniscare.ui.screens.marketplace.MarketplaceScreen
import com.example.caniscare.ui.screens.profile.MyDogsScreen
import com.example.caniscare.ui.screens.profile.ProfileScreen
import com.example.caniscare.ui.screens.userchat.ChatConversationScreen
import com.example.caniscare.ui.screens.userchat.ChatListScreen
import com.example.caniscare.ui.theme.CanisCareTheme

class MainActivity : ComponentActivity() {

    private val authRepository = AuthRepository()
    private val dogRepository = DogRepository()
    private val marketplaceRepository = MarketplaceRepository()
    private val cartRepository = CartRepository()
    private val paymentRepository = PaymentRepository()
    private val veterinariaRepository = VeterinariaRepository()
    private val geminiAIRepository = GeminiAIRepository()
    private val userChatRepository = UserChatRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CanisCareTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val configuration = LocalConfiguration.current
                val isCompactScreen = configuration.screenWidthDp < 400

                var currentUserProfile by remember { mutableStateOf<com.example.caniscare.data.model.Usuario?>(null) }

                LaunchedEffect(authRepository.currentUser?.uid) {
                    if (authRepository.isUserLoggedIn) {
                        authRepository.getCurrentUserProfile().onSuccess {
                            currentUserProfile = it
                        }
                    } else {
                        currentUserProfile = null
                    }
                }

                val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                tonalElevation = 4.dp
                            ) {
                                Screen.bottomNavItems.forEach { screen ->
                                    val selected = currentRoute == screen.route
                                    NavigationBarItem(
                                        selected = selected,
                                        alwaysShowLabel = !isCompactScreen || selected,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            screen.icon?.let {
                                                Icon(it, contentDescription = screen.title)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = screen.title,
                                                fontSize = if (isCompactScreen) 9.sp else 10.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = if (authRepository.isUserLoggedIn) Screen.Calculator.route else Screen.Login.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 1. Auth: Login
                            composable(Screen.Login.route) {
                                LoginScreen(
                                    authRepository = authRepository,
                                    onLoginSuccess = {
                                        navController.navigate(Screen.Calculator.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    },
                                    onNavigateToRegister = {
                                        navController.navigate(Screen.Register.route)
                                    }
                                )
                            }

                            // 2. Auth: Registro
                            composable(Screen.Register.route) {
                                RegisterScreen(
                                    authRepository = authRepository,
                                    onRegisterSuccess = {
                                        navController.navigate(Screen.Calculator.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    },
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            // 3. Calculadora Canina & App Info (Punto 1 y 2)
                            composable(Screen.Calculator.route) {
                                CalculatorScreen(
                                    onNavigateToSaveDog = { name, age, size ->
                                        navController.navigate(Screen.MyDogs.route)
                                    }
                                )
                            }

                            // 4. Marketplace Canino (Punto 6)
                            composable(Screen.Marketplace.route) {
                                val uid = authRepository.currentUser?.uid ?: "guest"
                                MarketplaceScreen(
                                    currentUserId = uid,
                                    userRole = currentUserProfile?.rol ?: "cliente",
                                    marketplaceRepository = marketplaceRepository,
                                    cartRepository = cartRepository,
                                    onNavigateToCart = {
                                        navController.navigate(Screen.Cart.route)
                                    }
                                )
                            }

                            // 5. Carrito de Compras & Pasarela Sandbox (Puntos 7 y 8)
                            composable(Screen.Cart.route) {
                                val uid = authRepository.currentUser?.uid ?: "guest"
                                val uName = currentUserProfile?.nombre ?: "Usuario"
                                val address = currentUserProfile?.direccion ?: ""
                                CartScreen(
                                    currentUserId = uid,
                                    currentUserName = uName,
                                    userInitialAddress = address,
                                    cartRepository = cartRepository,
                                    paymentRepository = paymentRepository,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    onNavigateToHistory = {
                                        navController.navigate(Screen.OrderHistory.route)
                                    }
                                )
                            }

                            // 6. Historial de Pedidos
                            composable(Screen.OrderHistory.route) {
                                val uid = authRepository.currentUser?.uid ?: "guest"
                                OrderHistoryScreen(
                                    currentUserId = uid,
                                    paymentRepository = paymentRepository,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            // 7. Mapa de Veterinarias & Urgencias 24H (Punto 5)
                            composable(Screen.VetMap.route) {
                                VetMapScreen(
                                    veterinariaRepository = veterinariaRepository
                                )
                            }

                            // 8. Chat con IA CanisBot (Punto 4)
                            composable(Screen.AIChat.route) {
                                DogAIChatScreen(
                                    geminiAIRepository = geminiAIRepository,
                                    onNavigateToEmergencies = {
                                        navController.navigate(Screen.VetMap.route)
                                    }
                                )
                            }

                            // 9. Comunidad / Chat P2P entre Usuarios (Punto 9)
                            composable(Screen.UserChat.route) {
                                val uid = authRepository.currentUser?.uid ?: "guest"
                                val uName = currentUserProfile?.nombre ?: "Usuario"
                                ChatListScreen(
                                    currentUserId = uid,
                                    currentUserName = uName,
                                    userChatRepository = userChatRepository,
                                    onNavigateToChat = { convId, otherName ->
                                        navController.navigate(Screen.ChatConversation.createRoute(convId, otherName))
                                    }
                                )
                            }

                            // 10. Conversación P2P 1 a 1
                            composable(
                                route = Screen.ChatConversation.route,
                                arguments = listOf(
                                    navArgument("conversacionId") { type = NavType.StringType },
                                    navArgument("otherUserName") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val convId = backStackEntry.arguments?.getString("conversacionId") ?: ""
                                val otherName = backStackEntry.arguments?.getString("otherUserName") ?: "Chat"
                                val uid = authRepository.currentUser?.uid ?: "guest"
                                val uName = currentUserProfile?.nombre ?: "Usuario"

                                ChatConversationScreen(
                                    conversacionId = convId,
                                    otherUserName = otherName,
                                    currentUserId = uid,
                                    currentUserName = uName,
                                    userChatRepository = userChatRepository,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            // 11. Perfil del Usuario (Punto 3)
                            composable(Screen.Profile.route) {
                                ProfileScreen(
                                    authRepository = authRepository,
                                    onNavigateToMyDogs = {
                                        navController.navigate(Screen.MyDogs.route)
                                    },
                                    onLogout = {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // 12. Mis Perros Registrados (Punto 3)
                            composable(Screen.MyDogs.route) {
                                val uid = authRepository.currentUser?.uid ?: "guest"
                                MyDogsScreen(
                                    currentUserId = uid,
                                    dogRepository = dogRepository,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
