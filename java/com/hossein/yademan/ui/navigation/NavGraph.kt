// مسیر: app/src/main/java/com/hossein/yademan/ui/navigation/NavGraph.kt
package com.hossein.yademan.ui.navigation

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.services.NotificationHelper
import com.hossein.yademan.ui.components.BottomNavBar
import com.hossein.yademan.ui.components.ScreenBackground
import com.hossein.yademan.ui.components.YTab
import com.hossein.yademan.ui.screens.AddReminderScreen
import com.hossein.yademan.ui.screens.BackupScreen
import com.hossein.yademan.ui.screens.BillsScreen
import com.hossein.yademan.ui.screens.CalendarScreen
import com.hossein.yademan.ui.screens.EditReminderScreen
import com.hossein.yademan.ui.screens.ForgotPasswordScreen
import com.hossein.yademan.ui.screens.HomeScreen
import com.hossein.yademan.ui.screens.HouseholdScreen
import com.hossein.yademan.ui.screens.HubKey
import com.hossein.yademan.ui.screens.HubScreen
import com.hossein.yademan.ui.screens.LoginScreen
import com.hossein.yademan.ui.screens.MedicineScreen
import com.hossein.yademan.ui.screens.NoteEditScreen
import com.hossein.yademan.ui.screens.NotesScreen
import com.hossein.yademan.ui.screens.NotificationsScreen
import com.hossein.yademan.ui.screens.ProfileScreen
import com.hossein.yademan.ui.screens.RemindersScreen
import com.hossein.yademan.ui.screens.RentScreen // ✅ ۱. ایمپورت صفحه اجاره
import com.hossein.yademan.ui.screens.SettingsScreen
import com.hossein.yademan.ui.screens.ShoppingScreen
import com.hossein.yademan.ui.screens.SubscriptionsScreen
import com.hossein.yademan.ui.screens.TrustScreen
import com.hossein.yademan.ui.theme.YColors

object Routes {
    const val EDIT = "edit?id={id}"
    const val SETTINGS = "settings"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RENT = "rent" // ✅ ۲. تعریف مسیر اجاره

    fun edit(id: String) = "edit?id=${Uri.encode(id)}"
    const val BILLS = "bills?tab={tab}"
    const val CALENDAR = "calendar"
    const val SUBSCRIPTIONS = "subscriptions"
    const val MEDICINE = "medicine"
    const val NOTES = "notes"
    const val TRUST = "trust"
    const val NOTE_EDIT = "note_edit?id={id}"
    const val ADD = "add?type={type}&title={title}&day={day}&hub={hub}"
    const val NOTIFICATIONS = "notifications"
    const val LOGIN = "login"
    const val HUB = "hub?key={key}&label={label}"

    fun bills(tab: Int) = "bills?tab=$tab"
    fun noteEdit(id: String?) = if (id == null) "note_edit" else "note_edit?id=${Uri.encode(id)}"
    fun add(type: String? = null, title: String? = null, day: Long? = null, hub: String? = null): String {
        val params = mutableListOf<String>()
        if (type != null) params.add("type=${Uri.encode(type)}")
        if (!title.isNullOrBlank()) params.add("title=${Uri.encode(title)}")
        if (day != null) params.add("day=$day")
        if (hub != null) params.add("hub=${Uri.encode(hub)}")
        return if (params.isEmpty()) "add" else "add?" + params.joinToString("&")
    }
    fun hub(key: String, label: String) = "hub?key=${Uri.encode(key)}&label=${Uri.encode(label)}"
}

