package com.parkinglksnext

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.parkinglksnext.navigation.DrawerContent
import com.parkinglksnext.navigation.Routes
import com.parkinglksnext.repository.ParkingSpotRepository
import com.parkinglksnext.ui.theme.ParkingLKSNextTheme
import com.parkinglksnext.util.SeedData
import com.parkinglksnext.viewmodel.AuthViewModel
import com.parkinglksnext.viewmodel.HistoryViewModel
import com.parkinglksnext.viewmodel.NewReservationViewModel
import com.parkinglksnext.viewmodel.ProfileViewModel
import com.parkinglksnext.viewmodel.ReservationsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = if (authState.isAuthenticated) {
        Routes.Dashboard.route
    } else {
        Routes.Login.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    // ─── Google Sign-In via Credential Manager (no deprecated SDK) ──
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
        } catch (_: Exception) {
            // User cancelled or error — ignore silently
        }
    }

    @Suppress("DEPRECATION")
    val onGoogleSignIn: () -> Unit = {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions
            .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    // ─── Profile photo picker ────────────────────────────────────
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { _: android.net.Uri? ->
        // Photo picked — for now, the UI shows initials as placeholder.
        // The URI can be uploaded to Firebase Storage in a future iteration.
    }

    val onPhotoClick: () -> Unit = {
        photoPickerLauncher.launch("image/*")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = authState.isAuthenticated,
        drawerContent = {
            DrawerContent(
                userProfile = authState.userProfile,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo(Routes.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    authViewModel.logout()
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    ) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(Routes.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = {
                        navController.navigate(Routes.Register.route)
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Routes.Dashboard.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(Routes.ForgotPassword.route)
                    },
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

            composable(Routes.Dashboard.route) {
                ActiveReservationsScreen(
                    viewModel = reservationsViewModel,
                    onNavigateToNewReservation = {
                        navController.navigate(Routes.NewReservation.route)
                    },
                    onNavigateToEditReservation = { id ->
                        navController.navigate(Routes.EditReservation.createRoute(id))
                    },
                    onOpenMenu = { scope.launch { drawerState.open() } }
                )
            }

            composable(Routes.NewReservation.route) {
                NewReservationScreen(
                    viewModel = newReservationViewModel,
                    profileViewModel = profileViewModel,
                    onNavigateToDashboard = {
                        navController.navigate(Routes.Dashboard.route) {
                            popUpTo(Routes.Dashboard.route) { inclusive = true }
                        }
                    },
                    onOpenMenu = { scope.launch { drawerState.open() } }
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
                HistoryScreen(
                    viewModel = historyViewModel,
                    onOpenMenu = { scope.launch { drawerState.open() } }
                )
            }

            composable(Routes.Profile.route) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateToEditProfile = {
                        navController.navigate(Routes.EditProfile.route)
                    },
                    onOpenMenu = { scope.launch { drawerState.open() } },
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
        }
    }
}
