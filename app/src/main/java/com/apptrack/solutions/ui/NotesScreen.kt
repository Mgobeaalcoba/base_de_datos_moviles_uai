package com.apptrack.solutions.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.apptrack.solutions.data.NoteViewModel
import com.apptrack.solutions.model.Note
import com.apptrack.solutions.model.Tag
import com.apptrack.solutions.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    modifier: Modifier = Modifier,
    noteViewModel: NoteViewModel,
    onSignOut: () -> Unit
) {
    val notes by noteViewModel.notes.collectAsState()
    val users by noteViewModel.users.collectAsState()
    val selectedUser by noteViewModel.selectedUser.collectAsState()
    val isLoading by noteViewModel.isLoading.collectAsState()
    val errorMessage by noteViewModel.errorMessage.collectAsState()

    var showUserSelector by remember { mutableStateOf(selectedUser == null || users.size > 1) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedUser) {
        if (selectedUser != null && users.size == 1) {
            showUserSelector = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header con información del usuario y opción de logout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mis Notas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                selectedUser?.let { user ->
                    Text(
                        text = "Usuario: ${user.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                if (selectedUser != null) {
                    IconButton(onClick = { showUserSelector = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Cambiar usuario")
                    }
                }
                TextButton(onClick = onSignOut) {
                    Text("Cerrar sesión")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de usuario (se muestra si no hay usuario seleccionado o hay múltiples usuarios)
        if (showUserSelector && users.isNotEmpty()) {
            UserSelectorSection(
                users = users,
                selectedUser = selectedUser,
                onUserSelected = { user ->
                    noteViewModel.selectUser(user)
                    showUserSelector = false
                }
            )
        } else if (selectedUser != null) {
            // Botón para agregar nota (solo visible cuando hay un usuario seleccionado)
            Button(
                onClick = { showAddNoteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Nota")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de notas
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (notes.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes notas aún.\n¡Agrega tu primera nota!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notes) { note ->
                        NoteCard(
                            note = note,
                            onDeleteClick = { noteViewModel.deleteNote(note) }
                        )
                    }
                }
            }
        }

        // Mostrar errores
        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }

    // Dialog para agregar nota
    if (showAddNoteDialog) {
        AddNoteDialog(
            viewModel = noteViewModel,
            onDismiss = { showAddNoteDialog = false },
            onNoteAdded = { showAddNoteDialog = false }
        )
    }
}

@Composable
private fun UserSelectorSection(
    users: List<User>,
    selectedUser: User?,
    onUserSelected: (User) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (selectedUser == null) "Selecciona tu usuario para continuar:" else "Cambiar usuario:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            users.forEach { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUserSelected(user) }
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (user == selectedUser)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun NoteCard(
    note: Note,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (note.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar nota",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNoteDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit,
    onNoteAdded: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf("") }
    var newTagText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<List<Tag>>(emptyList()) }

    val isLoading by viewModel.isLoading.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Nueva Nota",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo título
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo contenido
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Contenido") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo adjunto
                OutlinedTextField(
                    value = attachmentUri,
                    onValueChange = { attachmentUri = it },
                    label = { Text("URL del adjunto (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sección de etiquetas
                Text(
                    text = "Etiquetas para esta nota:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mostrar solo las etiquetas seleccionadas para esta nota
                if (selectedTags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedTags) { tag ->
                            TagChip(
                                tag = tag,
                                onRemove = {
                                    selectedTags = selectedTags.filter { it.id != tag.id }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Campo para agregar nueva etiqueta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        label = { Text("Nueva etiqueta") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                    Button(
                        onClick = {
                            if (newTagText.isNotBlank()) {
                                val newTag = viewModel.createTag(newTagText)
                                if (newTag != null && !selectedTags.any { it.id == newTag.id }) {
                                    selectedTags = selectedTags + newTag
                                }
                                newTagText = ""
                            }
                        },
                        enabled = !isLoading && newTagText.isNotBlank()
                    ) {
                        Text("Agregar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.createNote(
                                title = title,
                                content = content,
                                selectedTags = selectedTags,
                                attachmentUri = attachmentUri.takeIf { it.isNotBlank() }
                            )
                            onNoteAdded()
                        },
                        enabled = !isLoading && title.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Crear Nota")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: Tag,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodySmall,
                color = Color(android.graphics.Color.parseColor(tag.color))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.Delete,
                contentDescription = "Quitar etiqueta",
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onRemove() },
                tint = Color(android.graphics.Color.parseColor(tag.color))
            )
        }
    }
}
