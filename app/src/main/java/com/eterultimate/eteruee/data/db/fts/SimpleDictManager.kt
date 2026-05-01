package com.eterultimate.eteruee.data.db.fts

import android.content.Context
import java.io.File

object SimpleDictManager {

    private const val DICT_ASSET_DIR = "simple_dict"
    private const val VERSION_FILE = "version.txt"

    // 涓?assets 涓殑璇嶅吀鐗堟湰瀵归綈锛屾洿鏂拌瘝鍏告椂閫掑姝ゅ€?
    private const val CURRENT_VERSION = 1

    /**
     * 灏?assets/simple_dict 瑙ｅ帇鍒?files/simple_dict锛岃繑鍥炶瘝鍏哥洰褰曘€?
     * 宸叉槸鏈€鏂扮増鏈椂鐩存帴杩斿洖锛屾棤闇€閲嶅鎷疯礉銆?
     * 鍙湪浠绘剰绾跨▼璋冪敤銆?
     */
    fun extractDict(context: Context): File {
        val destDir = File(context.filesDir, DICT_ASSET_DIR)
        val versionFile = File(destDir, VERSION_FILE)

        if (versionFile.exists() && versionFile.readText().trim().toIntOrNull() == CURRENT_VERSION) {
            return destDir
        }

        destDir.deleteRecursively()
        destDir.mkdirs()
        copyAssetDir(context, DICT_ASSET_DIR, destDir)
        versionFile.writeText(CURRENT_VERSION.toString())
        return destDir
    }

    private fun copyAssetDir(context: Context, assetPath: String, destDir: File) {
        val assets = context.assets.list(assetPath) ?: return
        for (name in assets) {
            val childAsset = "$assetPath/$name"
            val destFile = File(destDir, name)
            val children = context.assets.list(childAsset)
            if (!children.isNullOrEmpty()) {
                destFile.mkdirs()
                copyAssetDir(context, childAsset, destFile)
            } else {
                context.assets.open(childAsset).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}

