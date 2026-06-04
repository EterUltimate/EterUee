package com.eterultimate.eteruee.data.files

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.LinkedHashMap
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.eterultimate.eteruee.data.datastore.SettingsStore

class SkillManager(
    private val context: Context,
    private val settingsStore: SettingsStore,
) {
    companion object {
        private const val TAG = "SkillManager"
    }

    fun getSkillsDir(): File {
        val dir = context.filesDir.resolve(FileFolders.SKILLS)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listSkills(): List<SkillMetadata> {
        val skillsDir = getSkillsDir()
        return findSkillFiles(skillsDir)
            .mapNotNull { skillFile ->
                parseSkillFile(skillFile, skillFile.parentFile ?: return@mapNotNull null)
            }
            .distinctBy { it.name }
    }

    fun readSkillBody(skillName: String): String? {
        val skillFile = resolveSkillDir(skillName)?.resolve("SKILL.md") ?: return null
        if (!skillFile.exists()) return null
        return SkillFrontmatterParser.extractBody(skillFile.readText())
    }

    fun readSkillContent(skillName: String): String? {
        val skillFile = resolveSkillDir(skillName)?.resolve("SKILL.md") ?: return null
        if (!skillFile.exists()) return null
        return skillFile.readText()
    }

    fun saveSkill(name: String, content: String): SkillMetadata? {
        val skillDir = resolveSkillDir(name) ?: return null
        skillDir.mkdirs()
        val skillFile = skillDir.resolve("SKILL.md")
        skillFile.writeText(content)
        return parseSkillFile(skillFile, skillDir)
    }

    suspend fun deleteSkill(name: String): Boolean = withContext(Dispatchers.IO) {
        val skillDir = resolveSkillDir(name) ?: return@withContext false
        val deleted = skillDir.deleteRecursively()
        if (deleted) {
            settingsStore.update { settings ->
                settings.copy(
                    assistants = settings.assistants.map { assistant ->
                        if (assistant.enabledSkills.contains(name)) {
                            assistant.copy(enabledSkills = assistant.enabledSkills - name)
                        } else {
                            assistant
                        }
                    }
                )
            }
        }
        deleted
    }

    fun getSkillDir(skillName: String): File? = resolveSkillDir(skillName)

    fun saveSkillFile(skillName: String, relativePath: String, content: String): Boolean {
        val skillDir = resolveSkillDir(skillName) ?: return false
        val target = SkillPaths.resolveSkillFile(skillDir, relativePath) ?: return false
        target.parentFile?.mkdirs()
        target.writeText(content)
        return true
    }

    fun saveSkillFilesAtomically(skillName: String, files: Map<String, String>): Boolean {
        return saveSkillBinaryFilesAtomically(
            skillName = skillName,
            files = files.mapValues { it.value.toByteArray() }
        )
    }

    private fun saveSkillBinaryFilesAtomically(skillName: String, files: Map<String, ByteArray>): Boolean {
        val skillsDir = getSkillsDir()
        val targetDir = resolveSkillDir(skillName) ?: return false
        val stagingDir = createTempSkillDir(skillsDir, skillName, "staging") ?: return false
        var backupDir: File? = null

        try {
            for ((relativePath, content) in files) {
                val target = SkillPaths.resolveSkillFile(stagingDir, relativePath) ?: return false
                target.parentFile?.mkdirs()
                target.writeBytes(content)
            }

            if (!stagingDir.resolve("SKILL.md").exists()) return false

            if (targetDir.exists()) {
                backupDir = createTempSkillDir(skillsDir, skillName, "backup") ?: return false
                backupDir.delete()
                if (!targetDir.renameTo(backupDir)) return false
            }

            if (!stagingDir.renameTo(targetDir)) {
                if (backupDir != null && !targetDir.exists()) {
                    backupDir.renameTo(targetDir)
                }
                return false
            }

            backupDir?.deleteRecursively()
            return true
        } catch (e: Exception) {
            Log.w(TAG, "saveSkillFilesAtomically: Failed to save $skillName", e)
            if (backupDir != null && !targetDir.exists()) {
                backupDir.renameTo(targetDir)
            }
            return false
        } finally {
            if (stagingDir.exists()) {
                stagingDir.deleteRecursively()
            }
            if (backupDir?.exists() == true && targetDir.exists()) {
                backupDir.deleteRecursively()
            }
        }
    }

    fun saveSkillPackageFiles(packageName: String, files: Map<String, ByteArray>): List<SkillMetadata>? {
        if (files.isEmpty()) return null
        val skillsDir = getSkillsDir()
        val targetRoot = resolvePackageDir(skillsDir, packageName) ?: return null
        val stagingRoot = createTempSkillDir(skillsDir, sanitizePackageName(packageName), "package-staging")
            ?: return null
        var backupRoot: File? = null

        try {
            for ((relativePath, content) in files) {
                val target = SkillPaths.resolveSkillPackageFile(stagingRoot, relativePath) ?: return null
                target.parentFile?.mkdirs()
                target.writeBytes(content)
            }

            val importedSkills = files.keys
                .filter { it.substringAfterLast('/') == "SKILL.md" }
                .mapNotNull { relativePath ->
                    val skillFile = SkillPaths.resolveSkillPackageFile(stagingRoot, relativePath)
                        ?: return@mapNotNull null
                    parseSkillFile(skillFile, skillFile.parentFile ?: return@mapNotNull null)
                }
                .distinctBy { it.name }

            if (importedSkills.isEmpty()) return null

            if (targetRoot.exists()) {
                backupRoot = createTempSkillDir(skillsDir, targetRoot.name, "package-backup") ?: return null
                backupRoot.delete()
                if (!targetRoot.renameTo(backupRoot)) return null
            }

            if (!stagingRoot.renameTo(targetRoot)) {
                if (backupRoot != null && !targetRoot.exists()) {
                    backupRoot.renameTo(targetRoot)
                }
                return null
            }

            backupRoot?.deleteRecursively()
            return files.keys
                .filter { it.substringAfterLast('/') == "SKILL.md" }
                .mapNotNull { relativePath ->
                    val skillFile = SkillPaths.resolveSkillPackageFile(targetRoot, relativePath)
                        ?: return@mapNotNull null
                    parseSkillFile(skillFile, skillFile.parentFile ?: return@mapNotNull null)
                }
                .distinctBy { it.name }
                .takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "saveSkillPackageFiles: Failed to save package $packageName", e)
            if (backupRoot != null && !targetRoot.exists()) {
                backupRoot.renameTo(targetRoot)
            }
            return null
        } finally {
            if (stagingRoot.exists()) {
                stagingRoot.deleteRecursively()
            }
            if (backupRoot?.exists() == true && targetRoot.exists()) {
                backupRoot.deleteRecursively()
            }
        }
    }

    fun importSkillZip(uri: Uri): List<SkillMetadata>? {
        val displayName = FileUtils.getFileNameFromUri(context, uri)
            ?: uri.lastPathSegment
            ?: "skill-package.zip"
        val zipFiles = readSkillZipFiles(uri)
        val files = stripCommonRoot(zipFiles)
        val skillMdEntries = files.keys.filter { it.substringAfterLast('/') == "SKILL.md" }
        if (skillMdEntries.isEmpty()) return null

        val rootSkillFile = skillMdEntries.singleOrNull { it == "SKILL.md" }
        if (skillMdEntries.size == 1 && rootSkillFile != null) {
            val skillMdContent = String(files[rootSkillFile] ?: return null, Charsets.UTF_8)
            val skillName = SkillFrontmatterParser.parse(skillMdContent)["name"] ?: return null
            if (skillName.isBlank()) return null
            if (!saveSkillBinaryFilesAtomically(skillName, files)) return null
            val skillDir = resolveSkillDir(skillName) ?: return null
            return listOfNotNull(parseSkillFile(skillDir.resolve("SKILL.md"), skillDir))
        }

        return saveSkillPackageFiles(displayName, files)
    }

    fun deleteSkillFile(skillName: String, relativePath: String): Boolean {
        val skillDir = resolveSkillDir(skillName) ?: return false
        val target = SkillPaths.resolveSkillFile(skillDir, relativePath) ?: return false
        return target.delete()
    }

    fun resolveSkillFile(skillName: String, relativePath: String): File? {
        val skillDir = resolveSkillDir(skillName) ?: return null
        val skillsRoot = getSkillsDir().canonicalFile
        val packageRoot = skillDir.parentFile ?: return null
        if (packageRoot.canonicalFile == skillsRoot) {
            return SkillPaths.resolveSkillFile(skillDir, relativePath)
        }
        return SkillPaths.resolveSkillPackageFile(packageRoot, skillDir.name + File.separator + relativePath)
    }

    private fun resolveSkillDir(skillName: String): File? {
        val directDir = SkillPaths.resolveSkillDir(getSkillsDir(), skillName)
        if (directDir?.resolve("SKILL.md")?.exists() == true) return directDir

        return listSkills().firstOrNull { it.name == skillName }?.skillDir ?: directDir
    }

    private fun createTempSkillDir(skillsRoot: File, skillName: String, suffix: String): File? {
        repeat(100) { attempt ->
            val candidate = skillsRoot.resolve(".$skillName.$suffix.$attempt.tmp")
            if (!candidate.exists() && candidate.mkdirs()) {
                return candidate
            }
        }
        return null
    }

    private fun resolvePackageDir(skillsRoot: File, packageName: String): File? {
        val baseName = sanitizePackageName(packageName)
        val direct = SkillPaths.resolveSkillDir(skillsRoot, baseName) ?: return null
        if (!direct.resolve("SKILL.md").exists()) return direct

        repeat(100) { attempt ->
            val candidate = SkillPaths.resolveSkillDir(skillsRoot, "$baseName-package-$attempt")
                ?: return@repeat
            if (!candidate.exists() || !candidate.resolve("SKILL.md").exists()) return candidate
        }
        return null
    }

    private fun sanitizePackageName(name: String): String {
        val sanitized = name
            .substringBeforeLast('.', missingDelimiterValue = name)
            .replace(Regex("""[^A-Za-z0-9._-]"""), "-")
            .trim('-', '.', '_')
        return sanitized.takeIf { it.isNotBlank() && it != "." && it != ".." } ?: "skill-package"
    }

    private fun readSkillZipFiles(uri: Uri): Map<String, ByteArray> {
        val files = LinkedHashMap<String, ByteArray>()
        val input = context.contentResolver.openInputStream(uri) ?: return emptyMap()
        ZipInputStream(input).use { zipIn ->
            while (true) {
                val entry = zipIn.nextEntry ?: break
                if (!entry.isDirectory) {
                    val relativePath = normalizeZipEntryPath(entry.name) ?: return emptyMap()
                    files[relativePath] = zipIn.readBytes()
                }
                zipIn.closeEntry()
            }
        }
        return files
    }

    private fun normalizeZipEntryPath(entryName: String): String? {
        val normalized = entryName.replace('\\', '/').trimStart('/')
        if (normalized.isBlank()) return null
        val segments = normalized.split('/').filter { it.isNotBlank() }
        if (segments.any { it == "." || it == ".." }) return null
        return segments.joinToString("/")
    }

    private fun stripCommonRoot(files: Map<String, ByteArray>): Map<String, ByteArray> {
        if (files.isEmpty()) return files
        val roots = files.keys.map { it.substringBefore('/') }.distinct()
        if (roots.size != 1) return files

        val root = roots.single()
        if (files.keys.any { it == root }) return files

        return files.mapKeysTo(LinkedHashMap()) { (path, _) ->
            path.removePrefix("$root/")
        }.filterKeys { it.isNotBlank() }
    }

    private fun parseSkillFile(skillFile: File, skillDir: File): SkillMetadata? {
        return runCatching {
            val content = skillFile.readText()
            val frontmatter = SkillFrontmatterParser.parse(content)
            val name = frontmatter["name"]?.takeIf { it.isNotBlank() } ?: return null
            val description = frontmatter["description"]?.takeIf { it.isNotBlank() } ?: return null
            SkillMetadata(
                name = name,
                description = description,
                compatibility = frontmatter["compatibility"],
                allowedTools = frontmatter["allowed-tools"]?.split(" ")?.filter { it.isNotBlank() } ?: emptyList(),
                skillDir = skillDir,
            )
        }.getOrElse {
            Log.w(TAG, "parseSkillFile: Failed to parse ${skillFile.absolutePath}", it)
            null
        }
    }

    private fun findSkillFiles(root: File): List<File> {
        val result = mutableListOf<File>()
        fun visit(dir: File) {
            if (dir.name.startsWith(".")) return
            val skillFile = dir.resolve("SKILL.md")
            if (skillFile.isFile) {
                result += skillFile
            }
            dir.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedBy { it.name }
                ?.forEach(::visit)
        }
        visit(root)
        return result.sortedBy { it.relativeTo(root).path }
    }
}

