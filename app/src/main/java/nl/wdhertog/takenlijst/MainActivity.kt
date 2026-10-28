package nl.wdhertog.takenlijst

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nl.wdhertog.takenlijst.data.TaskGroup
import nl.wdhertog.takenlijst.ui.AppViewModel
import nl.wdhertog.takenlijst.ui.DrawableIcon
import nl.wdhertog.takenlijst.ui.IconPickerScreen
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
        setContent { TakenTheme { AppRoot(vm) } }
    }
}

/** Sleutel waarmee de iconenkiezer zijn keuze teruggeeft aan de taakeditor. */
private const val PICKED_ICON = "gekozen_icoon"

private enum class Tab(val route: String, val label: String, val icon: Int) {
    TODAY("vandaag", "Vandaag", R.drawable.ic_sun),
    TASKS("taken", "Taken", R.drawable.ic_list),
    SHOPPING("boodschappen", "Boodschappen", R.drawable.ic_cart),
    MORE("meer", "Meer", R.drawable.ic_grid),
}

@Composable
private fun AppRoot(vm: AppViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tab = Tab.entries.firstOrNull { it.route == route }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshDate() }

    fun go(t: Tab) = nav.navigate(t.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

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
        NavHost(nav, startDestination = Tab.TODAY.route) {
            composable(Tab.TODAY.route) {
                TodayScreen(vm, snackbar, padding, onEdit = { nav.navigate("bewerk/$it") }, onShopping = { go(Tab.SHOPPING) })
            }
            composable(Tab.TASKS.route) { TasksScreen(vm, padding, onEdit = { nav.navigate("bewerk/$it") }) }
            composable(Tab.SHOPPING.route) { ShoppingScreen(vm, snackbar, padding, onScan = { nav.navigate("invoer/foto") }, onVoice = { nav.navigate("invoer/spraak") }) }
            composable(Tab.MORE.route) { MoreScreen(vm, padding) }
            composable("bewerk/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val picked by entry.savedStateHandle.getStateFlow<String?>(PICKED_ICON, null).collectAsState()
                TaskEditScreen(
                    vm, entry.arguments?.getLong("id") ?: 0L,
                    onDone = { nav.popBackStack() },
                    onPickIcon = { current, title, group ->
                        nav.navigate("icoon/${group.name}?huidig=${Uri.encode(current)}&naam=${Uri.encode(title)}")
                    },
                    pickedIcon = picked,
                    onPickedIconHandled = { entry.savedStateHandle[PICKED_ICON] = null },
                )
            }
            composable(
                "icoon/{groep}?huidig={huidig}&naam={naam}",
                arguments = listOf(
                    navArgument("huidig") { defaultValue = "" },
                    navArgument("naam") { defaultValue = "" },
                ),
            ) { entry ->
                val args = entry.arguments
                IconPickerScreen(
                    current = args?.getString("huidig").orEmpty(),
                    title = args?.getString("naam").orEmpty(),
                    group = TaskGroup.valueOf(args?.getString("groep") ?: TaskGroup.HOME.name),
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
