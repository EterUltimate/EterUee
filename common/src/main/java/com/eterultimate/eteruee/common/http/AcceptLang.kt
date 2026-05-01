package com.eterultimate.eteruee.common.http

import java.util.Locale

/**
 * 鏋勫缓 Accept-Language 澶村€肩殑瀹炵敤绫汇€?
 *
 * 涓昏鐗规€э細
 * 1) 鏀寔 Android 鍜?JVM 鐜鐨勭郴缁熻瑷€鑾峰彇銆?
 * 2) 鏀寔灏?"zh-CN" 灞曞紑涓?["zh-CN", "zh"]锛堝彲閰嶇疆锛夈€?
 * 3) 鍘婚噸骞舵寜浼樺厛绾ч檷鏉冪敓鎴?q 鍙傛暟锛圛ETF RFC 7231锛夈€?
 * 4) 缁撴灉褰㈠锛?"zh-CN, zh;q=0.9, en-US;q=0.8, en;q=0.7"
 */
class AcceptLanguageBuilder private constructor(
    private val localesInPreference: List<Locale>,
    private val options: Options
) {

    data class Options(
        /** 鍙備笌鐢熸垚鐨勮瑷€鏍囩鏈€澶氫釜鏁帮紙鍘婚噸鍚庡啀鎴柇锛夈€?/
        val maxLanguages: Int = 6,
        /** 浠?1.0 璧凤紝姣忎釜鍚庣画鏉＄洰鐨?q 閫掑噺姝ラ暱锛? < step <= 1锛夈€?/
        val qStep: Double = 0.1,
        /** q 鐨勪笅闄愶紙鍖呭惈锛夛紱鑻ラ€掑噺鍒版洿浣庡垯澶瑰埌璇ヤ笅闄愩€?/
        val minQ: Double = 0.1,
        /** 鏄惁涓哄湴鍖哄寲璇█娣诲姞鈥滈€氱敤璇█鐮佲€濓紝濡?"zh-CN" 杩藉姞 "zh"銆?/
        val includeGenericLanguage: Boolean = true,
        /** 鏄惁瀵规爣绛惧幓閲嶏紙淇濇寔棣栨鍑虹幇鐨勯『搴忥級銆?/
        val deduplicate: Boolean = true
    )

    companion object {
        /** 鐩存帴浠?JVM锛堟闈?鏈嶅姟鍣級绯荤粺鐜鍒涘缓銆?/
        fun fromJvmSystem(options: Options = Options()): AcceptLanguageBuilder {
            val primary = Locale.getDefault()
            // JVM 閫氬父鍙彁渚涗竴涓富 Locale锛涘鏋滈渶瑕佽嚜瀹氫箟鍒楄〃锛屽彲鑷浼犲叆 withLocales銆?
            return AcceptLanguageBuilder(listOf(primary), options)
        }

        /**
         * 浠?Android 绯荤粺鐜鍒涘缓銆?
         * @param context 寤鸿浼犲叆搴旂敤鎴栧綋鍓嶄笂涓嬫枃锛屼互鑾峰彇鐢ㄦ埛鈥滃簲鐢ㄥ唴璇█鈥?绯荤粺璇█棣栭€夊垪琛?
         */
        fun fromAndroid(context: android.content.Context, options: Options = Options()): AcceptLanguageBuilder {
            val locales = systemLocalesAndroid(context)
            return AcceptLanguageBuilder(locales, options)
        }

        /** 浣跨敤璋冪敤鏂硅嚜瀹氫箟鐨?Locale 鍒楄〃锛堟寜浼樺厛椤哄簭锛夊垱寤恒€?/
        fun withLocales(locales: List<Locale>, options: Options = Options()): AcceptLanguageBuilder {
            return AcceptLanguageBuilder(locales, options)
        }

        // Android 鐨勭郴缁?Locale 鍒楄〃鑾峰彇
        private fun systemLocalesAndroid(context: android.content.Context): List<Locale> {
            val cfg = context.resources.configuration
            return if (android.os.Build.VERSION.SDK_INT >= 24) {
                val list = cfg.locales
                (0 until list.size()).map { idx -> list[idx] }
            } else {
                listOf(cfg.locale)
            }
        }
    }

    /** 鐢熸垚鏈€缁堢殑 Accept-Language 澶村€硷紙涓嶅寘鍚?"Accept-Language:" 鍓嶇紑锛夈€?/
    fun build(): String {
        // 1) 鍏堝皢 Locale 杞垚璇█鏍囩锛屽苟鎸夐渶灞曞紑鈥滈€氱敤璇█鐮佲€?
        val tags = mutableListOf<String>()
        for (loc in localesInPreference) {
            val full = toLanguageTagCompat(loc)
            if (full.isNotBlank()) tags += full

            if (options.includeGenericLanguage) {
                val generic = genericLanguageOf(full)
                if (generic != null) tags += generic
            }
        }

        // 2) 鍘婚噸锛堜繚鎸侀娆″嚭鐜伴『搴忥級
        val distinct = if (options.deduplicate) tags.distinct() else tags

        // 3) 鎴柇
        val limited = distinct.take(options.maxLanguages.coerceAtLeast(1))

        // 4) 璧嬩簣 q 鍊硷細绗竴涓?1.0 涓嶅啓 q锛屽叾浣欐寜姝ラ暱閫掑噺鍒?minQ
        val parts = mutableListOf<String>()
        var q = 1.0
        for ((i, tag) in limited.withIndex()) {
            if (i == 0) {
                parts += tag
            } else {
                q = (1.0 - i * options.qStep).coerceAtLeast(options.minQ)
                parts += "$tag;q=${formatQ(q)}"
            }
        }

        return parts.joinToString(separator = ", ")
    }

    // --- 杈呭姪鏂规硶 ---

    private fun toLanguageTagCompat(locale: Locale): String {
        // JVM 7+ 鎻愪緵 Locale#toLanguageTag锛涗负瀹夊叏璧疯浠嶄繚搴曟墜鎷?
        val tag = locale.toLanguageTag()
        if (tag.isNotBlank()) return tag

        val language = locale.language ?: return ""
        val country = locale.country
        val variant = locale.variant

        return buildString {
            append(language)
            if (!country.isNullOrBlank()) append("-").append(country)
            if (!variant.isNullOrBlank()) append("-").append(variant)
        }
    }

    /** 浠?"zh-CN" 寰楀埌 "zh"锛涗粠 "en" 鍒欒繑鍥?null锛堟棤鏇撮€氱敤灞傜骇锛夈€?/
    private fun genericLanguageOf(tag: String): String? {
        val idx = tag.indexOf('-')
        if (idx <= 0) return null
        val head = tag.substring(0, idx)
        // 蹇界暐璇稿 "zh-Hans-CN" 鐨勬洿澶嶆潅鎯呭喌锛屼粎閫€涓€绾у嵆鍙?
        return if (head.isNotBlank()) head else null
    }

    /** q 鍊兼牸寮忥細鏈€澶氫繚鐣?3 浣嶅皬鏁帮紝鍘绘帀澶氫綑 0 涓庡皬鏁扮偣銆?/
    private fun formatQ(value: Double): String {
        val s = String.format(java.util.Locale.ROOT, "%.3f", value)
        return s.trimEnd('0').trimEnd('.')
    }
}

fun main() {
    val builder = AcceptLanguageBuilder.fromJvmSystem()
    println(builder.build())
}

