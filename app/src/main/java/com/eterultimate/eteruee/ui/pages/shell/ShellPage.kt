package com.eterultimate.eteruee.ui.pages.shell

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.shell.EmbeddedTermuxTerminalClient
import com.eterultimate.eteruee.shell.LocalShellRunner
import com.eterultimate.eteruee.shell.createEmbeddedTermuxSession
import com.eterultimate.eteruee.ui.components.nav.BackButton
import com.termux.view.TerminalView

@Composable
fun ShellPage() {
    val context = LocalContext.current
    var title by remember { mutableStateOf("EterUee Shell") }
    var finished by remember { mutableStateOf(false) }
    val client = remember {
        EmbeddedTermuxTerminalClient(
            context = context.applicationContext,
            onTitle = { newTitle ->
                if (newTitle.isNotBlank()) title = newTitle
            },
            onFinished = { finished = true },
        )
    }
    val session = remember {
        createEmbeddedTermuxSession(
            context = context.applicationContext,
            client = client,
        )
    }
    val workingDir = remember {
        LocalShellRunner.defaultWorkingDir(context).absolutePath
    }

    DisposableEffect(session) {
        onDispose {
            if (session.pid > 0 && session.isRunning) {
                session.finishIfRunning()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.shell_page_title))
                        Text(
                            text = if (finished) "Session finished" else title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = { BackButton() },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        TerminalView(viewContext, null).apply {
                            isFocusable = true
                            isFocusableInTouchMode = true
                            val textSizePx = (14 * viewContext.resources.displayMetrics.scaledDensity).toInt()
                            setTerminalViewClient(client)
                            setTextSize(textSizePx.coerceAtLeast(12))
                            attachSession(session)
                            setOnTouchListener { view, _ ->
                                view.requestFocus()
                                view.requestFocusFromTouch()
                                (viewContext.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                                    ?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                                false
                            }
                            post {
                                requestFocus()
                                requestFocusFromTouch()
                                (viewContext.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                                    ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            }
                            client.terminalView = this
                        }
                    },
                    update = { view ->
                        if (client.terminalView !== view) {
                            client.terminalView = view
                        }
                    },
                )
            }

            Text(
                text = "Shell: ${LocalShellRunner.SHELL_PATH} · cwd: $workingDir",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
