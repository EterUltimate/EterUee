package me.rerere.rikkahub.utils

fun Number.toFixed(digits: Int = 0) = "%.${digits}f".format(this)