data class SkillMetadata(
    val name: String,
    val description: String,
    val compatibility: String? = null,
    val allowedTools: List<String> = emptyList(),
    val skillDir: File,
) {
    val skillFile: File get() = skillDir.resolve("SKILL.md")
}

object SkillFrontmatterParser {
    private val frontmatterEndRegex = Regex("""\r?\n---(?:\r?\n|$)""")

    fun parse(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (!content.startsWith("---")) return result
        val endRange = findFrontmatterEndRange(content) ?: return result
        val yaml = content.substring(3, endRange.first).trim()
        yaml.lines().forEach { line ->
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val key = line.substring(0, colonIdx).trim()
                val value = line.substring(colonIdx + 1).trim().removeSurrounding("\"")
                if (key.isNotBlank() && value.isNotBlank()) {
                    result[key] = value
                }
            }
        }
        return result
    }

    fun extractBody(content: String): String {
        if (!content.startsWith("---")) return content
        val endRange = findFrontmatterEndRange(content) ?: return content
        return content.substring(endRange.last + 1).trimStart('\r', '\n')
    }

    private fun findFrontmatterEndRange(content: String): IntRange? {
        if (!content.startsWith("---")) return null
        return frontmatterEndRegex.find(content, startIndex = 3)?.range
    }
}

