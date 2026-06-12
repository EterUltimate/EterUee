package com.eterultimate.eteruee.web.routes

import com.eterultimate.eteruee.device.DeviceAgentManager
import com.eterultimate.eteruee.device.DeviceShellResult
import com.eterultimate.eteruee.linux.LinuxCommandResult
import com.eterultimate.eteruee.linux.LinuxEnvironmentManager
import com.eterultimate.eteruee.web.BadRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

fun Route.deviceRoutes(
    deviceAgentManager: DeviceAgentManager,
    linuxEnvironmentManager: LinuxEnvironmentManager,
) {
    route("/device") {
        get("/status") {
            call.respond(deviceAgentManager.getStatus())
        }

        post("/shizuku/request-permission") {
            call.respond(deviceAgentManager.requestShizukuPermission())
        }

        get("/info") {
            call.respond(deviceAgentManager.getDeviceInfo())
        }

        get("/apps") {
            val includeSystem = call.request.queryParameters["includeSystem"]?.toBooleanStrictOrNull() ?: false
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 250
            if (limit !in 1..1000) {
                throw BadRequestException("limit must be in 1..1000")
            }
            call.respond(deviceAgentManager.listInstalledApps(includeSystem = includeSystem, limit = limit))
        }

        post("/shell") {
            val request = call.receive<DeviceShellRequest>()
            if (request.command.isBlank()) {
                throw BadRequestException("command must not be blank")
            }
            val result: DeviceShellResult = deviceAgentManager.executeAdbShell(
                command = request.command,
                workingDir = request.workingDir,
                stdin = request.stdin,
                timeoutSeconds = request.timeoutSeconds,
            )
            call.respond(HttpStatusCode.OK, result)
        }

        route("/linux") {
            get("/status") {
                call.respond(linuxEnvironmentManager.getStatus())
            }

            post("/prepare") {
                call.respond(linuxEnvironmentManager.prepareInstallerScript())
            }

            post("/install") {
                val request = runCatching { call.receive<LinuxInstallRequest>() }
                    .getOrDefault(LinuxInstallRequest())
                val result = linuxEnvironmentManager.install(
                    timeoutSeconds = request.timeoutSeconds,
                )
                call.respond(HttpStatusCode.OK, result)
            }

            post("/shell") {
                val request = call.receive<DeviceShellRequest>()
                if (request.command.isBlank()) {
                    throw BadRequestException("command must not be blank")
                }
                val result: LinuxCommandResult = linuxEnvironmentManager.execute(
                    command = request.command,
                    workingDir = request.workingDir,
                    stdin = request.stdin,
                    timeoutSeconds = request.timeoutSeconds,
                )
                call.respond(HttpStatusCode.OK, result)
            }
        }
    }
}

@Serializable
data class LinuxInstallRequest(
    val timeoutSeconds: Int = 600,
)

@Serializable
data class DeviceShellRequest(
    val command: String,
    val workingDir: String? = null,
    val stdin: String? = null,
    val timeoutSeconds: Int = 30,
)
