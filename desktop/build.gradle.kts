import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale

private val desktopPackageName = "EterUee"
private val desktopLinuxPackageName = "eteruee"
private val desktopPackageVersion = "5.2.5"
private val hostOsName = System.getProperty("os.name").lowercase(Locale.ROOT)
private val isWindowsHost = hostOsName.contains("win")
private val isLinuxHost = hostOsName.contains("linux")
private val nativePackageTaskForCurrentHost = when {
    isWindowsHost -> "packageReleaseExe"
    isLinuxHost -> "packageReleaseDeb"
    else -> null
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.eterultimate.eteruee.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Deb)
            packageName = desktopPackageName
            packageVersion = desktopPackageVersion
            description = "EterUee desktop client"
            copyright = "Copyright (C) 2026 EterUltimate"
            vendor = "EterUltimate"

            windows {
                packageName = desktopPackageName
                menuGroup = "EterUltimate"
                dirChooser = true
                perUserInstall = true
                shortcut = true
                upgradeUuid = "18bd089b-48de-4a45-b2e5-7202f8693c80"
            }

            linux {
                packageName = desktopLinuxPackageName
                debMaintainer = "EterUltimate <noreply@example.com>"
                menuGroup = "Utility"
                shortcut = true
            }
        }
    }
}

private val releaseBinariesDir = layout.buildDirectory.dir("compose/binaries/main-release")
private val appImageManifestFile = releaseBinariesDir.map { it.file("desktop-app-image-manifest.txt") }
private val releaseManifestFile = releaseBinariesDir.map { it.file("desktop-release-manifest.txt") }

private fun expectedDesktopLauncher(appImageRoot: File): File = when {
    isWindowsHost -> appImageRoot.resolve("$desktopPackageName/$desktopPackageName.exe")
    isLinuxHost -> appImageRoot.resolve("$desktopPackageName/bin/$desktopPackageName")
    else -> appImageRoot.resolve("$desktopPackageName/$desktopPackageName")
}

private fun nativeInstallerArtifacts(releaseRoot: File): List<File> {
    val format = when {
        isWindowsHost -> "exe"
        isLinuxHost -> "deb"
        else -> return emptyList()
    }
    val extension = ".$format"
    return releaseRoot
        .resolve(format)
        .listFiles { file -> file.isFile && file.name.endsWith(extension) }
        .orEmpty()
        .sortedBy { it.name }
}

private fun desktopReleaseArtifacts(releaseRoot: File): List<File> {
    val launcher = expectedDesktopLauncher(releaseRoot.resolve("app"))
    return (listOf(launcher) + nativeInstallerArtifacts(releaseRoot))
        .filter { it.isFile }
        .sortedBy { it.relativeTo(releaseRoot).invariantSeparatorsPath }
}

private fun desktopAppImageArtifacts(releaseRoot: File): List<File> {
    val launcher = expectedDesktopLauncher(releaseRoot.resolve("app"))
    return listOf(launcher)
        .filter { it.isFile }
        .sortedBy { it.relativeTo(releaseRoot).invariantSeparatorsPath }
}

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
}

tasks.register("verifyDesktopReleaseDistributable") {
    group = "verification"
    description = "Verifies the Compose Desktop release app image contains a runnable launcher."
    dependsOn("createReleaseDistributable")

    doLast {
        val appImageRoot = releaseBinariesDir.get().dir("app").asFile
        val launcher = expectedDesktopLauncher(appImageRoot)

        check(appImageRoot.isDirectory) {
            "Expected desktop app image directory at ${appImageRoot.absolutePath}"
        }
        check(launcher.isFile && launcher.length() > 0L) {
            "Expected desktop launcher at ${launcher.absolutePath}"
        }
    }
}

tasks.register("verifyDesktopReleasePackage") {
    group = "verification"
    description = "Verifies the native desktop installer produced for the current operating system."
    nativePackageTaskForCurrentHost?.let { dependsOn(it) }

    doLast {
        check(nativePackageTaskForCurrentHost != null) {
            "Desktop native package pipeline supports Windows EXE and Linux DEB hosts only."
        }

        val releaseRoot = releaseBinariesDir.get().asFile
        val installers = nativeInstallerArtifacts(releaseRoot)

        check(installers.isNotEmpty()) {
            "Expected native desktop installer under ${releaseRoot.absolutePath}"
        }
        installers.forEach { installer ->
            check(installer.length() > 0L) {
                "Desktop installer is empty: ${installer.absolutePath}"
            }
        }
    }
}

fun writeDesktopManifest(
    outputFile: Provider<RegularFile>,
    includeNativeInstaller: Boolean,
    title: String,
) {
    val releaseRoot = releaseBinariesDir.get().asFile
    val artifacts = if (includeNativeInstaller) {
        desktopReleaseArtifacts(releaseRoot)
    } else {
        desktopAppImageArtifacts(releaseRoot)
    }
    check(artifacts.isNotEmpty()) {
        "No desktop release artifacts found under ${releaseRoot.absolutePath}"
    }

    val manifest = buildList {
        add(title)
        add("packageName=$desktopPackageName")
        add("packageVersion=$desktopPackageVersion")
        add("hostOs=${System.getProperty("os.name")}")
        add("hostArch=${System.getProperty("os.arch")}")
        add("nativeInstaller=$includeNativeInstaller")
        add("generatedAt=${Instant.now()}")
        add("")
        add("sha256 size path")
        artifacts.forEach { artifact ->
            add(
                listOf(
                    artifact.sha256(),
                    artifact.length().toString(),
                    artifact.relativeTo(releaseRoot).invariantSeparatorsPath,
                ).joinToString(" ")
            )
        }
    }

    val output = outputFile.get().asFile
    output.parentFile.mkdirs()
    output.writeText(manifest.joinToString(System.lineSeparator()) + System.lineSeparator())
    logger.lifecycle("Wrote desktop manifest: ${output.absolutePath}")
}

tasks.register("writeDesktopAppImageManifest") {
    group = "distribution"
    description = "Writes a SHA-256 manifest for the desktop release app image launcher."
    dependsOn("verifyDesktopReleaseDistributable")

    outputs.file(appImageManifestFile)

    doLast {
        writeDesktopManifest(
            outputFile = appImageManifestFile,
            includeNativeInstaller = false,
            title = "EterUee Desktop App Image Manifest",
        )
    }
}

tasks.register("writeDesktopReleaseManifest") {
    group = "distribution"
    description = "Writes a SHA-256 manifest for the desktop app launcher and native installer."
    dependsOn("verifyDesktopReleaseDistributable", "verifyDesktopReleasePackage")

    outputs.file(releaseManifestFile)

    doLast {
        writeDesktopManifest(
            outputFile = releaseManifestFile,
            includeNativeInstaller = true,
            title = "EterUee Desktop Release Manifest",
        )
    }
}

tasks.register("desktopReleaseAppImage") {
    group = "distribution"
    description = "Runs desktop tests, builds the release app image, verifies the launcher, and writes checksums."
    dependsOn("test", "writeDesktopAppImageManifest")
}

tasks.register("desktopReleasePackage") {
    group = "distribution"
    description = "Runs desktop tests, builds the release app image, packages the native installer, and writes checksums."
    dependsOn("test", "writeDesktopReleaseManifest")
}
