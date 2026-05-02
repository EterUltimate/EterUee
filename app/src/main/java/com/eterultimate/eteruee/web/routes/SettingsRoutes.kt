package com.eterultimate.eteruee.web.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import com.eterultimate.eteruee.ai.provider.BuiltInTools
import com.eterultimate.eteruee.ai.provider.ModelType
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.datastore.findModelById
import com.eterultimate.eteruee.utils.JsonInstant
import com.eterultimate.eteruee.web.BadRequestException
import com.eterultimate.eteruee.web.NotFoundException
import com.eterultimate.eteruee.web.dto.UpdateAssistantModelRequest
import com.eterultimate.eteruee.web.dto.UpdateAssistantRequest
import com.eterultimate.eteruee.web.dto.UpdateAssistantReasoningLevelRequest
import com.eterultimate.eteruee.web.dto.UpdateAssistantMcpServersRequest
import com.eterultimate.eteruee.web.dto.UpdateAssistantInjectionsRequest
import com.eterultimate.eteruee.web.dto.UpdateBuiltInToolRequest
import com.eterultimate.eteruee.web.dto.UpdateFavoriteModelsRequest
import com.eterultimate.eteruee.web.dto.UpdateSearchEnabledRequest
import com.eterultimate.eteruee.web.dto.UpdateSearchServiceRequest
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

fun Route.settingsRoutes(
    settingsStore: SettingsStore
) {
    route("/settings") {
        post("/assistant") {
            val request = call.receive<UpdateAssistantRequest>()
            val assistantId = request.assistantId.toUuid("assistantId")

            settingsStore.updateAssistant(assistantId)
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/assistant/model") {
            val request = call.receive<UpdateAssistantModelRequest>()
            val assistantId = request.assistantId.toUuid("assistantId")
            val modelId = request.modelId.toUuid("modelId")

            val settings = settingsStore.settingsFlow.value
            if (settings.assistants.none { it.id == assistantId }) {
                throw NotFoundException("Assistant not found")
            }

            val model = settings.findModelById(modelId)
                ?: throw NotFoundException("Model not found")
            if (model.type != ModelType.CHAT) {
                throw BadRequestException("modelId must be a chat model")
            }

            settingsStore.updateAssistantModel(assistantId, modelId)
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/assistant/thinking-budget") {
            val request = call.receive<UpdateAssistantReasoningLevelRequest>()
            val assistantId = request.assistantId.toUuid("assistantId")

            val settings = settingsStore.settingsFlow.value
            if (settings.assistants.none { it.id == assistantId }) {
                throw NotFoundException("Assistant not found")
            }

            settingsStore.updateAssistantReasoningLevel(assistantId, request.reasoningLevel)
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/assistant/mcp") {
            val request = call.receive<UpdateAssistantMcpServersRequest>()
            val assistantId = request.assistantId.toUuid("assistantId")

            val settings = settingsStore.settingsFlow.value
            if (settings.assistants.none { it.id == assistantId }) {
                throw NotFoundException("Assistant not found")
            }

            val validServerIds = settings.mcpServers.map { it.id }.toSet()
            val requestedServerIds = request.mcpServerIds.map { it.toUuid("mcpServerIds") }.toSet()
            if (!validServerIds.containsAll(requestedServerIds)) {
                throw BadRequestException("mcpServerIds contains unknown server id")
            }

            settingsStore.updateAssistantMcpServers(assistantId, requestedServerIds)
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/assistant/injections") {
            val request = call.receive<UpdateAssistantInjectionsRequest>()
            val assistantId = request.assistantId.toUuid("assistantId")

            val settings = settingsStore.settingsFlow.value
            if (settings.assistants.none { it.id == assistantId }) {
                throw NotFoundException("Assistant not found")
            }

            val validModeInjectionIds = settings.modeInjections.map { it.id }.toSet()
            val requestedModeInjectionIds =
                request.modeInjectionIds.map { it.toUuid("modeInjectionIds") }.toSet()
            if (!validModeInjectionIds.containsAll(requestedModeInjectionIds)) {
                throw BadRequestException("modeInjectionIds contains unknown injection id")
            }

            val validLorebookIds = settings.lorebooks.map { it.id }.toSet()
            val requestedLorebookIds = request.lorebookIds.map { it.toUuid("lorebookIds") }.toSet()
            if (!validLorebookIds.containsAll(requestedLorebookIds)) {
                throw BadRequestException("lorebookIds contains unknown lorebook id")
            }

            val validQuickMessageIds = settings.quickMessages.map { it.id }.toSet()
            val requestedQuickMessageIds =
                request.quickMessageIds.map { it.toUuid("quickMessageIds") }.toSet()
            if (!validQuickMessageIds.containsAll(requestedQuickMessageIds)) {
                throw BadRequestException("quickMessageIds contains unknown quick message id")
            }

            settingsStore.updateAssistantInjections(
                assistantId = assistantId,
                modeInjectionIds = requestedModeInjectionIds,
                lorebookIds = requestedLorebookIds,
                quickMessageIds = requestedQuickMessageIds,
            )
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/search/enabled") {
            val request = call.receive<UpdateSearchEnabledRequest>()

            settingsStore.update { settings ->
                settings.copy(enableWebSearch = request.enabled)
            }
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/search/service") {
            val request = call.receive<UpdateSearchServiceRequest>()

            settingsStore.update { settings ->
                if (settings.searchServices.isEmpty()) {
                    throw BadRequestException("No search services configured")
                }
                if (request.index !in settings.searchServices.indices) {
                    throw BadRequestException("search service index out of range")
                }
                settings.copy(searchServiceSelected = request.index)
            }
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/model/built-in-tool") {
            val request = call.receive<UpdateBuiltInToolRequest>()
            val modelId = request.modelId.toUuid("modelId")
            val targetTool = parseBuiltInTool(request.tool)

            settingsStore.update { settings ->
                val model = settings.findModelById(modelId)
                    ?: throw NotFoundException("Model not found")
                if (model.type != ModelType.CHAT) {
                    throw BadRequestException("modelId must be a chat model")
                }

                val updatedModel = model.copy(
                    tools = if (request.enabled) {
                        model.tools + targetTool
                    } else {
                        model.tools - targetTool
                    }
                )

                settings.copy(
                    providers = settings.providers.map { provider ->
                        provider.editModel(updatedModel)
                    }
                )
            }

            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/favorite-models") {
            val request = call.receive<UpdateFavoriteModelsRequest>()
            val favoriteModelIds = request.modelIds.map { it.toUuid("modelId") }

            settingsStore.update { settings ->
                settings.copy(favoriteModels = favoriteModelIds)
            }
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        sse("/stream") {
            heartbeat {
                period = 15.seconds
            }
            settingsStore.settingsFlow
                .collect { settings ->
                    val json = JsonInstant.encodeToString(settings)
                    send(data = json, event = "update")
                }
        }
    }
}

private fun parseBuiltInTool(tool: String): BuiltInTools {
    return when (tool.trim().lowercase(Locale.ROOT)) {
        "search" -> BuiltInTools.Search
        "url_context", "url-context", "urlcontext" -> BuiltInTools.UrlContext
        "image_generation", "image-generation", "imagegeneration" -> BuiltInTools.ImageGeneration
        else -> throw BadRequestException("Unsupported built-in tool")
    }
}

