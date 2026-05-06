package com.eterultimate.eteruee.data.ai.tools

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.eterultimate.eteruee.ai.core.InputSchema
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.ui.UIMessagePart

object SshTools {

    private const val TAG = "SshTools"

    fun createSshExecuteTool(): Tool = Tool(
        name = "ssh_execute",
        description = """
            Execute a command on a remote server via SSH.
            Provide connection details (host, username) and the command to run.
            Supports password and private key authentication.
            Returns the command's stdout, stderr, and exit code.
            Use this to run shell commands, check server status, manage files, etc.
        """.trimIndent().replace("\n", " "),
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("host", buildJsonObject {
                        put("type", "string")
                        put("description", "Hostname or IP address of the SSH server")
                    })
                    put("port", buildJsonObject {
                        put("type", "integer")
                        put("description", "SSH port number (default: 22)")
                        put("default", 22)
                    })
                    put("username", buildJsonObject {
                        put("type", "string")
                        put("description", "SSH login username")
                    })
                    put("password", buildJsonObject {
                        put("type", "string")
                        put("description", "SSH password (use password OR privateKey, not both)")
                    })
                    put("privateKey", buildJsonObject {
                        put("type", "string")
                        put("description", "SSH private key in PEM format (use password OR privateKey, not both). Supports RSA, ECDSA, Ed25519.")
                    })
                    put("passphrase", buildJsonObject {
                        put("type", "string")
                        put("description", "Passphrase for the private key (if encrypted)")
                    })
                    put("command", buildJsonObject {
                        put("type", "string")
                        put("description", "The shell command to execute on the remote server")
                    })
                    put("timeout", buildJsonObject {
                        put("type", "integer")
                        put("description", "Command execution timeout in seconds (default: 30)")
                        put("default", 30)
                    })
                },
                required = listOf("host", "username", "command")
            )
        },
        needsApproval = true,
        execute = { params ->
            val jsonObject = params.jsonObject
            val host = jsonObject["host"]?.jsonPrimitive?.contentOrNull
                ?: error("host is required")
            val port = jsonObject["port"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 22
            val username = jsonObject["username"]?.jsonPrimitive?.contentOrNull
                ?: error("username is required")
            val password = jsonObject["password"]?.jsonPrimitive?.contentOrNull
            val privateKey = jsonObject["privateKey"]?.jsonPrimitive?.contentOrNull
            val passphrase = jsonObject["passphrase"]?.jsonPrimitive?.contentOrNull
            val command = jsonObject["command"]?.jsonPrimitive?.contentOrNull
                ?: error("command is required")
            val timeout = jsonObject["timeout"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 30

            if (password.isNullOrBlank() && privateKey.isNullOrBlank()) {
                error("Either password or privateKey must be provided")
            }

            val result = executeSshCommand(
                host = host,
                port = port,
                username = username,
                password = password,
                privateKey = privateKey,
                passphrase = passphrase,
                command = command,
                timeoutSeconds = timeout,
            )

            val payload = buildJsonObject {
                put("stdout", result.stdout)
                put("stderr", result.stderr)
                put("exitCode", result.exitCode)
                put("host", host)
                put("command", command)
            }
            listOf(UIMessagePart.Text(payload.toString()))
        }
    )

    private data class SshResult(
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
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

            // 私钥认证
            if (!privateKey.isNullOrBlank()) {
                val keyBytes = privateKey.toByteArray(Charsets.UTF_8)
                if (!passphrase.isNullOrBlank()) {
                    jsch.addIdentity("ssh_key", keyBytes, null, passphrase.toByteArray(Charsets.UTF_8))
                } else {
                    jsch.addIdentity("ssh_key", keyBytes, null, null)
                }
            }

            session = jsch.getSession(username, host, port)

            // 密码认证
            if (!password.isNullOrBlank() && privateKey.isNullOrBlank()) {
                session.setPassword(password)
            }

            // 严格主机密钥检查 — 首次连接自动接受
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "publickey,password")

            // 连接超时
            session.timeout = timeoutSeconds * 1000

            // 密码交互回调
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

            // 读取 stdout
            val stdout = channel.inputStream.bufferedReader(Charsets.UTF_8).readText()
            // 读取 stderr
            val stderr = channel.errStream.bufferedReader(Charsets.UTF_8).readText()

            // 等待命令执行完成
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
}
