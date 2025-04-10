package me.rerere.rikkahub.ui.utils

import android.util.Log
import androidx.navigation.NavController
import kotlin.uuid.Uuid

private const val TAG = "ChatUtil"

fun navigateToChatPage(
    navController: NavController,
    chatId: Uuid = Uuid.random()
) {
    Log.i(TAG, "navigateToChatPage: navigate to $chatId")
    navController.navigate("chat/${chatId}") {
        popUpTo(0) {
            inclusive = true
        }
        launchSingleTop = true
    }
}