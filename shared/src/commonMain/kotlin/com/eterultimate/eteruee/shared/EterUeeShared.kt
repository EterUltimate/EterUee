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
}

@Serializable
data class AppleTarget(
    val family: PlatformFamily,
    val kotlinTarget: String,
    val runtime: String,
)

@Serializable
data class SharedRuntimeCapabilities(
    val platformFamily: PlatformFamily,
    val appleTargets: List<AppleTarget>,
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

    @OptIn(ExperimentalTime::class)
    fun runtimeCapabilities(): SharedRuntimeCapabilities = SharedRuntimeCapabilities(
        platformFamily = currentPlatformFamily,
        appleTargets = supportedAppleTargets,
        generatedAtEpochMilliseconds = Clock.System.now().toEpochMilliseconds(),
    )

    fun runtimeCapabilitiesJson(): String = json.encodeToString(runtimeCapabilities())
}
