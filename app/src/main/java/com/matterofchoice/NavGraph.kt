package com.matterofchoice



import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNav(navController: NavHostController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    BottomAppBar(
        containerColor = Color.White,
        modifier = Modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        )
    ) {
        BottomNavItem(
            modifier = Modifier.weight(1f),
            isSelected = currentRoute == Screens.GameScreen.screen,
            selectedIcon = R.drawable.game1,
            unselectedIcon = R.drawable.game2,
            onClick = {
                navController.navigate(Screens.GameScreen.screen) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // Result Icon
        BottomNavItem(
            modifier = Modifier.weight(1f),
            isSelected = currentRoute == Screens.ResultScreen.screen,
            selectedIcon = R.drawable.result,
            unselectedIcon = R.drawable.result2,
            onClick = {
                navController.navigate(Screens.ResultScreen.screen) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // Settings Icon
        BottomNavItem(
            modifier = Modifier.weight(1f),
            isSelected = currentRoute == Screens.SettingsScreen.screen,
            selectedIcon = R.drawable.settings1,
            unselectedIcon = R.drawable.settings2,
            onClick = {
                navController.navigate(Screens.SettingsScreen.screen) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        BottomNavItem(
            modifier = Modifier.weight(1f),
            isSelected = currentRoute == Screens.AnalysisScreen.screen,
            selectedIcon = R.drawable.analysis,
            unselectedIcon = R.drawable.analysis2,
            onClick = {
                navController.navigate(Screens.AnalysisScreen.screen) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
fun BottomNavItem(
    isSelected: Boolean,
    selectedIcon: Int,
    unselectedIcon: Int,
    onClick: () -> Unit,
    modifier: Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(if (isSelected) selectedIcon else unselectedIcon),
            contentDescription = null
        )
    }
}
