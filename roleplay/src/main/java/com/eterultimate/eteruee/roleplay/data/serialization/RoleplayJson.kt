package com.eterultimate.eteruee.roleplay.data.serialization

import com.eterultimate.eteruee.ai.util.InstantSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.json.Json
import java.time.Instant

internal val RoleplayJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    prettyPrint = true
    serializersModule = SerializersModule {
        contextual(Instant::class, InstantSerializer)
    }
}

internal val CompactRoleplayJson = Json(RoleplayJson) {
    prettyPrint = false
}
