package me.rerere.rikkahub.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.rerere.rikkahub.ui.hooks.LocalNavController

@Composable
fun BackButton(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    IconButton(
        onClick = {
            navController.popBackStack()
        }
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back"
        )
    }
}