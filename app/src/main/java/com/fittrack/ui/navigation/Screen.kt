package com.fittrack.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object PlanList : Screen("plan_list")
    object PlanDetail : Screen("plan_detail/{planId}") {
        fun createRoute(planId: Long) = "plan_detail/$planId"
    }
    object AddPlan : Screen("add_plan")
    object EditPlan : Screen("edit_plan/{planId}") {
        fun createRoute(planId: Long) = "edit_plan/$planId"
    }
    object Workout : Screen("workout/{planId}") {
        fun createRoute(planId: Long) = "workout/$planId"
    }
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object Chat : Screen("chat")
    object PlanGenerator : Screen("plan_generator")
}
