package com.example.medtrackerapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.medtrackerapp.ui.dashboard.DashboardScreen
import com.example.medtrackerapp.ui.upload.UploadScreen
import com.example.medtrackerapp.ui.category.CategoryListScreen
import com.example.medtrackerapp.ui.navigation.Screen
import com.example.medtrackerapp.ui.category.CategoryDetailScreen

/**
 * Secciones principales de la aplicación
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : BottomNavItem(
        route = "dashboard",
        title = "Inicio",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    object History : BottomNavItem(
        route = "history",
        title = "Historial",
        selectedIcon = Icons.Filled.List,
        unselectedIcon = Icons.Outlined.List
    )
    
    object Profile : BottomNavItem(
        route = "profile",
        title = "Perfil",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

/**
 * Navegación principal de la aplicación
 */
@Composable
fun MedTrackerNavigation() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = { MedTrackerBottomNav(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Pantalla principal
            composable(BottomNavItem.Dashboard.route) {
                DashboardScreen(navController)
            }
            
            // Historial
            composable(BottomNavItem.History.route) {
                // HistoryScreen(navController)
                // Implementación pendiente
                Text("Pantalla de Historial")
            }
            
            // Perfil
            composable(BottomNavItem.Profile.route) {
                // ProfileScreen(navController)
                // Implementación pendiente
                Text("Pantalla de Perfil")
            }
            
            // Otras pantallas
            composable(Screen.Upload.route) {
                UploadScreen(navController)
            }
            
            // Detalles de categoría
            composable(Screen.CategoryList.route) { navBackStackEntry ->
                val examId = navBackStackEntry.arguments?.getString("examId") ?: ""
                CategoryListScreen(navController, examId)
            }
            
            // Detalle de categoría (parámetros)
            composable(Screen.CategoryDetail.route) { navBackStackEntry ->
                val examId = navBackStackEntry.arguments?.getString("examId") ?: ""
                val categoryName = navBackStackEntry.arguments?.getString("categoryName") ?: ""
                CategoryDetailScreen(navController, examId, categoryName)
            }
            
            // Detalles de parámetro
            composable(Screen.ParameterDetail.route) { navBackStackEntry ->
                val parameterName = navBackStackEntry.arguments?.getString("parameterName") ?: ""
                // ParameterDetailScreen(navController, parameterName)
                // Implementación pendiente
                Text("Detalles del parámetro $parameterName")
            }
            
            // Comparación de parámetros
            composable(Screen.CompareParameters.route) { navBackStackEntry ->
                val parameterName = navBackStackEntry.arguments?.getString("parameterName") ?: ""
                // CompareParametersScreen(navController, parameterName)
                // Implementación pendiente
                Text("Comparar parámetro $parameterName")
            }
        }
    }
}

/**
 * Barra de navegación inferior
 */
@Composable
fun MedTrackerBottomNav(navController: NavController) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.History,
        BottomNavItem.Profile
    )
    
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(text = item.title) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
} 