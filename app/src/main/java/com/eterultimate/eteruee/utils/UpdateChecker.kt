package com.eterultimate.eteruee.utils

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.rerere.common.http.await
import com.eterultimate.eteruee.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request

private const val API_URL = "https://updates.rikka-ai.com/"

class UpdateChecker(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    fun checkUpdate(): Flow<UiState<UpdateInfo>> = flow {
        emit(UiState.Loading)
        emit(
            UiState.Success(
                data = try {
                    val response = client.newCall(
                        Request.Builder()
                            .url(API_URL)
                            .get()
                            .addHeader(
                                "User-Agent",
                                "EterUee ${BuildConfig.VERSION_NAME} #${BuildConfig.VERSION_CODE}"
                            )
                            .build()
                    ).await()
                    if (response.isSuccessful) {
                        json.decodeFromString<UpdateInfo>(response.body.string())
                    } else {
                        throw Exception("Failed to fetch update info")
                    }
                } catch (e: Exception) {
                    throw Exception("Failed to fetch update info", e)
                }
            )
        )
    }.catch {
        emit(UiState.Error(it))
    }.flowOn(Dispatchers.IO)

    fun downloadUpdate(context: Context, download: UpdateDownload) {
        runCatching {
            val request = DownloadManager.Request(download.url.toUri()).apply {
                // 璁剧疆涓嬭浇鏃堕€氱煡鏍忕殑鏍囬鍜屾弿杩?
                setTitle(download.name)
                setDescription("姝ｅ湪涓嬭浇鏇存柊鍖?..")
                // 涓嬭浇瀹屾垚鍚庨€氱煡鏍忓彲瑙?
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                // 鍏佽鍦ㄧЩ鍔ㄧ綉缁滃拰WiFi涓嬩笅杞?
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                // 璁剧疆鏂囦欢淇濆瓨璺緞
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, download.name)
                // 鍏佽涓嬭浇鐨勬枃浠剁被鍨?
                setMimeType("application/vnd.android.package-archive")
            }
            // 鑾峰彇绯荤粺鐨凞ownloadManager
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            // 浣犲彲浠ヤ繚瀛樿繑鍥炵殑downloadId鍒版湰鍦帮紝浠ヤ究鍚庣画鏌ヨ涓嬭浇杩涘害鎴栫姸鎬?
        }.onFailure {
            Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
            context.openUrl(download.url) // 璺宠浆鍒颁笅杞介〉闈?
        }
    }
}

@Serializable
data class UpdateDownload(
    val name: String,
    val url: String,
    val size: String
)

@Serializable
data class UpdateInfo(
    val version: String,
    val publishedAt: String,
    val changelog: String,
    val downloads: List<UpdateDownload>
)

/**
 * 鐗堟湰鍙峰€肩被锛屽皝瑁呯増鏈彿瀛楃涓插苟鎻愪緵姣旇緝鍔熻兘
 *
 * 鏀寔瀹屾暣鐨?SemVer 瑙勮寖锛歁AJOR.MINOR.PATCH[-prerelease][+build]
 * - 棰勫彂甯冪増鏈紭鍏堢骇浣庝簬姝ｅ紡鐗堬細1.0.0-alpha < 1.0.0
 * - 棰勫彂甯冩爣璇嗙鎸夋閫愪釜姣旇緝锛氭暟瀛楁寜鏁板€兼瘮杈冿紝瀛楃涓叉寜瀛楀吀搴忔瘮杈?
 * - 棰勫彂甯冩爣璇嗙浼樺厛绾э細alpha < beta < rc锛堥€氳繃瀛楀吀搴忚嚜鐒舵弧瓒筹級
 * - build metadata锛?鍙峰悗闈㈢殑閮ㄥ垎锛変笉褰卞搷浼樺厛绾ф瘮杈?
 */
@JvmInline
value class Version(val value: String) : Comparable<Version> {

    private fun parse(): ParsedVersion {
        // 鍘绘帀 build metadata锛?鍙峰悗闈㈢殑閮ㄥ垎锛?
        val withoutBuild = value.split("+").first()
        // 鍒嗙涓荤増鏈彿鍜岄鍙戝竷鏍囪瘑绗?
        val hyphenIndex = withoutBuild.indexOf('-')
        val (coreStr, prereleaseStr) = if (hyphenIndex >= 0) {
            withoutBuild.substring(0, hyphenIndex) to withoutBuild.substring(hyphenIndex + 1)
        } else {
            withoutBuild to null
        }
        val core = coreStr.split(".").map { it.toIntOrNull() ?: 0 }
        val prerelease = prereleaseStr?.split(".")
        return ParsedVersion(core, prerelease)
    }

    override fun compareTo(other: Version): Int {
        val a = this.parse()
        val b = other.parse()

        // 鍏堟瘮杈冧富鐗堟湰鍙?
        val maxLen = maxOf(a.core.size, b.core.size)
        for (i in 0 until maxLen) {
            val ap = if (i < a.core.size) a.core[i] else 0
            val bp = if (i < b.core.size) b.core[i] else 0
            if (ap != bp) return ap.compareTo(bp)
        }

        // 涓荤増鏈彿鐩稿悓鏃舵瘮杈冮鍙戝竷鏍囪瘑绗?
        // 鏈夐鍙戝竷鏍囪瘑绗︾殑鐗堟湰浼樺厛绾т綆浜庢病鏈夌殑锛?.0.0-alpha < 1.0.0
        return when {
            a.prerelease == null && b.prerelease == null -> 0
            a.prerelease != null && b.prerelease == null -> -1
            a.prerelease == null && b.prerelease != null -> 1
            else -> comparePrerelease(a.prerelease!!, b.prerelease!!)
        }
    }

    companion object {
        fun compare(version1: String, version2: String): Int {
            return Version(version1).compareTo(Version(version2))
        }

        private fun comparePrerelease(a: List<String>, b: List<String>): Int {
            val maxLen = maxOf(a.size, b.size)
            for (i in 0 until maxLen) {
                // 瀛楁灏戠殑浼樺厛绾ф洿浣庯細1.0.0-alpha < 1.0.0-alpha.1
                if (i >= a.size) return -1
                if (i >= b.size) return 1

                val aNum = a[i].toIntOrNull()
                val bNum = b[i].toIntOrNull()

                val cmp = when {
                    // 閮芥槸瀛楋細鎸夋暟鍊兼瘮杈?
                    aNum != null && bNum != null -> aNum.compareTo(bNum)
                    // 鏁板瓧浼樺厛绾т綆浜庡瓧绗︿覆
                    aNum != null -> -1
                    bNum != null -> 1
                    // 閮芥槸瀛楃涓诧細鎸夊瓧鍏稿簭姣旇緝
                    else -> a[i].compareTo(b[i])
                }
                if (cmp != 0) return cmp
            }
            return 0
        }
    }
}

private data class ParsedVersion(
    val core: List<Int>,
    val prerelease: List<String>?,
)

// 鎵╁睍鎿嶄綔绗﹀嚱鏁帮紝浣挎瘮杈冩洿鐩磋
operator fun String.compareTo(other: Version): Int = Version(this).compareTo(other)
operator fun Version.compareTo(other: String): Int = this.compareTo(Version(other))

