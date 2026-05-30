package com.eterultimate.eteruee.ui.pages.ssh

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.ui.components.nav.BackButton
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ServerStack01

private const val TAG = "SshPage"

private data class SshTerminalSession(
    val session: Session,
    val channel: ChannelShell,
    val input: OutputStream,
)

private fun openSshShell(
    host: String,
    port: Int,
    username: String,
    password: String?,
    privateKey: String?,
    passphrase: String?,
    timeoutSeconds: Int,
): SshTerminalSession {
    var session: Session? = null

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

        val channel = session.openChannel("shell") as ChannelShell
        channel.setPty(true)
        channel.setPtyType("xterm")
        channel.connect(timeoutSeconds * 1000)
        return SshTerminalSession(session, channel, channel.outputStream)
    } catch (e: Exception) {
        Log.e(TAG, "SSH shell failed", e)
        session?.disconnect()
        throw e
    }
}

private fun SshTerminalSession.close() {
    runCatching { input.close() }
    runCatching { channel.disconnect() }
    runCatching { session.disconnect() }
}

private fun SshTerminalSession.sendLine(command: String) {
    input.write((command + "\n").toByteArray(Charsets.UTF_8))
    input.flush()
}

private suspend fun readSshShellOutput(
    terminalSession: SshTerminalSession,
    onOutput: (String) -> Unit,
) = withContext(Dispatchers.IO) {
    val input = terminalSession.channel.inputStream
    val buffer = ByteArray(4096)
    while (isActive && terminalSession.channel.isConnected && !terminalSession.channel.isClosed) {
        val available = input.available()
        if (available > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size, available))
            if (read > 0) {
                val chunk = String(buffer, 0, read, Charsets.UTF_8)
                withContext(Dispatchers.Main) {
                    onOutput(chunk)
                }
            }
        } else {
            Thread.sleep(50)
        }
    }
}

@Composable
fun SshPage() {
    val scope = rememberCoroutineScope()
    var terminalSession by remember { mutableStateOf<SshTerminalSession?>(null) }
    var readJob by remember { mutableStateOf<Job?>(null) }
    var terminalOutput by remember { mutableStateOf("") }

    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("") }
    var useKeyAuth by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }

    var commandInput by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }

    fun canConnect(): Boolean {
        if (isConnecting || terminalSession != null) return false
        if (host.isBlank() || username.isBlank()) return false
        if (!useKeyAuth && password.isBlank()) return false
        if (useKeyAuth && privateKey.isBlank()) return false
        return true
    }

    fun disconnect() {
        readJob?.cancel()
        readJob = null
        terminalSession?.close()
        terminalSession = null
    }

    fun connect() {
        if (!canConnect()) return
        val portNum = port.toIntOrNull() ?: 22
        isConnecting = true
        terminalOutput = "Connecting to $username@$host:$portNum...\n"

        scope.launch(Dispatchers.IO) {
            try {
                val opened = openSshShell(
                    host = host,
                    port = portNum,
                    username = username,
                    password = if (useKeyAuth) null else password,
                    privateKey = if (useKeyAuth) privateKey else null,
                    passphrase = if (useKeyAuth) passphrase.takeIf { it.isNotBlank() } else null,
                    timeoutSeconds = 30,
                )
                withContext(Dispatchers.Main) {
                    terminalSession = opened
                    terminalOutput += "Connected.\n"
                    readJob = scope.launch {
                        readSshShellOutput(opened) { chunk -> terminalOutput += chunk }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    terminalOutput += "Connection failed: ${e.message ?: e.javaClass.simpleName}\n"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isConnecting = false
                }
            }
        }
    }

    fun sendCurrentCommand() {
        val currentSession = terminalSession ?: return
        val line = commandInput
        if (line.isBlank()) return
        commandInput = ""
        scope.launch(Dispatchers.IO) {
            runCatching { currentSession.sendLine(line) }
                .onFailure { error ->
                    withContext(Dispatchers.Main) {
                        terminalOutput += "\n[send failed] ${error.message ?: error.javaClass.simpleName}\n"
                    }
                }
        }
    }

    DisposableEffect(Unit) {
        onDispose { disconnect() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ssh_page_title)) },
                navigationIcon = { BackButton() },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.ssh_page_connection),
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedTextField(
                                value = host,
                                onValueChange = { host = it },
                                modifier = Modifier.weight(2f),
                                label = { Text(stringResource(R.string.ssh_page_host)) },
                                singleLine = true,
                                leadingIcon = { Icon(HugeIcons.ServerStack01, null) },
                            )
                            OutlinedTextField(
                                value = port,
                                onValueChange = { port = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.ssh_page_port)) },
                                singleLine = true,
                            )
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.ssh_page_username)) },
                            singleLine = true,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = !useKeyAuth,
                                onClick = { useKeyAuth = false },
                                label = { Text(stringResource(R.string.ssh_page_password_auth)) },
                            )
                            FilterChip(
                                selected = useKeyAuth,
                                onClick = { useKeyAuth = true },
                                label = { Text(stringResource(R.string.ssh_page_key_auth)) },
                            )
                        }

                        if (!useKeyAuth) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.ssh_page_password)) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                            )
                        } else {
                            OutlinedTextField(
                                value = privateKey,
                                onValueChange = { privateKey = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.ssh_page_private_key)) },
                                minLines = 4,
                                maxLines = 8,
                            )
                            OutlinedTextField(
                                value = passphrase,
                                onValueChange = { passphrase = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.ssh_page_passphrase)) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            if (terminalSession == null) {
                                Button(onClick = { connect() }, enabled = canConnect()) {
                                    Text(if (isConnecting) "Connecting" else "Connect")
                                }
                            } else {
                                Button(onClick = { disconnect() }) {
                                    Text("Disconnect")
                                }
                            }
                        }
                    }
                }
            }

            item {
                SshTerminalPane(
                    host = host.ifBlank { "host" },
                    username = username.ifBlank { "user" },
                    output = terminalOutput,
                    commandInput = commandInput,
                    onCommandInputChange = { commandInput = it },
                    isConnected = terminalSession != null,
                    onExecute = { sendCurrentCommand() },
                )
            }
        }
    }
}

@Composable
private fun SshTerminalPane(
    host: String,
    username: String,
    output: String,
    commandInput: String,
    onCommandInputChange: (String) -> Unit,
    isConnected: Boolean,
    onExecute: () -> Unit,
) {
    val terminalColor = Color(0xFF101418)
    val terminalText = Color(0xFFE7ECEF)
    val promptColor = Color(0xFF8BD5A7)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp),
        colors = CardDefaults.cardColors(containerColor = terminalColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (output.isBlank()) {
                Text(
                    text = stringResource(R.string.ssh_page_empty_hint),
                    color = terminalText.copy(alpha = 0.64f),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                SelectionContainer {
                    Text(
                        text = output,
                        color = terminalText,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$username@$host$ ",
                    color = promptColor,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
                BasicTextField(
                    value = commandInput,
                    onValueChange = onCommandInputChange,
                    enabled = isConnected,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = terminalText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onExecute() }),
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Transparent),
                )
                if (!isConnected) {
                    Text(
                        text = " disconnected",
                        color = terminalText.copy(alpha = 0.64f),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
