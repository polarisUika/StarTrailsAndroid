package com.startrails.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.startrails.core.designsystem.theme.Spacing
import com.startrails.core.designsystem.theme.findActivity
import com.startrails.feature.gallery.GalleryRoute
import com.startrails.feature.settings.SettingsRoute
import com.startrails.feature.stacker.StackerRoute

private object Routes {
    const val STACKER = "stacker"
    const val GALLERY = "gallery"
    const val SETTINGS = "settings"
}

private data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

private val items = listOf(
    NavItem(Routes.STACKER, "叠加", Icons.Filled.Layers, Icons.Outlined.Layers),
    NavItem(Routes.GALLERY, "作品", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    NavItem(Routes.SETTINGS, "设置", Icons.Filled.Settings, Icons.Outlined.Settings),
)

/**
 * 顶层导航外壳：
 *  - 紧凑宽度：底部 NavigationBar；
 *  - 中等宽度，或展开但高度不足（横屏手机）：NavigationRail；
 *  - 其余情况：PermanentNavigationDrawer。
 *
 * 三种布局的目标地名称与顺序完全一致，切换窗口尺寸时不会丢失当前标签。
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun StarTrailsNavHost() {
    val navController = rememberNavController()
    // LocalContext 可能被 ContextWrapper 包装，直接强转会抛 ClassCastException。
    val activity = LocalContext.current.findActivity() ?: return
    val windowSizeClass = calculateWindowSizeClass(activity)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rawRoute = backStackEntry?.destination?.route ?: Routes.STACKER
    val currentRoute = if (rawRoute.startsWith(Routes.STACKER)) Routes.STACKER else rawRoute

    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass

    when {
        widthClass == WindowWidthSizeClass.Compact ->
            BottomBarNavigation(navController, currentRoute)

        widthClass == WindowWidthSizeClass.Medium || heightClass == WindowHeightSizeClass.Compact ->
            RailNavigation(navController, currentRoute)

        else -> DrawerNavigation(navController, currentRoute)
    }
}

@Composable
private fun BottomBarNavigation(navController: NavHostController, currentRoute: String) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        // 顶部留白交给页面自己的 TopAppBar 处理，避免状态栏被重复计算。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigate(navController, item.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.icon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(item.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { padding ->
        AppScreens(navController, Modifier.padding(padding))
    }
}

@Composable
private fun RailNavigation(navController: NavHostController, currentRoute: String) {
    Row(Modifier.fillMaxSize()) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            header = { BrandMark(modifier = Modifier.padding(top = Spacing.md)) },
        ) {
            Spacer(Modifier.height(Spacing.lg))
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationRailItem(
                    selected = selected,
                    onClick = { navigate(navController, item.route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(item.label) },
                )
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .navigationBarsPadding(),
        ) {
            AppScreens(navController)
        }
    }
}

@Composable
private fun DrawerNavigation(navController: NavHostController, currentRoute: String) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.lg)) {
                    BrandHeader()
                    Spacer(Modifier.height(Spacing.xl))
                    items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationDrawerItem(
                            selected = selected,
                            onClick = { navigate(navController, item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.icon,
                                    contentDescription = null,
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            shape = MaterialTheme.shapes.small,
                            colors = NavigationDrawerItemDefaults.colors(),
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            AppScreens(navController)
        }
    }
}

/** 侧栏 / 抽屉顶部的品牌标识。 */
@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandMark()
        Spacer(Modifier.width(Spacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = "StarTrails", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "星轨合成",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Layers,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun navigate(navController: NavHostController, route: String) {
    if (navController.currentDestination?.route == route) return
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppScreens(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.STACKER,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 40)) +
                slideInHorizontally(animationSpec = tween(durationMillis = 280)) { it / 16 }
        },
        exitTransition = { fadeOut(animationSpec = tween(durationMillis = 140)) },
        popEnterTransition = {
            fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 40)) +
                slideInHorizontally(animationSpec = tween(durationMillis = 280)) { -it / 16 }
        },
        popExitTransition = { fadeOut(animationSpec = tween(durationMillis = 140)) },
    ) {
        composable(Routes.STACKER) { StackerRoute() }
        composable("${Routes.STACKER}/detail/{jobId}") {
            StackerRoute(
                isDetailMode = true,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.GALLERY) {
            GalleryRoute(
                onOpenStacker = { navigate(navController, Routes.STACKER) },
                onOpenJob = { id -> navController.navigate("${Routes.STACKER}/detail/$id") },
            )
        }
        composable(Routes.SETTINGS) { SettingsRoute() }
    }
}
