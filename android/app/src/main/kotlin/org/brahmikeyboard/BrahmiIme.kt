package org.brahmikeyboard

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/** Brahmi keyboard: EN, BR (roman -> Brahmi) and PB (Pure Brahmi) modes. */
class BrahmiIme : InputMethodService() {
    private lateinit var prefs: Prefs
    private lateinit var engine: BrahmiEngine
    private lateinit var root: LinearLayout
    private lateinit var tag: TextView
    private lateinit var prev: TextView
    private lateinit var pop: TextView
    private lateinit var warn: TextView
    private lateinit var keys: LinearLayout
    private var brahmiFont: Typeface? = null
    private val ui = Handler(Looper.getMainLooper())

    private var layer = "abc"            // abc | num | more | lang
    private var shift = 0                // 0 off, 1 once, 2 caps lock
    private var lastShift = 0L
    private var password = false

    // ---------- lifecycle ----------
    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        engine = BrahmiEngine(Data.tables(this))
        syncFromPrefs()
    }

    override fun onCreateInputView(): View {
        syncFromPrefs()
        brahmiFont = try { Typeface.createFromAsset(assets, "fonts/NotoSansBrahmi-Regular.ttf") } catch (e: Exception) { null }

        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(4), dp(8), dp(4)) }
        tag = TextView(this).apply { textSize = 11f; setPadding(dp(6), dp(1), dp(6), dp(1)) }
        prev = TextView(this).apply { textSize = 20f; maxLines = 1; setPadding(dp(8), 0, 0, 0); typeface = brahmiFont }
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(prev) }
        strip.addView(tag); strip.addView(scroll, LinearLayout.LayoutParams(0, dp(40), 1f))
        pop = banner("#B3261E", Color.WHITE); warn = banner("#F5A623", Color.parseColor("#1D1B16"))
        keys = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(3), dp(4), dp(3), dp(4)) }
        root.addView(strip); root.addView(pop); root.addView(warn); root.addView(keys)
        rebuild()
        return root
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        syncFromPrefs()
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        val v = info.inputType and InputType.TYPE_MASK_VARIATION
        password = (cls == InputType.TYPE_CLASS_TEXT && (v == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                v == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || v == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
                (cls == InputType.TYPE_CLASS_NUMBER && v == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        engine.clear(); layer = "abc"; shift = 0
        if (::root.isInitialized) { rebuild() }
    }

    override fun onFinishInput() { commit(null); super.onFinishInput() }

    private fun syncFromPrefs() {
        engine.mode = prefs.mode; engine.lang = prefs.lang; engine.script = prefs.script; engine.smart = prefs.smart
        if (!Data.tables(this).scripts.containsKey(engine.lang)) engine.lang = "Hindi"
    }

    // ---------- text output ----------
    private fun commit(extra: String?) {
        val ic = currentInputConnection
        if (engine.buf.isNotEmpty()) { ic?.commitText(engine.commitText(), 1); engine.clear() }
        if (extra != null) ic?.commitText(extra, 1)
        drawPreview()
    }

    private fun backspace() {
        if (engine.buf.isNotEmpty()) engine.backspace() else currentInputConnection?.deleteSurroundingTextInCodePoints(1, 0)
        drawPreview()
    }

    private fun deleteWord() {
        if (engine.buf.isNotEmpty()) engine.clear() else {
            val before = currentInputConnection?.getTextBeforeCursor(200, 0)?.toString() ?: ""
            val m = Regex("\\S+\\s*$").find(before)
            if (m != null) currentInputConnection?.deleteSurroundingTextInCodePoints(m.value.codePointCount(0, m.value.length), 0)
        }
        drawPreview()
    }

    private fun typeKey(raw: String) {
        var ch = raw
        if (engine.mode != "pb" && shift != 0) { ch = ch.uppercase(); if (shift == 1) { shift = 0; rebuild() } }
        if (engine.mode == "en") { currentInputConnection?.commitText(ch, 1); return }
        val r = engine.add(ch)
        r.pop?.let { showBanner(pop, it, 3200) }
        r.warn?.let { showBanner(warn, it, 4200) }
        drawPreview()
    }

    private fun handle(k: String) {
        when {
            k.startsWith("c:") -> typeKey(k.substring(2))
            k.startsWith("L:") -> { engine.lang = k.substring(2); engine.script = ""; prefs.lang = engine.lang; prefs.script = ""; layer = "abc"; rebuild(); drawPreview() }
            else -> when (k) {
                "sh" -> if (engine.mode != "pb") {
                    val n = System.currentTimeMillis()
                    shift = if (shift == 2) 0 else if (shift == 1 && n - lastShift < 350) 2 else if (shift != 0) 0 else 1
                    lastShift = n; rebuild()
                }
                "num" -> { layer = if (layer == "num") "abc" else "num"; rebuild() }
                "abc" -> { layer = "abc"; rebuild() }
                "more" -> { layer = "more"; rebuild() }
                "lang" -> { layer = "lang"; rebuild() }
                "mode" -> {
                    commit(null)
                    engine.mode = when (engine.mode) { "en" -> "br"; "br" -> "pb"; else -> "en" }
                    prefs.mode = engine.mode; layer = "abc"; shift = 0; rebuild()
                }
                "set" -> startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "sp" -> commit(" ")
                "en" -> { commit(null); sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER) }
                "bk" -> backspace()
                "del" -> deleteWord()
            }
        }
    }

    // ---------- preview and messages ----------
    private fun drawPreview() {
        if (!::tag.isInitialized) return
        tag.text = engine.mode.uppercase()
        prev.text = if (password) "*".repeat(engine.buf.codePointCount(0, engine.buf.length)) else engine.preview()
    }

    private fun showBanner(v: TextView, msg: String, ms: Long) {
        v.text = msg; v.visibility = View.VISIBLE
        ui.removeCallbacksAndMessages(v)
        ui.postAtTime({ v.visibility = View.GONE }, v, android.os.SystemClock.uptimeMillis() + ms)
    }

    private fun banner(bg: String, fg: Int) = TextView(this).apply {
        setBackgroundColor(Color.parseColor(bg)); setTextColor(fg); textSize = 13f
        setTypeface(typeface, Typeface.BOLD); setPadding(dp(10), dp(7), dp(10), dp(7)); visibility = View.GONE
    }

    // ---------- layouts ----------
    private class K(val id: String, val label: CharSequence, val kind: String = "k")

    private val pbd = (0x11052..0x1105B).toList()
    private val pr1: List<Any> = listOf<Any>("@") + pbd + listOf<Any>("#", "%", "del")
    private val pr2: List<Any> = listOf(0x11023, 0x11014, 0x11038, 0x1103A, 0x1103B, 0x1103C, 0x1103D, 0x11042, 0x11043, 0x11044, 0x11045, 0x11001, 0x11016, 0x11025, "bk")
    private val pr3: List<Any> = listOf(0x1101D, 0x11020, 0x1100A, 0x11008, 0x1102D, 0x11022, 0x1102C, 0x11009, 0x11007, 0x11011, 0x11027, 0x11010, 0x11017, "en")
    private val pr4: List<Any> = listOf(0x11006, 0x1100F, 0x1101E, 0x11005, 0x11032, 0x11024, 0x11028, 0x11015, 0x11033, 0x1101A, 0x11013, 0x1102E, 0x11034, 0x1101C)
    private val pr5: List<Any> = listOf("num", 0x1101F, 0x1101B, 0x11019, 0x11018, 0x1102F, 0x11029, 0x11026, 0x1102B, 0x1102A, 0x11021, ".", ",", "?")

    private fun comb(c: Int) = c in 0x11000..0x11004 || c in 0x11038..0x11046 || c == 0x11070 || c == 0x11073 || c == 0x11074
    private fun sg(c: Int) = String(Character.toChars(c))

    private fun small(main: String, cap: String): CharSequence {
        if (cap.isEmpty()) return main
        val s = SpannableString(main + "\n" + cap)
        s.setSpan(RelativeSizeSpan(0.55f), main.length, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }

    /** Caption under a roman key in BR mode: what it types in the preferred script. */
    private fun cap2(c: String): String {
        if (engine.mode != "br") return ""
        val ch = if (shift != 0 && !c[0].isDigit()) c.uppercase() else c
        val d = engine.toDev(ch)
        if (d == ch || Regex("[ऋऌॠॡृॄॢॣःँॅॆ]").containsMatchIn(d)) return ""
        val sign = d.isNotEmpty() && d[0].code in 0x900..0x903 || d.isNotEmpty() && d[0].code in 0x93E..0x94D
        return (if (sign) "◌" else "") + engine.toScript(d, engine.lang)
    }

    private fun cap(c: Int): String {
        val t = engine.toScript(engine.fromBr(sg(c)), engine.lang)
        return if (t.isNotEmpty() && t != sg(c)) t else ""
    }

    private fun pbKey(x: Any): K = when (x) {
        "del" -> K("del", "Delete", "fn"); "bk" -> K("bk", "⌫", "fn"); "en" -> K("en", "↵", "fn"); "num" -> K("num", "123", "fn")
        is String -> K("c:$x", x)
        else -> { val c = x as Int; K("c:" + sg(c), small((if (comb(c)) "◌" else "") + sg(c), cap(c)), "br") }
    }

    private fun layout(): List<List<K>> {
        val sh = K("sh", if (shift == 2) "⇪" else "⇧", if (shift != 0) "fnon" else "fn")
        val modeK = K("mode", engine.mode.uppercase() + " ⇄", "fn")
        val langK = K("lang", engine.lang.take(5), "fn")
        val setK = K("set", "⚙", "fn")
        val sp = K("sp", "space", "sp")
        if (layer == "lang") {
            val l = Data.tables(this).languages.chunked(4).map { r -> r.map { K("L:$it", it, if (it == engine.lang) "fnon" else "fn") } }
            return l + listOf(listOf(K("abc", "← back", "fn")))
        }
        if (layer == "more") return listOf((listOf(0x11046, 0x1107F)).map { pbKey(it) }, listOf(K("abc", "← back", "fn"), sp, K("bk", "⌫", "fn"), K("en", "↵", "fn")))
        if (layer == "num") {
            fun r(s: String) = s.map { K("c:$it", it.toString()) }
            return listOf(r("1234567890"), r("@#₹%&*-+()"), r("!\"':;/?_=~"),
                listOf(K("abc", "ABC", "fn")) + r(",.€£^<>") + K("bk", "⌫", "fn"),
                listOf(K("abc", "← back", "fn")) + (if (engine.mode == "pb") listOf(K("more", "Ext", "fn")) else emptyList()) + sp + K("en", "↵", "fn"))
        }
        if (engine.mode == "pb") {
            val bottom = listOf(sh, modeK, setK, pbKey(0x11030), sp, pbKey(0x11031), sh, langK)
            return listOf(pr1, pr2, pr3, pr4, pr5).map { r -> r.map { pbKey(it) } } + listOf(bottom)
        }
        fun cs(c: Char) = if (shift != 0 && !c.isDigit()) c.uppercaseChar() else c
        fun letter(c: Char) = K("c:$c", small(cs(c).toString(), cap2(c.toString())))
        val rows = listOf("1234567890", "qwertyuiop", "asdfghjkl")
        val out = ArrayList<List<K>>()
        rows.forEachIndexed { i, r ->
            val row = ArrayList<K>()
            if (i == 0) row.add(K("c:@", "@"))
            r.forEach { row.add(letter(it)) }
            if (i == 0) { row.add(K("c:#", "#")); row.add(K("c:%", "%")); row.add(K("del", "Delete", "fn")) }
            if (i == 2) row.add(K("en", "↵", "fn"))
            out.add(row)
        }
        out.add(listOf(K("num", "123", "fn")) + "zxcvbnm".map { letter(it) } + listOf(K("c:.", "."), K("c:,", ","), K("c:?", "?"), K("bk", "⌫", "fn")))
        out.add(listOf(sh, modeK, setK, sp, sh, langK))
        return out
    }

    // ---------- drawing ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private class Pal(val bg: Int, val key: Int, val fn: Int, val tx: Int, val ac: Int, val acTx: Int, val strip: Int)

    private fun pal(): Pal {
        val dark = when (prefs.theme) { "dark" -> true; "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES }
        return if (dark) Pal(Color.parseColor("#2D2A21"), Color.parseColor("#3A362B"), Color.parseColor("#4A4535"), Color.parseColor("#F0EBDD"), Color.parseColor("#E0A458"), Color.parseColor("#1B1914"), Color.parseColor("#26231C"))
        else Pal(Color.parseColor("#DCD6C6"), Color.WHITE, Color.parseColor("#C9C2AE"), Color.parseColor("#1D1B16"), Color.parseColor("#8A4B14"), Color.parseColor("#F4F1EA"), Color.parseColor("#F4F1EA"))
    }

    private fun rounded(c: Int, r: Int = 7) = GradientDrawable().apply { setColor(c); cornerRadius = dp(r).toFloat() }

    private fun rebuild() {
        if (!::root.isInitialized) return
        val p = pal()
        root.setBackgroundColor(p.bg)
        (tag.parent as View).setBackgroundColor(p.strip)
        tag.background = rounded(p.ac, 4); tag.setTextColor(p.acTx); prev.setTextColor(p.tx)
        keys.removeAllViews()
        val pb = engine.mode == "pb" && layer == "abc"
        for (row in layout()) {
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (k in row) {
                val fn = k.kind == "fn" || k.kind == "fnon"
                val v = TextView(this).apply {
                    text = k.label; gravity = Gravity.CENTER; includeFontPadding = false
                    textSize = if (fn) 13f else if (k.kind == "br") 20f else 17f
                    if (k.kind == "br" || (pb && !fn)) typeface = brahmiFont
                    setTextColor(if (k.kind == "fnon") p.acTx else p.tx)
                    background = rounded(if (k.kind == "fnon") p.ac else if (fn) p.fn else p.key)
                    setOnTouchListener { view, e -> touch(view, e, k.id); true }
                }
                val w = when (k.kind) { "sp" -> 4f; "fn", "fnon" -> 1.4f; else -> 1f }
                r.addView(v, LinearLayout.LayoutParams(0, dp(if (pb) 42 else 46), w).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
            }
            keys.addView(r, LinearLayout.LayoutParams(-1, -2))
        }
        drawPreview()
    }

    private val repeat = object : Runnable { var id = ""; override fun run() { handle(id); ui.postDelayed(this, 60) } }

    private fun touch(v: View, e: MotionEvent, id: String) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.alpha = 0.7f; handle(id)
                if (id == "bk") { repeat.id = id; ui.postDelayed(repeat, 400) }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.alpha = 1f; ui.removeCallbacks(repeat) }
        }
    }
}
