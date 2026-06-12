package com.eterultimate.eteruee.linux

import android.content.Context
import android.os.Build
import com.eterultimate.eteruee.shell.LocalShellRunner
import com.github.luben.zstd.ZstdInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream

private const val EXECUTOR_NAME = "eteruee-archlinux-proot"
private const val INSTALL_EXECUTOR_NAME = "eteruee-archlinux-installer"
private const val DEFAULT_TIMEOUT_SECONDS = 60
private const val MAX_TIMEOUT_SECONDS = 600
private const val INSTALL_SCRIPT_NAME = "install-archlinux.sh"
private const val MAX_ROOTFS_ENTRY_BYTES = 512L * 1024 * 1024
private const val MAX_TERMUX_PACKAGE_ENTRY_BYTES = 64L * 1024 * 1024
private const val TERMUX_PREFIX_PATH = "data/data/com.termux/files/usr/"
private const val TERMUX_REPO_BASE_URL = "https://packages.termux.dev/apt/termux-main/"

private val ARCH_ROOTFS_SOURCES = mapOf(
    "arm64-v8a" to RootfsSource(
        url = "https://os.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz",
        fileName = "ArchLinuxARM-aarch64-latest.tar.gz",
        compression = RootfsCompression.Gzip,
        stripFirstPathSegment = false,
    ),
    "armeabi-v7a" to RootfsSource(
        url = "https://os.archlinuxarm.org/os/ArchLinuxARM-armv7-latest.tar.gz",
        fileName = "ArchLinuxARM-armv7-latest.tar.gz",
        compression = RootfsCompression.Gzip,
        stripFirstPathSegment = false,
    ),
    "x86_64" to RootfsSource(
        url = "https://geo.mirror.pkgbuild.com/iso/latest/archlinux-bootstrap-x86_64.tar.zst",
        fileName = "archlinux-bootstrap-x86_64.tar.zst",
        compression = RootfsCompression.Zstd,
        stripFirstPathSegment = true,
    ),
)

private val TERMUX_BOOTSTRAP_PACKAGES = mapOf(
    "arm64-v8a" to listOf(
        TermuxPackageSource(
            fileName = "proot_5.1.107.78-1_aarch64.deb",
            path = "pool/main/p/proot/proot_5.1.107.78-1_aarch64.deb",
            sha256 = "f703888191e7a1aade19882a36236507a39796c7e7016c57ed2aedd309b1a2c6",
        ),
        TermuxPackageSource(
            fileName = "libandroid-shmem_0.7_aarch64.deb",
            path = "pool/main/liba/libandroid-shmem/libandroid-shmem_0.7_aarch64.deb",
            sha256 = "0da3a24d558b93c92bcf8d611e0826a99ff96e396b148e6cdf33b47c47c57ff6",
        ),
        TermuxPackageSource(
            fileName = "libtalloc_2.4.3_aarch64.deb",
            path = "pool/main/libt/libtalloc/libtalloc_2.4.3_aarch64.deb",
            sha256 = "ac81ad623d74c209718b9f3acb2dd702cc8a88c431e820d212229910b4db29da",
        ),
    ),
    "armeabi-v7a" to listOf(
        TermuxPackageSource(
            fileName = "proot_5.1.107.78-1_arm.deb",
            path = "pool/main/p/proot/proot_5.1.107.78-1_arm.deb",
            sha256 = "7b8757fea6fa66dcd1964058329d92cc271936c6070c283f37455f4c7fc79ade",
        ),
        TermuxPackageSource(
            fileName = "libandroid-shmem_0.7_arm.deb",
            path = "pool/main/liba/libandroid-shmem/libandroid-shmem_0.7_arm.deb",
            sha256 = "5832fd11dca9be2a288dd8fbc2b2799b289c812c7a8764f1f8234c425aa64ce5",
        ),
        TermuxPackageSource(
            fileName = "libtalloc_2.4.3_arm.deb",
            path = "pool/main/libt/libtalloc/libtalloc_2.4.3_arm.deb",
            sha256 = "cd56f87007e487c8025fac2df2a27b2bc58102344040a527eaa6fa7527d18f9b",
        ),
    ),
    "x86_64" to listOf(
        TermuxPackageSource(
            fileName = "proot_5.1.107.78-1_x86_64.deb",
            path = "pool/main/p/proot/proot_5.1.107.78-1_x86_64.deb",
            sha256 = "5c1ac8b10a1e188dbce3f9406075f1970dc5ac152cd4acb389f57406ec90390c",
        ),
        TermuxPackageSource(
            fileName = "libandroid-shmem_0.7_x86_64.deb",
            path = "pool/main/liba/libandroid-shmem/libandroid-shmem_0.7_x86_64.deb",
            sha256 = "ffa9e4c87467b158b148d0ff92dda796aa038276c2075af3269cdcdb06f25797",
        ),
        TermuxPackageSource(
            fileName = "libtalloc_2.4.3_x86_64.deb",
            path = "pool/main/libt/libtalloc/libtalloc_2.4.3_x86_64.deb",
            sha256 = "7ca2eaae2e53b28228a01301bc410b62845403d6317c25b8e0a7f40681de0628",
        ),
    ),
)

class LinuxEnvironmentManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val executor = Executors.newCachedThreadPool()

    private val baseDir: File
        get() = File(context.filesDir, "linux")

    private val downloadDir: File
        get() = File(baseDir, "downloads")

    val rootfsDir: File
        get() = File(baseDir, "archlinux")

    val binDir: File
        get() = File(baseDir, "bin")

    val prootBinary: File
        get() = File(termuxUsrDir, "bin/proot")

    val termuxUsrDir: File
        get() = File(baseDir, "usr")

    val installScript: File
        get() = File(baseDir, INSTALL_SCRIPT_NAME)

    fun getStatus(): LinuxEnvironmentStatus {
        val primaryAbi = primaryAbi()
        val rootfsSource = rootfsSourceForAbi(primaryAbi)
        val termuxPackages = termuxPackagesForAbi(primaryAbi)
        val rootfsInstalled = isRootfsInstalled()
        val prootExecutable = prootBinary.canExecute()
        val shellRunner = resolveRunner()
        return LinuxEnvironmentStatus(
            installed = rootfsInstalled,
            rootfsPath = rootfsDir.absolutePath,
            termuxUsrPath = termuxUsrDir.absolutePath,
            prootPath = prootBinary.absolutePath,
            prootExecutable = prootExecutable,
            installScriptPath = installScript.absolutePath,
            rootfsArchivePath = rootfsSource?.let { File(downloadDir, it.fileName).absolutePath },
            primaryAbi = primaryAbi,
            supportedRootfsUrl = rootfsSource?.url,
            termuxPackageUrls = termuxPackages.map { it.url },
            runner = shellRunner,
            canExecuteLinux = shellRunner != null,
            message = when {
                shellRunner != null -> "Arch Linux runner is ready"
                rootfsSource == null -> "Unsupported Android ABI for Arch Linux bootstrap: $primaryAbi"
                !rootfsInstalled -> "Arch Linux rootfs is not installed"
                !prootBinary.exists() -> "proot binary is missing"
                !prootExecutable -> "proot binary is not executable"
                else -> "Arch Linux runner is not ready"
            },
        )
    }

    suspend fun prepareInstallerScript(): LinuxEnvironmentStatus = withContext(Dispatchers.IO) {
        baseDir.mkdirs()
        binDir.mkdirs()
        termuxUsrDir.mkdirs()
        downloadDir.mkdirs()
        installScript.writeText(buildInstallScript(), Charsets.UTF_8)
        installScript.setExecutable(true, false)
        getStatus().copy(message = "Arch Linux installer script prepared")
    }

    suspend fun install(timeoutSeconds: Int = MAX_TIMEOUT_SECONDS): LinuxCommandResult = withContext(Dispatchers.IO) {
        val timeout = timeoutSeconds.coerceIn(1, MAX_TIMEOUT_SECONDS)
        val installLog = StringBuilder()
        prepareInstallerScript()

        val initialStatus = getStatus()
        if (initialStatus.supportedRootfsUrl == null) {
            return@withContext initialStatus.toInstallResult(
                command = "linux.install",
                stdout = "",
                stderr = initialStatus.message,
                exitCode = 2,
            )
        }

        try {
            installTermuxBootstrapPackages(installLog)
            val source = requireNotNull(rootfsSourceForAbi(primaryAbi()))
            val archive = downloadRootfsIfNeeded(source, installLog)
            if (!isRootfsInstalled()) {
                extractRootfs(source, archive, installLog)
            } else {
                installLog.appendLine("Rootfs already installed at ${rootfsDir.absolutePath}")
            }
        } catch (error: Throwable) {
            return@withContext getStatus().toInstallResult(
                command = "linux.install",
                stdout = installLog.toString(),
                stderr = error.message ?: error.javaClass.simpleName,
                exitCode = 1,
            )
        }

        val status = getStatus()
        if (!status.canExecuteLinux) {
            return@withContext status.toInstallResult(
                command = "linux.install",
                stdout = installLog.toString(),
                stderr = status.message,
                exitCode = 3,
            )
        }

        val verify = execute(
            command = "printf 'Arch Linux ready: '; head -n 1 /etc/os-release",
            timeoutSeconds = timeout,
        )
        verify.copy(
            stdout = installLog.append(verify.stdout).toString(),
            executor = INSTALL_EXECUTOR_NAME,
            command = "linux.install",
        )
    }

    suspend fun execute(
        command: String,
        workingDir: String? = null,
        stdin: String? = null,
        timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS,
    ): LinuxCommandResult = withContext(Dispatchers.IO) {
        require(command.isNotBlank()) { "command is required" }
        val runner = resolveRunner()
            ?: return@withContext LinuxCommandResult(
                stdout = "",
                stderr = getStatus().message,
                exitCode = -1,
                executor = EXECUTOR_NAME,
                shell = "bash",
                workingDir = workingDir?.takeIf { it.isNotBlank() } ?: "/root",
                command = command,
                rootfsPath = rootfsDir.absolutePath,
                prootPath = prootBinary.absolutePath,
                fallback = true,
            )

        val timeout = timeoutSeconds.coerceIn(1, MAX_TIMEOUT_SECONDS).toLong()
        val linuxWorkingDir = workingDir?.takeIf { it.isNotBlank() } ?: "/root"
        val process = ProcessBuilder(
            runner,
            "-0",
            "-r",
            rootfsDir.absolutePath,
            "-b",
            "/dev",
            "-b",
            "/proc",
            "-b",
            "/sys",
            "-b",
            "${LocalShellRunner.defaultWorkingDir(context).absolutePath}:/mnt/eteruee",
            "-w",
            linuxWorkingDir,
            "/usr/bin/env",
            "-i",
            "HOME=/root",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/bin:/usr/sbin:/sbin:/bin",
            "TERM=xterm-256color",
            "LANG=C.UTF-8",
            "/bin/bash",
            "-lc",
            command,
        )
            .directory(baseDir)
            .apply {
                environment().putAll(linuxRunnerEnvironment())
            }
            .start()

        if (stdin != null) {
            process.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(stdin)
            }
        } else {
            process.outputStream.close()
        }

        val stdoutFuture = executor.submit(Callable {
            process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        })
        val stderrFuture = executor.submit(Callable {
            process.errorStream.bufferedReader(Charsets.UTF_8).readText()
        })

        val completed = process.waitFor(timeout, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
        }

        val stdout = runCatching { stdoutFuture.get(1, TimeUnit.SECONDS) }.getOrDefault("")
        val stderr = runCatching { stderrFuture.get(1, TimeUnit.SECONDS) }.getOrDefault("")
        LinuxCommandResult(
            stdout = stdout,
            stderr = if (completed) stderr else stderr + "\n[TIMEOUT] Command timed out after ${timeout}s",
            exitCode = if (completed) process.exitValue() else -1,
            executor = EXECUTOR_NAME,
            shell = "/bin/bash",
            workingDir = linuxWorkingDir,
            command = command,
            rootfsPath = rootfsDir.absolutePath,
            prootPath = runner,
            fallback = false,
        )
    }

    private fun installTermuxBootstrapPackages(log: StringBuilder) {
        val packages = termuxPackagesForAbi(primaryAbi())
        if (packages.isEmpty()) return
        if (prootBinary.isFile && prootBinary.canExecute() && termuxUsrDir.resolve("lib/libtalloc.so").isFile) {
            log.appendLine("Termux proot runtime already installed at ${termuxUsrDir.absolutePath}")
            return
        }

        termuxUsrDir.mkdirs()
        packages.forEach { source ->
            val archive = downloadPackageIfNeeded(source, log)
            extractTermuxPackage(archive, log)
        }
        prootBinary.setExecutable(true, true)
        termuxUsrDir.resolve("libexec/proot/loader").setExecutable(true, true)
        termuxUsrDir.resolve("libexec/proot/loader32").setExecutable(true, true)
        log.appendLine("Termux proot runtime installed at ${termuxUsrDir.absolutePath}")
    }

    private fun downloadRootfsIfNeeded(source: RootfsSource, log: StringBuilder): File {
        downloadDir.mkdirs()
        val archive = File(downloadDir, source.fileName)
        if (archive.isFile && archive.length() > 0) {
            log.appendLine("Rootfs archive already downloaded at ${archive.absolutePath}")
            return archive
        }

        downloadFile(
            url = source.url,
            target = archive,
            log = log,
            label = "Arch Linux rootfs",
        )
        return archive
    }

    private fun downloadPackageIfNeeded(source: TermuxPackageSource, log: StringBuilder): File {
        val archive = File(downloadDir, source.fileName)
        if (archive.isFile && archive.length() > 0) {
            verifySha256(archive, source.sha256)
            log.appendLine("Termux package already downloaded at ${archive.absolutePath}")
            return archive
        }

        downloadFile(
            url = source.url,
            target = archive,
            log = log,
            label = "Termux package",
        )
        verifySha256(archive, source.sha256)
        return archive
    }

    private fun downloadFile(
        url: String,
        target: File,
        log: StringBuilder,
        label: String,
    ) {
        downloadDir.mkdirs()
        val tmp = File(downloadDir, "${target.name}.part")
        if (tmp.exists()) tmp.delete()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        log.appendLine("Downloading $url")
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Failed to download $label: HTTP ${response.code}")
            }
            val body = response.body
            tmp.outputStream().use { output ->
                body.byteStream().copyTo(output)
            }
        }
        if (tmp.length() <= 0) {
            tmp.delete()
            error("Downloaded $label is empty")
        }
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
        log.appendLine("Downloaded $label to ${target.absolutePath}")
    }

    private fun extractRootfs(source: RootfsSource, archive: File, log: StringBuilder) {
        val canonicalRoot = rootfsDir.canonicalFile
        if (canonicalRoot.exists()) {
            canonicalRoot.deleteRecursively()
        }
        canonicalRoot.mkdirs()
        log.appendLine("Extracting rootfs to ${canonicalRoot.absolutePath}")

        rootfsArchiveStream(source, archive).use { input ->
            TarArchiveInputStream(input).use { tar ->
                while (true) {
                    val entry = tar.nextEntry ?: break
                    if (!tar.canReadEntryData(entry)) continue

                    val relativeName = sanitizeTarEntryName(entry.name, source.stripFirstPathSegment)
                        ?: continue
                    val target = File(canonicalRoot, relativeName).canonicalFile
                    if (!target.path.startsWith(canonicalRoot.path + File.separator)) {
                        error("Refusing to extract path outside rootfs: ${entry.name}")
                    }

                    when {
                        entry.isDirectory -> target.mkdirs()
                        entry.isSymbolicLink -> {
                            target.parentFile?.mkdirs()
                            runCatching {
                                java.nio.file.Files.createSymbolicLink(target.toPath(), java.nio.file.Path.of(entry.linkName))
                            }.onFailure {
                                File(target.parentFile, "${target.name}.symlink").writeText(entry.linkName, Charsets.UTF_8)
                            }
                        }
                        entry.isLink -> Unit
                        else -> {
                            if (entry.size > MAX_ROOTFS_ENTRY_BYTES) {
                                error("Refusing oversized rootfs entry: ${entry.name}")
                            }
                            target.parentFile?.mkdirs()
                            target.outputStream().use { output ->
                                tar.copyTo(output)
                            }
                            if ((entry.mode and 0b001_001_001) != 0) {
                                target.setExecutable(true, false)
                            }
                        }
                    }
                    if (!entry.isSymbolicLink && !entry.isLink) {
                        target.setReadable(true, false)
                        target.setWritable(true, true)
                    }
                }
            }
        }

        if (!isRootfsInstalled()) {
            error("Arch Linux rootfs extraction finished but etc/os-release was not found")
        }
        log.appendLine("Rootfs extraction complete")
    }

    private fun extractTermuxPackage(archive: File, log: StringBuilder) {
        archive.inputStream().buffered().use { fileInput ->
            ArArchiveInputStream(fileInput).use { ar ->
                while (true) {
                    val entry = ar.nextEntry ?: break
                    if (entry.name != "data.tar.xz") continue
                    XZCompressorInputStream(ar).use { xz ->
                        extractTermuxDataTar(xz)
                    }
                    log.appendLine("Extracted Termux package ${archive.name}")
                    return
                }
            }
        }
        error("Termux package does not contain data.tar.xz: ${archive.name}")
    }

    private fun extractTermuxDataTar(input: InputStream) {
        val canonicalRoot = termuxUsrDir.canonicalFile
        canonicalRoot.mkdirs()
        TarArchiveInputStream(input).use { tar ->
            while (true) {
                val entry = tar.nextEntry ?: break
                if (!tar.canReadEntryData(entry)) continue

                val relativeName = sanitizeTermuxPackageEntryName(entry.name) ?: continue
                val target = File(canonicalRoot, relativeName).canonicalFile
                if (!target.path.startsWith(canonicalRoot.path + File.separator)) {
                    error("Refusing to extract path outside Termux runtime: ${entry.name}")
                }

                when {
                    entry.isDirectory -> target.mkdirs()
                    entry.isSymbolicLink -> {
                        target.parentFile?.mkdirs()
                        runCatching {
                            java.nio.file.Files.createSymbolicLink(target.toPath(), java.nio.file.Path.of(entry.linkName))
                        }.onFailure {
                            File(target.parentFile, "${target.name}.symlink").writeText(entry.linkName, Charsets.UTF_8)
                        }
                    }
                    entry.isLink -> Unit
                    else -> {
                        if (entry.size > MAX_TERMUX_PACKAGE_ENTRY_BYTES) {
                            error("Refusing oversized Termux package entry: ${entry.name}")
                        }
                        target.parentFile?.mkdirs()
                        target.outputStream().use { output ->
                            tar.copyTo(output)
                        }
                        if ((entry.mode and 0b001_001_001) != 0) {
                            target.setExecutable(true, true)
                        }
                    }
                }
                if (!entry.isSymbolicLink && !entry.isLink) {
                    target.setReadable(true, true)
                    target.setWritable(true, true)
                }
            }
        }
    }

    private fun rootfsArchiveStream(source: RootfsSource, archive: File): InputStream {
        val input = BufferedInputStream(FileInputStream(archive))
        return when (source.compression) {
            RootfsCompression.Gzip -> GZIPInputStream(input)
            RootfsCompression.Zstd -> ZstdInputStream(input)
        }
    }

    private fun sanitizeTarEntryName(name: String, stripFirstPathSegment: Boolean): String? {
        val normalized = name.replace('\\', '/').trimStart('/')
        val parts = normalized.split('/')
            .filter { it.isNotEmpty() && it != "." }
            .drop(if (stripFirstPathSegment) 1 else 0)
        if (parts.isEmpty() || parts.any { it == ".." }) return null
        return parts.joinToString(File.separator)
    }

    private fun sanitizeTermuxPackageEntryName(name: String): String? {
        val normalized = name.replace('\\', '/').trimStart('.', '/')
        if (!normalized.startsWith(TERMUX_PREFIX_PATH)) return null
        val relative = normalized.removePrefix(TERMUX_PREFIX_PATH)
        val parts = relative.split('/')
            .filter { it.isNotEmpty() && it != "." }
        if (parts.isEmpty() || parts.any { it == ".." }) return null
        return parts.joinToString(File.separator)
    }

    private fun resolveRunner(): String? =
        prootBinary
            .takeIf { isRootfsInstalled() && it.isFile && it.canExecute() }
            ?.absolutePath

    private fun linuxRunnerEnvironment(): Map<String, String> {
        val usr = termuxUsrDir.absolutePath
        return LocalShellRunner.defaultEnvironment(context) + mapOf(
            "PATH" to "$usr/bin:/system/bin:/system/xbin",
            "LD_LIBRARY_PATH" to "$usr/lib",
            "PROOT_LOADER" to "$usr/libexec/proot/loader",
            "PROOT_TMP_DIR" to context.cacheDir.absolutePath,
        )
    }

    private fun isRootfsInstalled(): Boolean =
        rootfsDir.resolve("etc/os-release").isFile

    private fun primaryAbi(): String =
        Build.SUPPORTED_ABIS.firstOrNull().orEmpty()

    private fun rootfsSourceForAbi(abi: String): RootfsSource? =
        ARCH_ROOTFS_SOURCES[abi.lowercase(Locale.ROOT)]

    private fun termuxPackagesForAbi(abi: String): List<TermuxPackageSource> =
        TERMUX_BOOTSTRAP_PACKAGES[abi.lowercase(Locale.ROOT)].orEmpty()

    private fun verifySha256(file: File, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        check(actual.equals(expected, ignoreCase = true)) {
            "SHA-256 mismatch for ${file.name}: expected $expected, got $actual"
        }
    }

    private fun buildInstallScript(): String {
        val primaryAbi = primaryAbi()
        val rootfsSource = rootfsSourceForAbi(primaryAbi)
        return """
            #!/system/bin/sh
            set -eu

            ROOTFS_DIR="${rootfsDir.absolutePath}"
            PROOT="${prootBinary.absolutePath}"

            cat <<'EOF'
            EterUee installs Arch Linux from app code, not this shell script.
            Rootfs URL: ${rootfsSource?.url ?: "unsupported ABI: $primaryAbi"}
            Rootfs path: ${rootfsDir.absolutePath}
            proot path: ${prootBinary.absolutePath}
            Termux runtime path: ${termuxUsrDir.absolutePath}

            Call linux_environment/install from EterUee to install proot and rootfs.
            EOF

            if [ ! -f "${'$'}ROOTFS_DIR/etc/os-release" ]; then
              echo "Rootfs is not installed yet; use EterUee's Linux installer API." >&2
              exit 4
            fi
            if [ ! -x "${'$'}PROOT" ]; then
              echo "Missing executable proot binary at ${'$'}PROOT" >&2
              exit 3
            fi
            export LD_LIBRARY_PATH="${termuxUsrDir.absolutePath}/lib"
            export PROOT_LOADER="${termuxUsrDir.absolutePath}/libexec/proot/loader"
            "${'$'}PROOT" -0 -r "${'$'}ROOTFS_DIR" -b /dev -b /proc -b /sys -w /root /bin/sh -lc 'printf "Arch Linux ready: "; head -n 1 /etc/os-release'
        """.trimIndent()
    }
}

