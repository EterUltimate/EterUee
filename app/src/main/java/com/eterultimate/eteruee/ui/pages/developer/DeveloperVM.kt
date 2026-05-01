package com.eterultimate.eteruee.ui.pages.developer

import androidx.lifecycle.ViewModel
import com.eterultimate.eteruee.data.ai.AILoggingManager

class DeveloperVM(
    private val aiLoggingManager: AILoggingManager
) : ViewModel() {
    val logs = aiLoggingManager.getLogs()
}

