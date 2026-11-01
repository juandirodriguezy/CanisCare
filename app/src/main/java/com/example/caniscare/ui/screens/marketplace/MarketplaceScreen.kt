package com.example.caniscare.ui.screens.marketplace

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.caniscare.data.local.LocalPhotoManager
import com.example.caniscare.data.model.Producto
import com.example.caniscare.data.repository.CartRepository
import com.example.caniscare.data.repository.MarketplaceRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    currentUserId: String,
    userRole: String,
    marketplaceRepository: MarketplaceRepository,
    cartRepository: CartRepository,
    onNavigateToCart: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allProducts by marketplaceRepository.getProductsFlow().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var showAdminDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Producto?>(null) }

    LaunchedEffect(Unit) {
        marketplaceRepository.seedInitialCanineProductsIfNeeded()
    }

    val categories = listOf("Todos", "Concentrados", "Alimentos Húmedos", "Snacks Caninos")

    val filteredProducts = remember(allProducts, searchQuery, selectedCategory) {
        allProducts.filter { prod ->
            val matchesCategory = selectedCategory == "Todos" || prod.categoriaNombre.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    prod.nombre.contains(searchQuery, ignoreCase = true) ||
                    prod.marca.contains(searchQuery, ignoreCase = true) ||
                    prod.descripcion.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tienda Canina", fontWeight = FontWeight.Bold)
                        Text(
                            "Alimentos y Nutrición 100% Canina",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCart) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    productToEdit = null
                    showAdminDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo Alimento") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp)
        ) {
            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar alimento, marca o snack...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Filtro por categorías caninas con Scroll Horizontal Responsivo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp, maxLines = 1) }
                    )
                }
            }

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron alimentos en esta categoría.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Cuadrícula Adaptativa: 1-2 columnas en móviles, 3+ en tablets/landscape
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductItemCard(
                            producto = product,
                            canManage = userRole == "admin" || true,
                            onAddToCart = {
                                scope.launch {
                                    val res = cartRepository.addToCart(currentUserId, product)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "${product.nombre} añadido al carrito", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error al agregar al carrito", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onEdit = {
                                productToEdit = product
                                showAdminDialog = true
                            },
                            onDelete = {
                                scope.launch {
                                    marketplaceRepository.deleteProduct(product.id)
                                    Toast.makeText(context, "Producto eliminado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }

        if (showAdminDialog) {
            AdminProductDialog(
                productoToEdit = productToEdit,
                onDismiss = { showAdminDialog = false },
                onSaveProduct = { prod ->
                    scope.launch {
                        marketplaceRepository.saveProduct(prod)
                        showAdminDialog = false
                        Toast.makeText(context, "Producto guardado con éxito", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun ProductItemCard(
    producto: Producto,
    canManage: Boolean,
    onAddToCart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Imagen con altura responsiva
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (LocalPhotoManager.fileExists(producto.fotoLocalPath)) {
                    AsyncImage(
                        model = File(producto.fotoLocalPath),
                        contentDescription = producto.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = producto.marca.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = producto.presentacion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = currencyFormat.format(producto.precio),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (producto.stockDisponible > 0) "Stock: ${producto.stockDisponible} un." else "Agotado",
                style = MaterialTheme.typography.labelSmall,
                color = if (producto.stockDisponible > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddToCart,
                enabled = producto.stockDisponible > 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text(if (producto.stockDisponible > 0) "Agregar" else "Sin Stock", fontSize = 12.sp)
            }

            if (canManage) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
