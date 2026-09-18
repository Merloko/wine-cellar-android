package com.winecellar.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.winecellar.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object Routes {
    const val CELLAR = "cellar"
    const val DRINK_NOW = "drinkNow"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val SCAN = "scan"
    const val ADD = "add?barcode={barcode}"
    const val DETAIL = "detail/{id}"
    const val EDIT = "edit/{id}"
    fun detail(id: Long) = "detail/$id"
    fun edit(id: Long) = "edit/$id"
    fun add(barcode: String? = null) =
        if (barcode.isNullOrBlank()) "add" else "add?barcode=${Uri.encode(barcode)}"
}

@Composable
fun WineCellarRoot(vm: WineViewModel = viewModel()) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.CELLAR) {

        homeTab(Routes.CELLAR, nav, showFab = true, actions = { CellarActions(vm, nav) }) { padding ->
            val state by vm.uiState.collectAsStateWithLifecycle()
            CellarScreen(
                state = state,
                currentYear = vm.currentYear(),
                onQuery = vm::setQuery,
                onLocation = vm::setLocation,
                onStyle = vm::setStyle,
                onWinery = vm::setWinery,
                onSort = vm::setSort,
                onClearFilters = vm::clearFilters,
                onOpenWine = { nav.navigate(Routes.detail(it)) },
                contentPadding = padding,
            )
        }

        homeTab(Routes.DRINK_NOW, nav) { padding ->
            val state by vm.drinkNow.collectAsStateWithLifecycle()
            DrinkNowScreen(
                state = state,
                currentYear = vm.currentYear(),
                onOpenWine = { nav.navigate(Routes.detail(it)) },
                contentPadding = padding,
            )
        }

        homeTab(Routes.HISTORY, nav) { padding ->
            val history by vm.history.collectAsStateWithLifecycle()
            HistoryScreen(
                history = history,
                onDelete = vm::deleteLog,
                contentPadding = padding,
            )
        }

        homeTab(Routes.STATS, nav) { padding ->
            val stats by vm.stats.collectAsStateWithLifecycle()
            StatsScreen(stats = stats, contentPadding = padding)
        }

        composable(Routes.DETAIL) { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: -1L
            val wine by vm.wine(id).collectAsStateWithLifecycle(initialValue = null)
            WineDetailScreen(
                wine = wine,
                currentYear = vm.currentYear(),
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.edit(id)) },
                onDelete = { toDelete -> vm.delete(toDelete); nav.popBackStack() },
                onToggleFavorite = { w -> vm.save(w.copy(favorite = !w.favorite)) },
                onDrink = { rating, note -> wine?.let { vm.drinkBottle(it, rating, note) } },
            )
        }

        composable(
            route = Routes.ADD,
            arguments = listOf(
                navArgument("barcode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val barcode = entry.arguments?.getString("barcode")
            val state by vm.uiState.collectAsStateWithLifecycle()
            val lookup by vm.barcodeLookup.collectAsStateWithLifecycle()
            AddEditWineScreen(
                initial = null,
                knownLocations = state.locations,
                knownWineries = state.wineries,
                initialBarcode = barcode,
                lookupState = lookup,
                onLookupBarcode = vm::lookupBarcodeOnline,
                onConsumeLookup = vm::consumeBarcodeLookup,
                onCancel = { nav.popBackStack() },
                onSave = { wine -> vm.save(wine); nav.popBackStack() },
            )
        }

        composable(Routes.EDIT) { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: -1L
            val wine by vm.wine(id).collectAsStateWithLifecycle(initialValue = null)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val lookup by vm.barcodeLookup.collectAsStateWithLifecycle()
            AddEditWineScreen(
                initial = wine,
                knownLocations = state.locations,
                knownWineries = state.wineries,
                lookupState = lookup,
                onLookupBarcode = vm::lookupBarcodeOnline,
                onConsumeLookup = vm::consumeBarcodeLookup,
                onCancel = { nav.popBackStack() },
                onSave = { updated -> vm.save(updated); nav.popBackStack() },
            )
        }

        composable(Routes.SCAN) {
            ScanScreen(
                onBack = { nav.popBackStack() },
                onBarcode = { code ->
                    vm.lookupBarcode(code) { found ->
                        if (found != null) {
                            nav.navigate(Routes.detail(found.id)) {
                                popUpTo(Routes.SCAN) { inclusive = true }
                            }
                        } else {
                            nav.navigate(Routes.add(code)) {
                                popUpTo(Routes.SCAN) { inclusive = true }
                            }
                        }
                    }
                },
            )
        }
    }
}

/** Cellar top-bar actions: quick scan + an export overflow menu. */
@Composable
private fun RowScope.CellarActions(vm: WineViewModel, nav: NavHostController) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }

    // Import: pick any file, then sniff CSV vs JSON by content.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val appContext = context.applicationContext
            vm.importFromUri(uri) { count ->
                val msg = when (count) {
                    null -> appContext.getString(R.string.import_failed)
                    0 -> appContext.getString(R.string.import_empty)
                    else -> appContext.resources.getQuantityString(R.plurals.import_success, count, count)
                }
                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // The write runs on the ViewModel scope (survives navigation); launching the
    // share sheet is a quick synchronous call once the file's Uri is ready.
    fun export(format: ExportFormat) {
        menu = false
        // Use the application context: the callback runs on the retained
        // viewModelScope and may fire after this Activity is gone.
        val appContext = context.applicationContext
        vm.requestExport(format) { uri, mime ->
            if (uri == null || !ExportUtils.launchShare(appContext, uri, mime)) {
                Toast.makeText(appContext, appContext.getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    IconButton(onClick = { nav.navigate(Routes.SCAN) }) {
        Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.action_scan_barcode))
    }
    IconButton(onClick = { menu = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
    }
    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_export_csv)) },
            onClick = { export(ExportFormat.CSV) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_export_json)) },
            onClick = { export(ExportFormat.JSON) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_import)) },
            onClick = {
                menu = false
                importLauncher.launch(arrayOf("*/*"))
            },
        )
    }
}

/** Registers one of the bottom-nav tabs, wrapped in the shared home chrome. */
private fun NavGraphBuilder.homeTab(
    route: String,
    nav: NavHostController,
    showFab: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    composable(route) {
        HomeScaffold(currentRoute = route, nav = nav, showFab = showFab, actions = actions, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScaffold(
    currentRoute: String,
    nav: NavHostController,
    showFab: Boolean,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleFor(currentRoute))) },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.CELLAR,
                    onClick = { navigateTab(nav, Routes.CELLAR) },
                    icon = { Icon(Icons.Filled.WineBar, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_cellar)) },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.DRINK_NOW,
                    onClick = { navigateTab(nav, Routes.DRINK_NOW) },
                    icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_drink_now)) },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.HISTORY,
                    onClick = { navigateTab(nav, Routes.HISTORY) },
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_history)) },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.STATS,
                    onClick = { navigateTab(nav, Routes.STATS) },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_stats)) },
                )
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = { nav.navigate(Routes.add()) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_wine))
                }
            }
        },
        content = content,
    )
}

@androidx.annotation.StringRes
private fun titleFor(route: String): Int = when (route) {
    Routes.DRINK_NOW -> R.string.tab_drink_now
    Routes.HISTORY -> R.string.tab_history
    Routes.STATS -> R.string.tab_stats
    else -> R.string.app_name
}

private fun navigateTab(nav: NavHostController, route: String) {
    if (nav.currentDestination?.route == route) return
    nav.navigate(route) {
        popUpTo(Routes.CELLAR) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
