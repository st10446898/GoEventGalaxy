package com.goevent.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.goevent.ui.components.GalaxyBottomBar
import com.goevent.ui.components.NavItem
import com.goevent.ui.screens.AboutScreen
import com.goevent.ui.screens.AchievementsScreen
import com.goevent.ui.screens.ChangePasswordScreen
import com.goevent.ui.screens.DigitalTicketScreen
import com.goevent.ui.screens.EditProfileScreen
import com.goevent.ui.screens.EventDetailsScreen
import com.goevent.ui.screens.EventMapScreen
import com.goevent.ui.screens.ExploreScreen
import com.goevent.ui.screens.ForgotPasswordScreen
import com.goevent.ui.screens.GoPointsHistoryScreen
import com.goevent.ui.screens.GoPointsScreen
import com.goevent.ui.screens.HelpScreen
import com.goevent.ui.screens.HomeScreen
import com.goevent.ui.screens.InterestSelectionScreen
import com.goevent.ui.screens.LanguageScreen
import com.goevent.ui.screens.MyTicketsScreen
import com.goevent.ui.screens.NotificationsScreen
import com.goevent.ui.screens.OnboardingScreen
import com.goevent.ui.screens.ProfileScreen
import com.goevent.ui.screens.QrViewScreen
import com.goevent.ui.screens.RegisterScreen
import com.goevent.ui.screens.RewardsScreen
import com.goevent.ui.screens.SavedScreen
import com.goevent.ui.screens.SettingsScreen
import com.goevent.ui.screens.SignInScreen
import com.goevent.ui.screens.SplashScreen
import com.goevent.ui.screens.booking.BookingFlowScreen
import com.goevent.ui.theme.cosmic
import com.goevent.ui.viewmodel.FavoritesViewModel
import com.goevent.ui.viewmodel.SessionViewModel
import com.goevent.ui.viewmodel.appViewModel

private val bottomItems = listOf(
    NavItem(Routes.HOME, "Home", Icons.Rounded.Home),
    NavItem(Routes.EXPLORE, "Explore", Icons.Rounded.Explore),
    NavItem(Routes.TICKETS, "Tickets", Icons.Rounded.ConfirmationNumber),
    NavItem(Routes.SAVED, "Saved", Icons.Rounded.Favorite),
    NavItem(Routes.PROFILE, "Profile", Icons.Rounded.Person)
)

private fun NavHostController.clearAndGo(route: String) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

