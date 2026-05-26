package com.eterultimate.eteruee.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
enum class PlatformFamily {
    ANDROID,
    IOS,
    MACOS,
    WINDOWS,
}

@Serializable
data class AppleTarget(
    val family: PlatformFamily,
    val kotlinTarget: String,
    val runtime: String,
)

@Serializable
data class WindowsTarget(
    val family: PlatformFamily,
    val kotlinTarget: String,
    val runtime: String,
    val minimumOs: String,
)

@Serializable
data class SharedRuntimeCapabilities(
    val platformFamily: PlatformFamily,
    val appleTargets: List<AppleTarget>,
    val windowsTargets: List<WindowsTarget>,
    val generatedAtEpochMilliseconds: Long,
)

expect val currentPlatformFamily: PlatformFamily

object EterUeeShared {
    const val frameworkName: String = "EterUeeShared"

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val supportedAppleTargets: List<AppleTarget> = listOf(
        AppleTarget(
            family = PlatformFamily.IOS,
            kotlinTarget = "iosArm64",
            runtime = "iPhone/iPad device",
        ),
        AppleTarget(
            family = PlatformFamily.IOS,
            kotlinTarget = "iosSimulatorArm64",
            runtime = "Apple Silicon iPhone/iPad simulator",
        ),
        AppleTarget(
            family = PlatformFamily.IOS,
            kotlinTarget = "iosX64",
            runtime = "Intel iPhone/iPad simulator",
        ),
        AppleTarget(
            family = PlatformFamily.MACOS,
            kotlinTarget = "macosArm64",
            runtime = "Apple Silicon macOS",
        ),
    )

    val supportedWindowsTargets: List<WindowsTarget> = listOf(
        WindowsTarget(
            family = PlatformFamily.WINDOWS,
            kotlinTarget = "mingwX64",
            runtime = "Windows x64 native executable",
            minimumOs = "Windows 11",
        ),
    )

    @OptIn(ExperimentalTime::class)
    fun runtimeCapabilities(): SharedRuntimeCapabilities = SharedRuntimeCapabilities(
        platformFamily = currentPlatformFamily,
        appleTargets = supportedAppleTargets,
        windowsTargets = supportedWindowsTargets,
        generatedAtEpochMilliseconds = Clock.System.now().toEpochMilliseconds(),
    )

    fun runtimeCapabilitiesJson(): String = json.encodeToString(runtimeCapabilities())
}
