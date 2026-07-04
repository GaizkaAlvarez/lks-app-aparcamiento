package com.parkinglksnext

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.parkinglksnext.navigation.Routes
import com.parkinglksnext.repository.ParkingSpotRepository
import com.parkinglksnext.ui.theme.ParkingLKSNextTheme
import com.parkinglksnext.ui.theme.ParklyOrange
import com.parkinglksnext.ui.theme.ParklyTextSecondary
import com.parkinglksnext.util.NotificationHelper
import com.parkinglksnext.util.SeedData
import com.parkinglksnext.viewmodel.AuthViewModel
import com.parkinglksnext.viewmodel.ChatViewModel
import com.parkinglksnext.viewmodel.HistoryViewModel
import com.parkinglksnext.viewmodel.NewReservationViewModel
import com.parkinglksnext.viewmodel.ProfileViewModel
import com.parkinglksnext.viewmodel.ReservationsViewModel
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        }
        enableEdgeToEdge()
        setContent {
            ParkingLKSNextTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    LaunchedEffect(Unit) {
        SeedData.ensureParkingSpotsSeeded(ParkingSpotRepository())
    }

    val authViewModel: AuthViewModel = viewModel()
    val reservationsViewModel: ReservationsViewModel = viewModel()
    val newReservationViewModel: NewReservationViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()

    val navController = rememberNavController()
    val context = LocalContext.current

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = if (authState.isAuthenticated) {
        Routes.Home.route
    } else {
        Routes.Login.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    // Bottom nav tabs
    val bottomNavRoutes = setOf(
        Routes.Home.route,
        Routes.Dashboard.route,
        Routes.History.route,
        Routes.Profile.route
    )

    // ─── Google Sign-In ──────────────────────────────────────────
    @Suppress("DEPRECATION")
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn
                .getSignedInAccountFromIntent(result.data)
            val account = task.getResult(
                com.google.android.gms.common.api.ApiException::class.java
            )
            account.idToken?.let { idToken ->
                authViewModel.signInWithGoogle(idToken)
            }
        } catch (_: Exception) { }
    }

    @Suppress("DEPRECATION")
    val onGoogleSignIn: () -> Unit = {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions
            .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    // ─── Profile photo picker ────────────────────────────────────
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { imageUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val maxWidth = 800
                    val resized = if (bitmap.width > maxWidth) {
                        val ratio = maxWidth.toFloat() / bitmap.width
                        val newHeight = (bitmap.height * ratio).toInt()
                        android.graphics.Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
                    } else bitmap
                    val outputStream = ByteArrayOutputStream()
                    resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream)
                    val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    outputStream.close()
                    profileViewModel.saveProfilePhotoBase64(base64String)
                    if (resized != bitmap) resized.recycle()
                    bitmap.recycle()
                }
            } catch (_: Exception) { }
        }
    }

    val onPhotoClick: () -> Unit = { photoPickerLauncher.launch("image/*") }

    // ─── Navigate to tab helper ──────────────────────────────────
    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.Home.route,
                        onClick = { navigateToTab(Routes.Home.route) },
                        icon = {
                            Icon(
                                if (currentRoute == Routes.Home.route) Icons.Filled.Home
                                else Icons.Outlined.Home,
                                contentDescription = "Inicio"
                            )
                        },
                        label = {
                            Text(
                                "Inicio",
                                fontSize = 11.sp,
                                fontWeight = if (currentRoute == Routes.Home.route) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ParklyOrange,
                            selectedTextColor = ParklyOrange,
                            unselectedIconColor = ParklyTextSecondary,
                            unselectedTextColor = ParklyTextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.Dashboard.route,
                        onClick = { navigateToTab(Routes.Dashboard.route) },
                        icon = {
                            Icon(
                                if (currentRoute == Routes.Dashboard.route) Icons.Filled.Bookmark
                                else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Mis Reservas"
                            )
                        },
                        label = {
                            Text(
                                "Mis Reservas",
                                fontSize = 11.sp,
                                fontWeight = if (currentRoute == Routes.Dashboard.route) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ParklyOrange,
                            selectedTextColor = ParklyOrange,
                            unselectedIconColor = ParklyTextSecondary,
                            unselectedTextColor = ParklyTextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.History.route,
                        onClick = { navigateToTab(Routes.History.route) },
                        icon = {
                            Icon(
                                if (currentRoute == Routes.History.route) Icons.Filled.History
                                else Icons.Outlined.History,
                                contentDescription = "Historial"
                            )
                        },
                        label = {
                            Text(
                                "Historial",
                                fontSize = 11.sp,
                                fontWeight = if (currentRoute == Routes.History.route) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ParklyOrange,
                            selectedTextColor = ParklyOrange,
                            unselectedIconColor = ParklyTextSecondary,
                            unselectedTextColor = ParklyTextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.Profile.route || currentRoute == Routes.EditProfile.route,
                        onClick = { navigateToTab(Routes.Profile.route) },
                        icon = {
                            Icon(
                                if (currentRoute == Routes.Profile.route || currentRoute == Routes.EditProfile.route) Icons.Filled.Person
                                else Icons.Outlined.Person,
                                contentDescription = "Perfil"
                            )
                        },
                        label = {
                            Text(
                                "Perfil",
                                fontSize = 11.sp,
                                fontWeight = if (currentRoute == Routes.Profile.route) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ParklyOrange,
                            selectedTextColor = ParklyOrange,
                            unselectedIconColor = ParklyTextSecondary,
                            unselectedTextColor = ParklyTextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate(Routes.Register.route) },
                    onNavigateToDashboard = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToForgotPassword = { navController.navigate(Routes.ForgotPassword.route) },
                    onGoogleSignIn = onGoogleSignIn
                )
            }

            composable(Routes.Register.route) {
                RegisterScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ForgotPassword.route) {
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.Home.route) {
                HomeScreen(
                    reservationsViewModel = reservationsViewModel,
                    profileViewModel = profileViewModel,
                    onNavigateToNewReservation = { navController.navigate(Routes.NewReservation.route) },
                    onNavigateToReservations = { navigateToTab(Routes.Dashboard.route) },
                    onNavigateToChatBot = { navController.navigate(Routes.ChatBot.route) }
                )
            }

            composable(Routes.Dashboard.route) {
                ActiveReservationsScreen(
                    viewModel = reservationsViewModel,
                    onNavigateToNewReservation = { navController.navigate(Routes.NewReservation.route) },
                    onNavigateToEditReservation = { id ->
                        navController.navigate(Routes.EditReservation.createRoute(id))
                    }
                )
            }

            composable(Routes.NewReservation.route) {
                NewReservationScreen(
                    viewModel = newReservationViewModel,
                    profileViewModel = profileViewModel,
                    onNavigateToDashboard = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Home.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.EditReservation.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                EditReservationScreen(
                    idReserva = id,
                    viewModel = reservationsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.History.route) {
                HistoryScreen(viewModel = historyViewModel)
            }

            composable(Routes.Profile.route) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateToEditProfile = { navController.navigate(Routes.EditProfile.route) },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.EditProfile.route) {
                EditProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onPhotoClick = onPhotoClick
                )
            }

            composable(Routes.ChatBot.route) {
                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
