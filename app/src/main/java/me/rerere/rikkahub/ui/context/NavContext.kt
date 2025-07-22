package me.rerere.rikkahub.ui.context

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

val LocalNavController = compositionLocalOf<NavHostController> {
    error("No NavController provided")
}
