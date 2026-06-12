package com.eterultimate.eteruee.plugin

enum class PluginPermission(val scope: String) {
    ConversationRead("conversation:read"),
    ConversationWrite("conversation:write"),
    AssistantRead("assistant:read"),
    AssistantWrite("assistant:write"),
    SettingsRead("settings:read"),
    FilesRead("files:read"),
    FilesWrite("files:write"),
    ToolsRead("tools:read"),
    ToolsExecute("tools:execute"),
    DeviceRead("device:read"),
    DeviceControl("device:control"),
    NetworkRelay("network:relay");

    companion object {
        fun fromScope(scope: String): PluginPermission? = entries.firstOrNull { it.scope == scope }
    }
}
