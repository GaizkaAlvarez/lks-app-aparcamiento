package com.parkinglksnext

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkinglksnext.network.SpotRecommendationDto
import com.parkinglksnext.ui.theme.*
import com.parkinglksnext.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    var inputText by remember { mutableStateOf("") }

    // Auto-scroll to bottom when new messages arrive or loading starts
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    Scaffold(
        containerColor = ParklyBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ParklySurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = ParklyTextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "Asistente IA",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ParklyTextPrimary
                        )
                        Text(
                            text = "Buscar Plaza",
                            fontSize = 12.sp,
                            color = ParklyTextSecondary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Messages list ──────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 4 }
                    ) {
                        MessageBubble(
                            message = message,
                            onReserve = { spot -> viewModel.reserveSpot(spot) },
                            onEdit = { spot ->
                                // Pre-fill input to let user refine via chat
                                inputText = "Quiero cambiar la reserva de la plaza ${spot.number}: "
                            },
                            onRetry = { viewModel.retryLastMessage() }
                        )
                    }
                }

                // Typing indicator
                if (uiState.isLoading) {
                    item(key = "typing") {
                        TypingIndicator()
                    }
                }
            }

            // ── Input bar ──────────────────────────────────────
            InputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = !uiState.isLoading
            )
        }
    }
}

// ─── Message Bubble ──────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: ChatViewModel.ChatMessage,
    onReserve: (SpotRecommendationDto) -> Unit,
    onEdit: (SpotRecommendationDto) -> Unit,
    onRetry: () -> Unit
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = when {
        message.isError -> ParklyRedLight
        message.isSuccess -> ParklyGreenLight
        message.isUser -> ParklyOrange
        else -> ParklySurface
    }
    val textColor = when {
        message.isError -> ParklyRed
        message.isSuccess -> ParklyGreen
        message.isUser -> Color.White
        else -> ParklyTextPrimary
    }
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (message.isUser) 16.dp else 4.dp,
        bottomEnd = if (message.isUser) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = bgColor,
            shadowElevation = if (message.isUser) 0.dp else 1.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Show retry button for errors
                if (message.isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ParklyRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reintentar", color = ParklyRed, fontSize = 12.sp)
                    }
                }

                // Show recommendation cards
                if (message.recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    message.recommendations.forEach { spot ->
                        SpotRecommendationCard(
                            spot = spot,
                            onReserve = { onReserve(spot) },
                            onEdit = { onEdit(spot) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

// ─── Spot Recommendation Card ────────────────────────────────────

@Composable
private fun SpotRecommendationCard(
    spot: SpotRecommendationDto,
    onReserve: () -> Unit,
    onEdit: () -> Unit
) {
    val typeEmoji = when (spot.type) {
        "electric" -> "⚡"
        "motorcycle" -> "🏍"
        else -> "🚗"
    }
    val typeLabel = when (spot.type) {
        "electric" -> "Con cargador"
        "motorcycle" -> "Moto"
        else -> "Común"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(ParklyOrange.copy(alpha = 0.4f))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spot number badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ParklyOrangeLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%02d", spot.number),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ParklyOrange
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Plaza ${spot.number}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = ParklyTextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(typeEmoji, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = typeLabel,
                        fontSize = 12.sp,
                        color = ParklyTextSecondary
                    )
                }
            }

            // Action buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onReserve,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ParklyOrange),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        "Reservar",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        "Modificar",
                        color = ParklyTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─── Typing Indicator ────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    var dotCount by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(400)
            dotCount = (dotCount % 3) + 1
        }
    }

    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
            .background(ParklySurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Pensando${".".repeat(dotCount)}",
            fontSize = 13.sp,
            color = ParklyTextSecondary
        )
    }
}

// ─── Input Bar ───────────────────────────────────────────────────

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Escribe tu mensaje...",
                        color = ParklyTextSecondary.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ParklyOrange,
                    unfocusedBorderColor = Color(0xFFE2E4ED),
                    focusedContainerColor = ParklyBackground,
                    unfocusedContainerColor = ParklyBackground
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            )

            // Send button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank() && enabled) ParklyOrange
                        else Color(0xFFE2E4ED)
                    )
                    .clickable(enabled = enabled && text.isNotBlank()) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (text.isNotBlank() && enabled) Color.White else ParklyTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