private fun NavHostController.goBottom(route: String) = navigate(route) {
    popUpTo(Routes.HOME) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun MainAppNavigation(session: SessionViewModel) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val favorites = appViewModel { FavoritesViewModel(it) }
    val ss by session.state.collectAsState()
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route?.substringBefore('?')

    // Saved events follow whoever is signed in.
    LaunchedEffect(ss.profile?.id) { if (ss.profile != null) favorites.reload() }

    // Signed-out users can only see the public screens.
    LaunchedEffect(ss.profile, ss.restoring, route) {
        if (!ss.restoring && ss.profile == null && route != null && route !in Routes.publicRoutes) {
            nav.clearAndGo(Routes.SIGN_IN)
        }
    }

    Scaffold(
        containerColor = cosmic.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (route in Routes.bottomRoutes) GalaxyBottomBar(route, bottomItems) { nav.goBottom(it.route) }
        }
    ) { inner ->
        NavHost(nav, startDestination = Routes.SPLASH, modifier = Modifier.padding(bottom = inner.calculateBottomPadding())) {

            // ------------------------------------------------ start and auth
            composable(Routes.SPLASH) { SplashScreen(session) { nav.clearAndGo(it) } }
            composable(Routes.ONBOARDING) { OnboardingScreen(session) { nav.clearAndGo(Routes.SIGN_IN) } }
            composable(Routes.SIGN_IN) {
                SignInScreen(session, onSuccess = { nav.clearAndGo(Routes.HOME) }, onRegister = { nav.navigate(Routes.REGISTER) }, onForgot = { nav.navigate(Routes.FORGOT_PASSWORD) })
            }
            composable(Routes.REGISTER) {
                RegisterScreen(session, onSuccess = { nav.clearAndGo(Routes.INTERESTS) }, onSignIn = { nav.popBackStack() })
            }
            composable(Routes.FORGOT_PASSWORD) { ForgotPasswordScreen(session) { nav.popBackStack() } }
            composable(Routes.CHANGE_PASSWORD) { ChangePasswordScreen(session) { nav.popBackStack() } }
            composable(Routes.INTERESTS) {
                InterestSelectionScreen {
                    if (nav.previousBackStackEntry == null) nav.clearAndGo(Routes.HOME) else nav.popBackStack()
                }
            }

            // ------------------------------------------------ main tabs
            composable(Routes.HOME) {
                HomeScreen(
                    session, favorites,
                    onEvent = { nav.navigate(Routes.event(it)) },
                    onExplore = { category ->
                        if (category == null) nav.goBottom(Routes.EXPLORE)
                        else nav.navigate(Routes.explore(category)) { popUpTo(Routes.HOME); launchSingleTop = true }
                    },
                    onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                    onProfile = { nav.goBottom(Routes.PROFILE) },
                    onRewards = { nav.navigate(Routes.GOPOINTS) }
                )
            }
            composable(
                "explore?category={category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { entry ->
                ExploreScreen(favorites, entry.arguments?.getString("category")) { nav.navigate(Routes.event(it)) }
            }
            composable(Routes.TICKETS) {
                MyTicketsScreen(session, onTicket = { nav.navigate(Routes.ticket(it)) }, onExplore = { nav.goBottom(Routes.EXPLORE) })
            }
            composable(Routes.SAVED) {
                SavedScreen(favorites, onEvent = { nav.navigate(Routes.event(it)) }, onExplore = { nav.goBottom(Routes.EXPLORE) })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    session,
                    onEdit = { nav.navigate(Routes.EDIT_PROFILE) }, onPoints = { nav.navigate(Routes.GOPOINTS) },
                    onInterests = { nav.navigate(Routes.INTERESTS) }, onChangePassword = { nav.navigate(Routes.CHANGE_PASSWORD) },
                    onSettings = { nav.navigate(Routes.SETTINGS) }, onHelp = { nav.navigate(Routes.HELP) }, onAbout = { nav.navigate(Routes.ABOUT) },
                    onSignedOut = { /* the sign-in guard above moves the user to Sign in */ }
                )
            }

            // ------------------------------------------------ events and booking flow
            composable(Routes.EVENT, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                EventDetailsScreen(id, favorites, onBack = { nav.popBackStack() }, onMap = { nav.navigate(Routes.map(id)) }, onBook = { nav.navigate(Routes.bookingFlow(id)) })
            }
            composable(Routes.MAP, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                EventMapScreen(e.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }
            composable(Routes.BOOKING_FLOW, listOf(navArgument("eventId") { type = NavType.StringType })) { e ->
                val eventId = e.arguments?.getString("eventId").orEmpty()
                BookingFlowScreen(eventId, onExit = { nav.popBackStack() }, onViewTickets = { nav.goBottom(Routes.TICKETS) })
            }
            composable(Routes.TICKET, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                DigitalTicketScreen(id, onBack = { nav.popBackStack() }, onQr = { nav.navigate(Routes.qr(id)) })
            }
            composable(Routes.QR, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                QrViewScreen(e.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }

            // ------------------------------------------------ points and rewards
            composable(Routes.GOPOINTS) {
                GoPointsScreen(session, onBack = { nav.popBackStack() }, onHistory = { nav.navigate(Routes.GOPOINTS_HISTORY) }, onRewards = { nav.navigate(Routes.REWARDS) }, onAchievements = { nav.navigate(Routes.ACHIEVEMENTS) })
            }
            composable(Routes.GOPOINTS_HISTORY) { GoPointsHistoryScreen(session) { nav.popBackStack() } }
            composable(Routes.REWARDS) { RewardsScreen(session) { nav.popBackStack() } }
            composable(Routes.ACHIEVEMENTS) { AchievementsScreen(session) { nav.popBackStack() } }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen(session) { nav.popBackStack() } }

            // ------------------------------------------------ account
            composable(Routes.EDIT_PROFILE) { EditProfileScreen(session) { nav.popBackStack() } }
            composable(Routes.SETTINGS) {
                SettingsScreen(session, onBack = { nav.popBackStack() }, onLanguage = { nav.navigate(Routes.LANGUAGE) }, onChangePassword = { nav.navigate(Routes.CHANGE_PASSWORD) })
            }
            composable(Routes.LANGUAGE) { LanguageScreen(session) { nav.popBackStack() } }
            composable(Routes.HELP) { HelpScreen { nav.popBackStack() } }
            composable(Routes.ABOUT) { AboutScreen { nav.popBackStack() } }
        }
    }
}
