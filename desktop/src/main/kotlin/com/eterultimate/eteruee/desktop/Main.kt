package com.eterultimate.eteruee.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.eterultimate.eteruee.shared.EterUeeShared
import com.eterultimate.eteruee.shared.PlatformFamily
import com.eterultimate.eteruee.shared.WebUiConfig
import com.eterultimate.eteruee.shared.currentPlatformFamily
import com.eterultimate.eteruee.shared.roleplay.RoleplayPromptBuildRequest
import com.eterultimate.eteruee.shared.roleplay.RoleplayPromptBuildResult
import com.eterultimate.eteruee.shared.roleplay.SharedChatMessage
import com.eterultimate.eteruee.shared.roleplay.SharedInsertionPosition
import com.eterultimate.eteruee.shared.roleplay.SharedMessageRole
import com.eterultimate.eteruee.shared.roleplay.SharedWorldInfoEntry
import com.eterultimate.eteruee.shared.roleplay.RoleplayPromptEngine
import java.awt.Desktop
import java.net.URI

private enum class DesktopWorkspace {
    AGENT,
    ROLEPLAY,
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "EterUee Desktop",
        state = rememberWindowState(width = 1180.dp, height = 760.dp),
    ) {
        DesktopApp()
    }
}

@Composable
fun DesktopApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1C5D99),
            secondary = Color(0xFF3B6C52),
            tertiary = Color(0xFF9A5A2E),
            surface = Color(0xFFF8FAFC),
            background = Color(0xFFF3F6F8),
        ),
    ) {
        var workspace by remember { mutableStateOf(DesktopWorkspace.AGENT) }

        Scaffold(
            topBar = {
                Header(
                    workspace = workspace,
                    onWorkspaceChange = { workspace = it },
                )
            },
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                RuntimePanel(
                    modifier = Modifier
                        .width(290.dp)
                        .fillMaxHeight(),
                )

                when (workspace) {
                    DesktopWorkspace.AGENT -> AgentPanel(Modifier.weight(1f).fillMaxHeight())
                    DesktopWorkspace.ROLEPLAY -> RoleplayPanel(Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header(
    workspace: DesktopWorkspace,
    onWorkspaceChange: (DesktopWorkspace) -> Unit,
) {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("E", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("EterUee Desktop", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Native desktop package pipeline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = workspace == DesktopWorkspace.AGENT,
                    onClick = { onWorkspaceChange(DesktopWorkspace.AGENT) },
                    label = { Text("Agent") },
                )
                FilterChip(
                    selected = workspace == DesktopWorkspace.ROLEPLAY,
                    onClick = { onWorkspaceChange(DesktopWorkspace.ROLEPLAY) },
                    label = { Text("Roleplay") },
                )
            }
        }
    }
}

@Composable
private fun RuntimePanel(modifier: Modifier = Modifier) {
    val runtime = remember { DesktopRuntimeSummary.fromCurrentHost() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard {
            Text("Runtime", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            InfoRow("Host", runtime.hostLabel)
            InfoRow("Family", runtime.platformFamily.name)
            InfoRow("Shared", EterUeeShared.frameworkName)
        }

        SectionCard {
            Text("Installers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text("Windows: packageReleaseExe", style = MaterialTheme.typography.bodyMedium)
            Text("Linux: packageReleaseDeb", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Each native installer is built on its own operating system with jpackage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard {
            Text("Web UI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(
                "Desktop packaging is ready for the existing browser UI while Android-only services are migrated.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { openUri(WebUiConfig.LOCAL_URL) }) {
                Text("Open local Web UI")
            }
        }
    }
}

@Composable
private fun AgentPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Agent Workspace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

        SectionCard {
            Text("Development focus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text(
                "The desktop shell keeps Agent workflows separate from roleplay. The production Android app still owns " +
                    "provider credentials, persistent conversations, files, and device services until those layers move " +
                    "behind shared interfaces.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CapabilityCard(
                title = "Work handling",
                body = "Task-oriented conversations, tool calls, files, and provider settings.",
                modifier = Modifier.weight(1f),
            )
            CapabilityCard(
                title = "Packaging",
                body = "Windows EXE and Linux DEB installers from the Compose Desktop module.",
                modifier = Modifier.weight(1f),
            )
            CapabilityCard(
                title = "Migration path",
                body = "Move Android-free contracts into shared before wiring full desktop parity.",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RoleplayPanel(modifier: Modifier = Modifier) {
    var characterName by remember { mutableStateOf("Mira") }
    var scenario by remember { mutableStateOf("A quiet port city called Arcadia") }
    var userMessage by remember { mutableStateOf("Tell me what changed after the storm.") }
    var includeWorldInfo by remember { mutableStateOf(true) }

    val promptResult = remember(characterName, scenario, userMessage, includeWorldInfo) {
        buildPreviewPrompt(
            characterName = characterName,
            scenario = scenario,
            userMessage = userMessage,
            includeWorldInfo = includeWorldInfo,
        )
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Roleplay Workspace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            SectionCard {
                OutlinedTextField(
                    value = characterName,
                    onValueChange = { characterName = it },
                    label = { Text("Character") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = scenario,
                    onValueChange = { scenario = it },
                    label = { Text("World info") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    label = { Text("User message") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = includeWorldInfo,
                        onClick = { includeWorldInfo = !includeWorldInfo },
                        label = { Text("World info") },
                    )
                    TextButton(onClick = {
                        characterName = "Mira"
                        scenario = "Arcadia is a quiet port city used by the desktop host."
                        userMessage = "Tell me about Arcadia."
                        includeWorldInfo = true
                    }) {
                        Text("Reset")
                    }
                }
            }
        }

        SectionCard(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Prompt Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${promptResult.injectedEntryCount} entries",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            AnimatedVisibility(promptResult.truncatedMessageCount > 0) {
                Text(
                    "Truncated ${promptResult.truncatedMessageCount} messages",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFFFF), RoundedCornerShape(8.dp))
                    .padding(14.dp),
            ) {
                Text(
                    promptResult.prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun CapabilityCard(title: String, body: String, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun buildPreviewPrompt(
    characterName: String,
    scenario: String,
    userMessage: String,
    includeWorldInfo: Boolean,
): RoleplayPromptBuildResult {
    val worldInfo = if (includeWorldInfo) {
        listOf(
            SharedWorldInfoEntry(
                key = scenario.substringBefore(" ").ifBlank { "Arcadia" },
                content = scenario,
                position = SharedInsertionPosition.AFTER_SYSTEM_PROMPT,
                order = 1,
            ),
        )
    } else {
        emptyList()
    }

    return RoleplayPromptEngine.buildPrompt(
        RoleplayPromptBuildRequest(
            systemPrompt = "Write as $characterName. Stay in character and keep the scene consistent.",
            worldInfoEntries = worldInfo,
            messages = listOf(
                SharedChatMessage(
                    role = SharedMessageRole.USER,
                    content = userMessage,
                ),
            ),
            maxContextLength = 4096,
            matchWorldInfoAgainst = userMessage + "\n" + scenario,
        ),
    )
}

private data class DesktopRuntimeSummary(
    val platformFamily: PlatformFamily,
    val hostLabel: String,
) {
    companion object {
        fun fromCurrentHost(): DesktopRuntimeSummary {
            val osName = System.getProperty("os.name")
            val osArch = System.getProperty("os.arch")
            return DesktopRuntimeSummary(
                platformFamily = currentPlatformFamily,
                hostLabel = "$osName / $osArch",
            )
        }
    }
}

private fun openUri(uri: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(uri))
        }
    }
}
