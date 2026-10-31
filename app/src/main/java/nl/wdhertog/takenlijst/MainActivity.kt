package nl.wdhertog.takenlijst

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import nl.wdhertog.takenlijst.ui.AppViewModel
import nl.wdhertog.takenlijst.ui.DrawableIcon
import nl.wdhertog.takenlijst.ui.GroupEditScreen
import nl.wdhertog.takenlijst.ui.GroupsScreen
import nl.wdhertog.takenlijst.ui.IconPickerScreen
import nl.wdhertog.takenlijst.ui.LocalGroups
import nl.wdhertog.takenlijst.ui.TaskIcon
import nl.wdhertog.takenlijst.ui.MoreScreen
import nl.wdhertog.takenlijst.ui.ScanScreen
import nl.wdhertog.takenlijst.ui.ShoppingScreen
import nl.wdhertog.takenlijst.ui.TaskEditScreen
import nl.wdhertog.takenlijst.ui.TasksScreen
import nl.wdhertog.takenlijst.ui.TodayScreen
import nl.wdhertog.takenlijst.ui.theme.App
import nl.wdhertog.takenlijst.ui.theme.TakenTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val mode by vm.themeMode.collectAsState()
            val dark = mode.isDark()
            // Statusbalk en navigatiebalk volgen het gekozen thema, niet alleen dat van de telefoon.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            TakenTheme(dark) { AppRoot(vm) }
        }
    }
}

/** Sleutel waarmee de iconenkiezer zijn keuze teruggeeft aan de taakeditor. */
private const val PICKED_ICON = "gekozen_icoon"

/** De tabbladen; ze staan naast elkaar in één pager, zodat je ertussen kunt swipen. */
private enum class Tab(val label: String, val icon: Int) {
    TODAY("Vandaag", R.drawable.ic_sun),
    TASKS("Taken", R.drawable.ic_list),
    SHOPPING("Boodschappen", R.drawable.ic_cart),
    MORE("Meer", R.drawable.ic_grid),
}

private const val TABS = "tabs"

@Composable
private fun AppRoot(vm: AppViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val pager = rememberPagerState { Tab.entries.size }
    val scope = rememberCoroutineScope()
    val tab = if (route == TABS) Tab.entries[pager.currentPage] else null

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshDate() }
    val groups by vm.groups.collectAsState()

    // Winkelmodus: scherm blijft aan en staat in portret, tot je de modus weer uitzet.
    val shopMode by vm.shopMode.collectAsState()
    val activity = LocalContext.current as? Activity
    DisposableEffect(shopMode) {
        if (shopMode) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun pickIcon(current: String, title: String, color: Int, allowAuto: Boolean, fallback: String) = nav.navigate(
        "icoon?huidig=${Uri.encode(current)}&naam=${Uri.encode(title)}&kleur=$color&auto=$allowAuto&terugval=${Uri.encode(fallback)}"
    )

    fun go(t: Tab) = scope.launch { pager.animateScrollToPage(t.ordinal) }

    // Terug vanaf een ander tabblad gaat eerst naar Vandaag, net als voorheen.
    BackHandler(enabled = tab != null && tab != Tab.TODAY) { go(Tab.TODAY) }

    CompositionLocalProvider(LocalGroups provides groups) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (tab != null) NavigationBar(containerColor = App.colors.card) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { go(t) },
                        icon = { DrawableIcon(t.icon, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface) },
                        label = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.TODAY || tab == Tab.TASKS) {
                FloatingActionButton(
                    onClick = { nav.navigate("bewerk/0") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                ) { Icon(Icons.Default.Add, "Nieuwe taak") }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = TABS) {
            composable(TABS) {
                // In de winkelmodus niet per ongeluk wegswipen van het lijstje.
                HorizontalPager(pager, userScrollEnabled = !shopMode, beyondViewportPageCount = 1) { page ->
                    when (Tab.entries[page]) {
                        Tab.TODAY -> TodayScreen(vm, snackbar, padding, onEdit = { nav.navigate("bewerk/$it") }, onShopping = { go(Tab.SHOPPING) })
                        Tab.TASKS -> TasksScreen(vm, snackbar, padding, onEdit = { nav.navigate("bewerk/$it") })
                        Tab.SHOPPING -> ShoppingScreen(vm, snackbar, padding, onScan = { nav.navigate("invoer/foto") }, onVoice = { nav.navigate("invoer/spraak") })
                        Tab.MORE -> MoreScreen(vm, padding, onGroups = { nav.navigate("groepen") })
                    }
                }
            }
            composable("groepen") {
                GroupsScreen(vm, onEdit = { id -> nav.navigate("groep/${id ?: "nieuw"}") }, onBack = { nav.popBackStack() })
            }
            composable("groep/{id}") { entry ->
                val picked by entry.savedStateHandle.getStateFlow<String?>(PICKED_ICON, null).collectAsState()
                GroupEditScreen(
                    vm, entry.arguments?.getString("id")?.takeIf { it != "nieuw" },
                    onPickIcon = { current, color -> pickIcon(current, "", color, allowAuto = false, fallback = current) },
                    pickedIcon = picked,
                    onPickedIconHandled = { entry.savedStateHandle[PICKED_ICON] = null },
                    onDone = { nav.popBackStack() },
                )
            }
            composable("bewerk/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val picked by entry.savedStateHandle.getStateFlow<String?>(PICKED_ICON, null).collectAsState()
                TaskEditScreen(
                    vm, entry.arguments?.getLong("id") ?: 0L,
                    onDone = { nav.popBackStack() },
                    onPickIcon = { current, title, group -> pickIcon(current, title, group.color, allowAuto = true, fallback = group.icon) },
                    pickedIcon = picked,
                    onPickedIconHandled = { entry.savedStateHandle[PICKED_ICON] = null },
                )
            }
            composable(
                "icoon?huidig={huidig}&naam={naam}&kleur={kleur}&auto={auto}&terugval={terugval}",
                arguments = listOf(
                    navArgument("huidig") { defaultValue = "" },
                    navArgument("naam") { defaultValue = "" },
                    navArgument("kleur") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("auto") { type = NavType.BoolType; defaultValue = true },
                    navArgument("terugval") { defaultValue = "" },
                ),
            ) { entry ->
                val args = entry.arguments
                IconPickerScreen(
                    current = args?.getString("huidig").orEmpty(),
                    title = args?.getString("naam").orEmpty(),
                    color = args?.getInt("kleur") ?: 0,
                    allowAuto = args?.getBoolean("auto") ?: true,
                    fallbackIcon = TaskIcon.byKey(args?.getString("terugval").orEmpty())?.drawable ?: R.drawable.ic_home,
                    onPick = { key ->
                        nav.previousBackStackEntry?.savedStateHandle?.set(PICKED_ICON, key)
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable("invoer/{soort}") {
                ScanScreen(vm, voice = it.arguments?.getString("soort") == "spraak", onDone = { nav.popBackStack() })
            }
        }
    }
    }
}
