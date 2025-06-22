package me.rerere.ai.core

enum class ReasoningLevel(val budgetTokens: Int) {
  OFF(0),
  LOW(1024),
  MEDIUM(16_000),
  HIGH(32_000);

  companion object {
    fun fromBudgetTokens(budgetTokens: Int): ReasoningLevel {
      return entries.minByOrNull { kotlin.math.abs(it.budgetTokens - budgetTokens) } ?: OFF
    }
  }
}