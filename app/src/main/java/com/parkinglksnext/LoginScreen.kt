package com.parkinglksnext

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkinglksnext.ui.theme.*
import com.parkinglksnext.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onNavigateToDashboard()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParklyBackground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // ── Logo LKS Next ────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_lks),
                contentDescription = "Logo LKS Next",
                modifier = Modifier
                    .width(200.dp)
                    .padding(bottom = 16.dp)
            )
            Text(
                text = "Bienvenido/a de nuevo",
                fontSize = 14.sp,
                color = ParklyTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── Email field ───────────────────────────────────────────
        Text(
            text = "Correo electrónico",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = ParklyTextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("tu@empresa.com", color = ParklyTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ParklyOrange,
                unfocusedBorderColor = Color(0xFFE2E4ED),
                focusedContainerColor = ParklySurface,
                unfocusedContainerColor = ParklySurface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Password field ────────────────────────────────────────
        Text(
            text = "Contraseña",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = ParklyTextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••", color = ParklyTextSecondary) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = ParklyTextSecondary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ParklyOrange,
                unfocusedBorderColor = Color(0xFFE2E4ED),
                focusedContainerColor = ParklySurface,
                unfocusedContainerColor = ParklySurface
            ),
            singleLine = true
        )

        TextButton(
            onClick = {
                viewModel.clearError()
                onNavigateToForgotPassword()
            },
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
        ) {
            Text(
                text = "¿Olvidaste tu contraseña?",
                color = ParklyOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Error ─────────────────────────────────────────────────
        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ParklyRedLight)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = uiState.error ?: "",
                    color = ParklyRed,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Sign In button ────────────────────────────────────────
        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    viewModel.login(email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !uiState.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = ParklyOrange),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Iniciar Sesión",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Divider ───────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E4ED))
            Text(
                text = "  o  ",
                color = ParklyTextSecondary,
                fontSize = 13.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E4ED))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Google button ─────────────────────────────────────────
        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !uiState.isLoading,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ParklySurface
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4ED))
        ) {
            Text(
                text = "Continuar con Google",
                color = ParklyTextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Register link ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¿No tienes cuenta? ",
                color = ParklyTextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = "Regístrate",
                color = ParklyOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable(enabled = !uiState.isLoading) { onNavigateToRegister() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
