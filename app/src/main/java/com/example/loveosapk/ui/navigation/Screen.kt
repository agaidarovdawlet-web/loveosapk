package com.example.loveosapk.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Home : Screen("home")
    object Cycle : Screen("cycle")
    object Chat : Screen("chat")
    object Stuff : Screen("stuff")
    object Notes : Screen("notes")
    object More : Screen("more")
    object Games : Screen("games")
    object Profile : Screen("profile")
}
