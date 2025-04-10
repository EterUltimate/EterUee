package me.rerere.rikkahub.ui.utils

import androidx.navigation.NavController
import kotlin.uuid.Uuid

fun navigateToChatPage(
    navController: NavController,
    chatId: Uuid = Uuid.random()
) {
    navController.navigate("chat/${chatId}") {
        launchSingleTop = true
    }
}