package me.rerere.rikkahub.ui.context

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

val LocalNavController = compositionLocalOf<NavBackStack> {
  error("No NavController provided")
}

fun NavBackStack.popBack() {
  if (this.size <= 1) return
  this.removeLastOrNull()
}

fun NavBackStack.push(key: NavKey) {
  this.add(key)
}

fun NavBackStack.replace(key: NavKey) {
  this.removeLastOrNull()
  this.add(key)
}

fun NavBackStack.pushSingleTop(key: NavKey) {
  this.clear()
  this.add(key)
}