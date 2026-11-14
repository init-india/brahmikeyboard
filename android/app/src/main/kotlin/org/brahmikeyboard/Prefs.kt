package org.brahmikeyboard

import android.content.Context

/** Remembered settings: last mode, preferred language, script override, theme, smart vowels. */
class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("abk", Context.MODE_PRIVATE)
    var mode: String get() = p.getString("mode", "lc")!!.let { if (it == "en") "lc" else it }; set(v) = p.edit().putString("mode", v).apply()
    var lcIn: String get() = p.getString("lcIn", "English")!!; set(v) = p.edit().putString("lcIn", v).apply()
    var lcOut: String get() = p.getString("lcOut", "English")!!; set(v) = p.edit().putString("lcOut", v).apply()
    /** Key height in dp. */
    var keyH: Int get() = p.getInt("keyH", 46); set(v) = p.edit().putInt("keyH", v).apply()
    /** Extra space under the keys in dp, to clear the phone's back/home buttons. */
    var gap: Int get() = p.getInt("gap", 0); set(v) = p.edit().putInt("gap", v).apply()
    /** Keyboard width as a percent of the screen, and where it sits when narrower: left, center or right. */
    var widthPct: Int get() = p.getInt("widthPct", 100); set(v) = p.edit().putInt("widthPct", v).apply()
    var align: String get() = p.getString("align", "center")!!; set(v) = p.edit().putString("align", v).apply()
    /** Colours as #RRGGBB, empty = follow the theme. */
    var kbColor: String get() = p.getString("kbColor", "")!!; set(v) = p.edit().putString("kbColor", v).apply()
    var keyColor: String get() = p.getString("keyColor", "")!!; set(v) = p.edit().putString("keyColor", v).apply()
    var textColor: String get() = p.getString("textColor", "")!!; set(v) = p.edit().putString("textColor", v).apply()
    var autoInset: Boolean get() = p.getBoolean("autoInset", true); set(v) = p.edit().putBoolean("autoInset", v).apply()
    var lang: String get() = p.getString("lang", "Hindi")!!; set(v) = p.edit().putString("lang", v).apply()
    var script: String get() = p.getString("script", "")!!; set(v) = p.edit().putString("script", v).apply()
    var theme: String get() = p.getString("theme", "system")!!; set(v) = p.edit().putString("theme", v).apply()
    var smart: Boolean get() = p.getBoolean("smart", false); set(v) = p.edit().putBoolean("smart", v).apply()
}

object Data {
    @Volatile private var tables: Tables? = null
    fun tables(ctx: Context): Tables = tables ?: synchronized(this) {
        tables ?: Tables(ctx.assets.open("tables.json").bufferedReader().use { it.readText() }).also { tables = it }
    }
}
