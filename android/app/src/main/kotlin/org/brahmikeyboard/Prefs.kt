package org.brahmikeyboard

import android.content.Context

/** Remembered settings: last mode, preferred language, script override, theme, smart vowels. */
class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("abk", Context.MODE_PRIVATE)
    var mode: String get() = p.getString("mode", "en")!!; set(v) = p.edit().putString("mode", v).apply()
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
