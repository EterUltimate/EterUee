package me.rerere.ai.provider

import kotlin.uuid.Uuid

class ProviderManager {
    private val providers: MutableMap<Uuid, AIProvider<*>> = hashMapOf()

    fun getProviderByUUID(uuid: Uuid) = providers[uuid]
}