@Composable
fun YademanNavGraph(pendingRoute: String?, onRouteConsumed: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = YTab.entries.firstOrNull { it.route == currentRoute }
    val context = LocalContext.current

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasPermission(context)) {
            val meta = ServiceLocator.meta
            if (meta.get(MetaKeys.NOTIF_PERMISSION_ASKED) == null) {
                meta.set(MetaKeys.NOTIF_PERMISSION_ASKED, "true")
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(pendingRoute) {
        if (pendingRoute == "notifications") {
            navController.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true }
        }
        if (pendingRoute != null) onRouteConsumed()
    }

    fun openTab(tab: YTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ScreenBackground {
        NavHost(
            navController = navController,
            startDestination = YTab.HOME.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(100)) },
            popEnterTransition = { fadeIn(animationSpec = tween(150)) },
            popExitTransition = { fadeOut(animationSpec = tween(100)) }
        ) {
            composable(YTab.HOME.route) {
                HomeScreen(
                    onBellClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onPlanClick = { navController.navigate(Routes.CALENDAR) },
                    onHubClick = { key, label -> onHub(navController, key, label) { openTab(YTab.SHOPPING) } },
                    onManageClick = { openTab(YTab.REMINDERS) }
                )
            }
            composable(YTab.REMINDERS.route) {
                RemindersScreen(
                    onOpenBills = { navController.navigate(Routes.bills(0)) },
                    onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                    onOpenMedicine = { navController.navigate(Routes.MEDICINE) },
                    onOpenNotes = { navController.navigate(Routes.NOTES) },
                    onOpenSubscriptions = { navController.navigate(Routes.SUBSCRIPTIONS) },
                    onOpenTrust = { navController.navigate(Routes.TRUST) },
                    onEdit = { id -> navController.navigate(Routes.edit(id)) }
                )
            }
            composable(YTab.SHOPPING.route) {
                ShoppingScreen()
            }
            composable(YTab.PROFILE.route) {
                ProfileScreen(
                    onLogin = { navController.navigate(Routes.LOGIN) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenBackup = { navController.navigate("backup") },
                    onOpenHousehold = { navController.navigate("household") },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(
                Routes.BILLS,
                arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
            ) { entry ->
                BillsScreen(
                    initialTab = entry.arguments?.getInt("tab") ?: 0,
                    onBack = { navController.popBackStack() },
                    onAdd = { type -> navController.navigate(Routes.add(type = type)) },
                    onEdit = { id -> navController.navigate(Routes.edit(id)) }
                )
            }
            composable(Routes.SUBSCRIPTIONS) {
                SubscriptionsScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Routes.add(type = ReminderType.SUBSCRIPTION)) }
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { day -> navController.navigate(Routes.add(type = ReminderType.MEETING, day = day)) }
                )
            }
            composable(Routes.MEDICINE) {
                MedicineScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Routes.add(type = ReminderType.MEDICINE)) }
                )
            }
            composable(Routes.NOTES) {
                NotesScreen(
                    onBack = { navController.popBackStack() },
                    onOpen = { id -> navController.navigate(Routes.noteEdit(id)) }
                )
            }
            composable(
                Routes.NOTE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { entry ->
                NoteEditScreen(
                    noteId = entry.arguments?.getString("id"),
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Routes.TRUST) {
                TrustScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Routes.add(type = ReminderType.TRUST)) }
                )
            }
            composable(
                Routes.ADD,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("day") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("hub") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { entry ->
                AddReminderScreen(
                    initialType = entry.arguments?.getString("type"),
                    initialTitle = entry.arguments?.getString("title"),
                    initialDay = entry.arguments?.getLong("day")?.takeIf { it > 0L },
                    initialHub = entry.arguments?.getString("hub"),
                    onClose = { navController.popBackStack() }
                )
            }
            composable(
                Routes.HUB,
                arguments = listOf(
                    navArgument("key") { type = NavType.StringType },
                    navArgument("label") { type = NavType.StringType }
                )
            ) { entry ->
                HubScreen(
                    hubKey = entry.arguments?.getString("key") ?: "",
                    label = entry.arguments?.getString("label") ?: "",
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Routes.add(hub = entry.arguments?.getString("key"))) },
                    onEdit = { id -> navController.navigate(Routes.edit(id)) }
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onClose = { navController.popBackStack() },
                    onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
                )
            }
            composable(Routes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }
            composable("backup") {
                BackupScreen(onBack = { navController.popBackStack() })
            }
            composable("household") {
                HouseholdScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Routes.EDIT,
                arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { entry ->
                EditReminderScreen(
                    reminderId = entry.arguments?.getString("id"),
                    onClose = { navController.popBackStack() }
                )
            }
            // ✅ ۳. اضافه کردن کامپوزابل صفحه اجاره
            composable(Routes.RENT) {
                RentScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (currentTab != null) {
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Brush.verticalGradient(0f to Color.Transparent, 0.75f to YColors.Bg0))
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(YColors.Bg0)
                )
            }
            BottomNavBar(
                currentTab = currentTab,
                onTabClick = { openTab(it) },
                onFabClick = { navController.navigate(Routes.add()) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}

private fun onHub(nav: NavHostController, key: String, label: String, openShopping: () -> Unit) {
    when (key) {
        HubKey.INSTALLMENT -> nav.navigate(Routes.bills(0))
        HubKey.CHECK -> nav.navigate(Routes.bills(1))
        HubKey.SUBSCRIPTION -> nav.navigate(Routes.SUBSCRIPTIONS)
        HubKey.MEDICINE -> nav.navigate(Routes.MEDICINE)
        HubKey.MEETING -> nav.navigate(Routes.CALENDAR)
        HubKey.NOTE -> nav.navigate(Routes.NOTES)
        HubKey.SHOPPING -> openShopping()
        "rent" -> nav.navigate(Routes.RENT) // ✅ ۴. هدایت کلیک روی کاشی اجاره به صفحه اجاره
        "add" -> nav.navigate(Routes.add())
        else -> nav.navigate(Routes.hub(key, label))
    }
}