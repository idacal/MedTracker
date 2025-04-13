package com.example.medtrackerapp.ui.navigation

/**
 * Rutas de navegación detalladas
 */
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object CategoryList : Screen("category_list/{examId}")
    object CategoryDetail : Screen("category_detail/{examId}/{categoryName}")
    object ParameterDetail : Screen("parameter_detail/{parameterName}")
    object CompareParameters : Screen("compare_parameters/{parameterName}")
    object Upload : Screen("upload")
    object History : Screen("history")
    object Profile : Screen("profile")
    
    // Funciones para crear rutas con parámetros
    fun createRoute(vararg args: String): String {
        var routeString = route
        args.forEach { arg ->
            val placeholder = "{${arg.substringBefore("=")}}"
            val value = arg.substringAfter("=")
            routeString = routeString.replace(placeholder, value)
        }
        return routeString
    }
} 