package com.eterultimate.eteruee.ui.pages.shell

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ComputerTerminal01
import me.rerere.hugeicons.stroke.Play
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.ui.components.nav.BackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "ShellPage"
private const val TERMUX_PACKAGE = "com.termux"
private const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
private const val TERMUX_RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"

private val executor = Executors.newCachedThreadPool()

private fun isTermuxInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

private data class ShellResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val executor: String,
)

private fun executeViaTermux(context: Context, command: String): ShellResult {
    return try {
        val intent = Intent().apply {
            setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
            action = TERMUX_RUN_COMMAND_ACTION
            putExtra("com.termux.RUN_COMMAND_PATH", "/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
        }
        context.startService(intent)
        ShellResult(
            stdout = "",
            stderr = context.getString(R.string.shell_page_termux_output_hint),
            exitCode = 0,
            executor = "termux",
        )
    } catch (e: Exception) {
        Log.e(TAG, "Termux execution failed", e)
        executeViaRuntime(command, 30)
    }
}

private fun executeViaRuntime(command: String, timeoutSeconds: Int): ShellResult {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        process.outputStream.close()

        val stdoutFuture = executor.submit(Callable {
            process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        })
        val stderrFuture = executor.submit(Callable {
            process.errorStream.bufferedReader(Charsets.UTF_8).readText()
        })

        val completed = process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)

        val stdout = try {
            stdoutFuture.get(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        } catch (_: Exception) {
            ""
        }
        val stderr = try {
            stderrFuture.get(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        } catch (_: Exception) {
            ""
        }

        if (!completed) {
            process.destroyForcibly()
            ShellResult(
                stdout = stdout,
                stderr = stderr + "\n[TIMEOUT] Command timed out after ${timeoutSeconds}s",
                exitCode = -1,
                executor = "runtime",
            )
        } else {
            ShellResult(
                stdout = stdout,
                stderr = stderr,
                exitCode = process.exitValue(),
                executor = "runtime",
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Runtime execution failed", e)
        ShellResult(
            stdout = "",
            stderr = e.message ?: e.javaClass.simpleName,
            exitCode = -1,
            executor = "runtime",
        )
    }
}

private data class ShellHistoryItem(
    val command: String,
    val result: ShellResult,
    val timestamp: Long = System.currentTimeMillis(),
)

@Composable
fun ShellPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val history = remember { mutableStateListOf<ShellHistoryItem>() }
    var commandInput by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }
    val termuxInstalled = remember { isTermuxInstalled(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shell_page_title)) },
                navigationIcon = { BackButton() },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (termuxInstalled) Color(0xFF00C853) else Color(0xFFFF5252),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Text(
                            text = if (termuxInstalled) "Termux" else "Runtime.exec",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (commandInput.isBlank() || isExecuting) return@FloatingActionButton
                    val cmd = commandInput.trim()
                    commandInput = ""
                    isExecuting = true
                    scope.launch(Dispatchers.IO) {
                        val result = if (termuxInstalled) {
                            executeViaTermux(context, cmd)
                        } else {
                            executeViaRuntime(cmd, 30)
                        }
                        withContext(Dispatchers.Main) {
                            history.add(ShellHistoryItem(cmd, result))
                            isExecuting = false
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        HugeIcons.Play,
                        contentDescription = stringResource(R.string.shell_page_execute),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Command input
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.shell_page_command_hint)) },
                label = { Text(stringResource(R.string.shell_page_command_label)) },
                singleLine = true,
                leadingIcon = {
                    Icon(HugeIcons.ComputerTerminal01, null)
                },
                trailingIcon = {
                    if (commandInput.isNotBlank()) {
                        IconButton(onClick = { commandInput = "" }) {
                            Text("×", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Output history
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history.reversed()) { item ->
                    ShellOutputCard(item = item)
                }

                if (history.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.shell_page_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShellOutputCard(item: ShellHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Command line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$",
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = item.command,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "[${item.result.executor}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Exit code indicator
            val exitColor = when {
                item.result.exitCode == 0 -> Color(0xFF00C853)
                else -> Color(0xFFFF5252)
            }
            Text(
                text = "exit: ${item.result.exitCode}",
                style = MaterialTheme.typography.labelSmall,
                color = exitColor,
                fontFamily = FontFamily.Monospace
            )

            // Stdout
            if (item.result.stdout.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = item.result.stdout,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Stderr
            if (item.result.stderr.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = item.result.stderr,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.result.exitCode == 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
