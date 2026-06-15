package com.parkinglksnext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    // ─── Seed parking spots on first launch ─────────────────────
    LaunchedEffect(Unit) {
        SeedData.ensureParkingSpotsSeeded(ParkingSpotRepository())
    }

    // ─── Activity-scoped ViewModels (shared across screens) ──────
    val authViewModel: AuthViewModel = viewModel()
    val reservationsViewModel: ReservationsViewModel = viewModel()
    val newReservationViewModel: NewReservationViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    // ─── Navigation ──────────────────────────────────────────────
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Auth state determines start destination
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = if (authState.isAuthenticated) {
        Routes.Dashboard.route
    } else {
        Routes.Login.route
    }

    // Current route for drawer highlighting
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                userProfile = authState.userProfile,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo(Routes.Dashboard.route) {
                            saveState = true
                        }
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
        // ─── NavHost: all 9 routes ──────────────────────────────
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            // ── Auth routes ─────────────────────────────────────
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
                    }
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

            // ── Main routes ─────────────────────────────────────
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
                    onNavigateBack = { navController.popBackStack() },
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
                    authViewModel = authViewModel,
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
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
