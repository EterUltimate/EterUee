package com.eterultimate.eteruee.utils

/**
 * 绉婚櫎瀛楃涓蹭腑鐨凪arkdown鏍煎紡
 * @return 绉婚櫎Markdown鏍煎紡鍚庣殑绾枃鏈?
 */
fun String.stripMarkdown(): String {
    return this
        // 绉婚櫎浠ｇ爜鍧?(```...``` 鍜?`...`)
        .replace(Regex("```[\\s\\S]*?```|`[^`]*?`"), "")
        // 绉婚櫎鍥剧墖鍜岄摼鎺ワ紝浣嗕繚鐣欏叾鏂囨湰鍐呭
        .replace(Regex("!?\\[([^\\]]+)\\]\\([^\\)]*\\)"), "$1")
        // 绉婚櫎鍔犵矖鍜屾枩浣?(鍏堝鐞嗕袱涓槦鍙风殑)
        .replace(Regex("\\*\\*([^*]+?)\\*\\*"), "$1")
        .replace(Regex("\\*([^*]+?)\\*"), "$1")
        // 绉婚櫎涓嬪垝绾?
        .replace(Regex("__([^_]+?)__"), "$1")
        .replace(Regex("_([^_]+?)_"), "$1")
        // 绉婚櫎鍒犻櫎绾?
        .replace(Regex("~~([^~]+?)~~"), "$1")
        // 绉婚櫎鏍囬鏍囪 (澶氳妯″紡)
        .replace(Regex("(?m)^#+\\s*"), "")
        // 绉婚櫎鍒楄〃鏍囪 (澶氳妯″紡)
        .replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
        .replace(Regex("(?m)^\\s*\\d+\\.\\s+"), "")
        // 绉婚櫎寮曠敤鏍囪 (澶氳妯″紡)
        .replace(Regex("(?m)^>\\s*"), "")
        // 绉婚櫎姘村钩鍒嗗壊绾?
        .replace(Regex("(?m)^(\\s*[-*_]){3,}\\s*$"), "")
        // 灏嗗涓崲琛岀鍘嬬缉锛屼互淇濈暀娈佃惤
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

fun String.extractThinkingTitle(): String? {
    // 鎸夎鍒嗗壊鏂囨湰
    val lines = this.lines()

    // 浠庡悗寰€鍓嶆煡鎵炬渶鍚庝竴涓鍚堟潯浠剁殑鍔犵矖鏂囨湰琛?
    for (i in lines.indices.reversed()) {
        val line = lines[i].trim()

        // 妫€鏌ユ槸鍚︿负鍔犵矖鏍煎紡涓旂嫭鍗犱竴鏁磋
        val boldPattern = Regex("^\\*\\*(.+?)\\*\\*$")
        val match = boldPattern.find(line)

        if (match != null) {
            // 杩斿洖鍔犵矖鏍囪鍐呯殑鏂囨湰鍐呭
            return match.groupValues[1].trim().takeUnless { it.isBlank() }
        }
    }

    return null
}

