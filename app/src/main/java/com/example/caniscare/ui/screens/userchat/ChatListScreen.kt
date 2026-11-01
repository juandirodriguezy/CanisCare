package com.example.caniscare.ui.screens.userchat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.caniscare.data.model.Conversacion
import com.example.caniscare.data.model.Usuario
import com.example.caniscare.data.repository.UserChatRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    currentUserId: String,
    currentUserName: String,
    userChatRepository: UserChatRepository,
    onNavigateToChat: (conversacionId: String, otherUserName: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val conversations by userChatRepository.getUserConversationsFlow(currentUserId).collectAsState(initial = emptyList())
    var availableUsers by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var showNewChatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        availableUsers = userChatRepository.getAvailableUsers(currentUserId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Comunidad Canina", fontWeight = FontWeight.Bold)
                        Text("Mensajería entre Usuarios Registrados", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        availableUsers = userChatRepository.getAvailableUsers(currentUserId)
                        showNewChatDialog = true
                    }
                },
                icon = { Icon(Icons.Default.AddComment, contentDescription = null) },
                text = { Text("Nuevo Chat") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(68.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No tienes conversaciones activas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Conecta con otros dueños de mascotas y comparte experiencias caninas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        val otherUserName = if (conv.participante1Id == currentUserId) conv.participante2Nombre else conv.participante1Nombre
                        ConversationItemRow(
                            conversacion = conv,
                            otherUserName = otherUserName,
                            onClick = { onNavigateToChat(conv.id, otherUserName) }
                        )
                    }
                }
            }
        }

        if (showNewChatDialog) {
            AlertDialog(
                onDismissRequest = { showNewChatDialog = false },
                title = { Text("Iniciar Conversación") },
                text = {
                    if (availableUsers.isEmpty()) {
                        Text("No hay otros usuarios registrados en la plataforma en este momento.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(availableUsers) { targetUser ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                val convId = userChatRepository.getOrCreateConversation(
                                                    currentUserId = currentUserId,
                                                    currentUserName = currentUserName,
                                                    targetUserId = targetUser.id,
                                                    targetUserName = targetUser.nombre
                                                )
                                                showNewChatDialog = false
                                                onNavigateToChat(convId, targetUser.nombre)
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(targetUser.nombre, fontWeight = FontWeight.Bold)
                                        Text(targetUser.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showNewChatDialog = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}

@Composable
private fun ConversationItemRow(
    conversacion: Conversacion,
    otherUserName: String,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(conversacion.fechaActualizacion) {
        dateFormat.format(conversacion.fechaActualizacion.toDate())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = otherUserName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = conversacion.ultimoMensaje,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 82.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