private fun LinuxEnvironmentStatus.toInstallResult(
    command: String,
    stdout: String,
    stderr: String,
    exitCode: Int,
): LinuxCommandResult = LinuxCommandResult(
    stdout = stdout,
    stderr = stderr,
    exitCode = exitCode,
    executor = INSTALL_EXECUTOR_NAME,
    shell = "kotlin",
    workingDir = rootfsPath,
    command = command,
    rootfsPath = rootfsPath,
    prootPath = prootPath,
    fallback = true,
)

private data class RootfsSource(
    val url: String,
    val fileName: String,
    val compression: RootfsCompression,
    val stripFirstPathSegment: Boolean,
)

private data class TermuxPackageSource(
    val fileName: String,
    val path: String,
    val sha256: String,
) {
    val url: String
        get() = TERMUX_REPO_BASE_URL + path
}

private enum class RootfsCompression {
    Gzip,
    Zstd,
}

@Serializable
data class LinuxEnvironmentStatus(
    val installed: Boolean,
    val rootfsPath: String,
    val termuxUsrPath: String,
    val prootPath: String,
    val prootExecutable: Boolean,
    val installScriptPath: String,
    val rootfsArchivePath: String? = null,
    val primaryAbi: String,
    val supportedRootfsUrl: String? = null,
    val termuxPackageUrls: List<String> = emptyList(),
    val runner: String? = null,
    val canExecuteLinux: Boolean,
    val message: String,
)

@Serializable
data class LinuxCommandResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val executor: String,
    val shell: String,
    val workingDir: String,
    val command: String,
    val rootfsPath: String,
    val prootPath: String,
    val fallback: Boolean,
)
