package com.eterultimate.eteruee.ui.pages.ssh

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Play
import me.rerere.hugeicons.stroke.ServerStack01
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.ui.components.nav.BackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SshPage"

private data class SshResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
)

private data class SshHistoryItem(
    val command: String,
    val result: SshResult,
    val host: String,
    val timestamp: Long = System.currentTimeMillis(),
)

private fun executeSshCommand(
    host: String,
    port: Int,
    username: String,
    password: String?,
    privateKey: String?,
    passphrase: String?,
    command: String,
    timeoutSeconds: Int,
): SshResult {
    var session: Session? = null
    var channel: ChannelExec? = null

    try {
        val jsch = JSch()

        if (!privateKey.isNullOrBlank()) {
            val keyBytes = privateKey.toByteArray(Charsets.UTF_8)
            if (!passphrase.isNullOrBlank()) {
                jsch.addIdentity("ssh_key", keyBytes, null, passphrase.toByteArray(Charsets.UTF_8))
            } else {
                jsch.addIdentity("ssh_key", keyBytes, null, null)
            }
        }

        session = jsch.getSession(username, host, port)

        if (!password.isNullOrBlank() && privateKey.isNullOrBlank()) {
            session.setPassword(password)
        }

        session.setConfig("StrictHostKeyChecking", "no")
        session.setConfig("PreferredAuthentications", "publickey,password")
        session.timeout = timeoutSeconds * 1000

        session.userInfo = object : UserInfo {
            override fun getPassword(): String? = password
            override fun getPassphrase(): String? = passphrase
            override fun promptYesNo(message: String?): Boolean = true
            override fun promptPassphrase(message: String?): Boolean = !passphrase.isNullOrBlank()
            override fun promptPassword(message: String?): Boolean = !password.isNullOrBlank()
            override fun showMessage(message: String?) {
                Log.d(TAG, "SSH: $message")
            }
        }

        session.connect(timeoutSeconds * 1000)

        channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        channel.setPty(false)
        channel.connect(timeoutSeconds * 1000)

        val stdout = channel.inputStream.bufferedReader(Charsets.UTF_8).readText()
        val stderr = channel.errStream.bufferedReader(Charsets.UTF_8).readText()

        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
        while (!channel.isClosed) {
            if (System.currentTimeMillis() > deadline) {
                channel.disconnect()
                return SshResult(
                    stdout = stdout,
                    stderr = stderr + "\n[TIMEOUT] Command timed out after ${timeoutSeconds}s",
                    exitCode = -1,
                )
            }
            Thread.sleep(100)
        }

        return SshResult(
            stdout = stdout,
            stderr = stderr,
            exitCode = channel.exitStatus,
        )
    } catch (e: Exception) {
        Log.e(TAG, "SSH execution failed", e)
        return SshResult(
            stdout = "",
            stderr = e.message ?: e.javaClass.simpleName,
            exitCode = -1,
        )
    } finally {
        channel?.disconnect()
        session?.disconnect()
    }
}

@Composable
fun SshPage() {
    val scope = rememberCoroutineScope()
    val history = remember { mutableStateListOf<SshHistoryItem>() }

    // Connection fields
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("") }
    var useKeyAuth by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }

    // Command execution
    var commandInput by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ssh_page_title)) },
                navigationIcon = { BackButton() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (commandInput.isBlank() || isExecuting) return@FloatingActionButton
                    if (host.isBlank() || username.isBlank()) return@FloatingActionButton
                    if (!useKeyAuth && password.isBlank()) return@FloatingActionButton
                    if (useKeyAuth && privateKey.isBlank()) return@FloatingActionButton

                    val cmd = commandInput.trim()
                    val portNum = port.toIntOrNull() ?: 22
                    commandInput = ""
                    isExecuting = true

                    scope.launch(Dispatchers.IO) {
                        val result = executeSshCommand(
                            host = host,
                            port = portNum,
                            username = username,
                            password = if (useKeyAuth) null else password,
                            privateKey = if (useKeyAuth) privateKey else null,
                            passphrase = if (useKeyAuth) passphrase.takeIf { it.isNotBlank() } else null,
                            command = cmd,
                            timeoutSeconds = 30,
                        )
                        withContext(Dispatchers.Main) {
                            history.add(SshHistoryItem(cmd, result, host))
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
                        contentDescription = stringResource(R.string.ssh_page_execute),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Connection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ssh_page_connection),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = host,
                                onValueChange = { host = it },
                                modifier = Modifier.weight(2f),
                                label = { Text(stringResource(R.string.ssh_page_host)) },
                                singleLine = true,
                                leadingIcon = { Icon(HugeIcons.ServerStack01, null) }
                            )
                            OutlinedTextField(
                                value = port,
                                onValueChange = { port = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.ssh_page_port)) },
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.ssh_page_username)) },
                            singleLine = true
                        )

                        // Auth method selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !useKeyAuth,
                                onClick = { useKeyAuth = false },
                                label = { Text(stringResource(R.string.ssh_page_password_auth)) }
                            )
                            FilterChip(
                                selected = useKeyAuth,
                                onClick = { useKeyAuth = true },
                                label = { Text(stringResource(R.string.ssh_page_key_auth)) }
                            )
                        }

                        if (!useKeyAuth) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.ssh_page_password)) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                        } else {
                            OutlinedTextField(
                                value = privateKey,
                                onValueChange = { privateKey = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.ssh_page_private_key)) },
                                minLines = 4,
                                maxLines = 8
                            )
                            OutlinedTextField(
                                value = passphrase,
                                onValueChange = { passphrase = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.ssh_page_passphrase)) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                        }
                    }
                }
            }

            // Command input
            item {
                HorizontalDivider()
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.ssh_page_command_hint)) },
                    label = { Text(stringResource(R.string.ssh_page_command_label)) },
                    singleLine = true,
                    leadingIcon = { Icon(HugeIcons.ServerStack01, null) }
                )
            }

            // Output history
            items(history.reversed()) { item ->
                SshOutputCard(item = item)
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
                            text = stringResource(R.string.ssh_page_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SshOutputCard(item: SshHistoryItem) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.host,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$ ${item.command}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            }

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
