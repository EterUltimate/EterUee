package com.eterultimate.eteruee.ui.pages.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.shell.EmbeddedTermuxTerminalClient
import com.eterultimate.eteruee.shell.LocalShellRunner
import com.eterultimate.eteruee.shell.createEmbeddedTermuxSession
import com.eterultimate.eteruee.ui.components.nav.BackButton
import com.termux.view.TerminalView
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ComputerTerminal01
import me.rerere.hugeicons.stroke.Play

@Composable
fun ShellPage() {
    val context = LocalContext.current
    var title by remember { mutableStateOf("EterUee Shell") }
    var finished by remember { mutableStateOf(false) }
    var commandInput by remember { mutableStateOf("") }
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

    fun sendCommand() {
        val command = commandInput.trim()
        if (command.isEmpty() || !session.isRunning) return
        commandInput = ""
        session.write(command + "\r")
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
                            val textSizePx = (14 * viewContext.resources.displayMetrics.scaledDensity).toInt()
                            setTerminalViewClient(client)
                            setTextSize(textSizePx.coerceAtLeast(12))
                            attachSession(session)
                            requestFocus()
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    enabled = session.isRunning,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.shell_page_command_hint)) },
                    leadingIcon = {
                        Icon(HugeIcons.ComputerTerminal01, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendCommand() }),
                )
                IconButton(
                    onClick = { sendCommand() },
                    enabled = commandInput.isNotBlank() && session.isRunning,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = HugeIcons.Play,
                        contentDescription = stringResource(R.string.shell_page_execute),
                    )
                }
            }
        }
    }
}
