package org.brahmikeyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView

/** Setup, language, script, theme and the user guide. Also the launcher screen. */
class SettingsActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = Prefs(this); val t = Data.tables(this)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }

        fun label(s: String) = TextView(this).apply { text = s; textSize = 15f; setPadding(0, pad / 2, 0, 0) }
        fun spinner(items: List<String>, cur: String, onPick: (String) -> Unit) = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, items)
            setSelection(items.indexOf(cur).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = onPick(items[pos])
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }

        col.addView(TextView(this).apply { text = "Brahmi Keyboard"; textSize = 22f })
        col.addView(TextView(this).apply { text = "1. Enable the keyboard, then 2. choose it as your input method."; setPadding(0, pad / 2, 0, 0) })
        col.addView(Button(this).apply { text = "Enable in system settings"; setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) } })
        col.addView(Button(this).apply { text = "Choose keyboard"; setOnClickListener { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() } })

        col.addView(TextView(this).apply { text = "Local mode"; textSize = 18f; setPadding(0, pad, 0, 0) })
        col.addView(TextView(this).apply { text = "Local mode turns what you type into another script. Type with English letters (phonetic) or with a language's own keys, and choose the script that comes out. English to English leaves the text unchanged."; textSize = 13f })
        col.addView(label("Type with"))
        col.addView(spinner(listOf("English") + BrahmiEngine(t).nativeLanguages(), prefs.lcIn) { prefs.lcIn = it })
        col.addView(label("Output script"))
        col.addView(spinner(listOf("English") + t.languages, prefs.lcOut) { prefs.lcOut = it })

        col.addView(TextView(this).apply { text = "Brahmi modes"; textSize = 18f; setPadding(0, pad, 0, 0) })
        col.addView(label("Preferred language"))
        col.addView(spinner(t.languages, prefs.lang) { if (it != prefs.lang) { prefs.lang = it; prefs.script = "" } })
        col.addView(label("Script (leave on Default to use the language's usual script)"))
        col.addView(spinner(listOf("Default") + t.scriptNames.sorted(), prefs.script.ifEmpty { "Default" }) { prefs.script = if (it == "Default") "" else it })
        col.addView(TextView(this).apply { text = "Size and position"; textSize = 18f; setPadding(0, pad, 0, 0) })
        fun seek(title: String, lo: Int, hi: Int, cur: Int, unit: String, set: (Int) -> Unit) {
            val tv = label("$title: $cur$unit")
            col.addView(tv)
            col.addView(SeekBar(this).apply {
                max = hi - lo; progress = cur - lo
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { tv.text = "$title: ${p + lo}$unit"; if (fromUser) set(p + lo) }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
            })
        }
        seek("Key height", 34, 72, prefs.keyH, " dp") { prefs.keyH = it }
        seek("Space under the keys (lifts the keyboard)", 0, 96, prefs.gap, " dp") { prefs.gap = it }
        seek("Keyboard width", 60, 100, prefs.widthPct, " %") { prefs.widthPct = it }
        col.addView(label("Position when narrower than the screen"))
        col.addView(spinner(listOf("center", "left", "right"), prefs.align) { prefs.align = it })
        col.addView(CheckBox(this).apply {
            text = "Keep clear of the phone's back / home buttons automatically"
            isChecked = prefs.autoInset; setOnCheckedChangeListener { _, c -> prefs.autoInset = c }
        })
        col.addView(TextView(this).apply { text = "Changes show the next time the keyboard opens."; textSize = 12f })
        col.addView(label("Theme"))
        col.addView(spinner(listOf("system", "light", "dark"), prefs.theme) { prefs.theme = it })
        col.addView(CheckBox(this).apply {
            text = "Smart vowels: consonant + vowel joins into one syllable (Pure Brahmi)"
            isChecked = prefs.smart; setOnCheckedChangeListener { _, c -> prefs.smart = c }
        })
        col.addView(TextView(this).apply { text = "User guide and Ashokan notes"; textSize = 18f; setPadding(0, pad, 0, pad / 2) })
        val guide = assets.open("guide.html").bufferedReader().use { it.readText() }
        col.addView(TextView(this).apply { text = Html.fromHtml(guide, Html.FROM_HTML_MODE_COMPACT); textSize = 14f })
        setContentView(ScrollView(this).apply { addView(col) })
    }
}
