package me.rerere.ai

import me.rerere.ai.registry.ModelRegistry
import org.junit.Test

class ModelRegistryTest {
    @Test
    fun testGPT5() {
        assert(ModelRegistry.GPT_5.match("gpt-5"))
        assert(!ModelRegistry.GPT_5.match("gpt-5-chat"))
        assert(ModelRegistry.GPT_5.match("gpt-5-mini"))
        assert(!ModelRegistry.GPT_5.match("deepseek-v3"))
        assert(!ModelRegistry.GPT_5.match("gemini-2.0-flash"))
        assert(!ModelRegistry.GPT_5.match("gpt-5.1"))
        assert(!ModelRegistry.GPT_5.match("gpt-4o"))
        assert(!ModelRegistry.GPT_5.match("gpt-5.0"))
        assert(!ModelRegistry.GPT_5.match("gpt-6"))
    }
}
