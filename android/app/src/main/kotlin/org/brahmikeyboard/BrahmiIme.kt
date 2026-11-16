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
import android.view.WindowInsets
import android.os.Build
import android.widget.FrameLayout
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Brahmi keyboard: EN, BR (roman -> Brahmi) and PB (Pure Brahmi) modes. */
class BrahmiIme : InputMethodService() {
    private lateinit var prefs: Prefs
    private lateinit var engine: BrahmiEngine
    private lateinit var outer: FrameLayout
    private lateinit var root: LinearLayout
    private var navInset = 0
    private lateinit var tag: TextView
    private lateinit var prev: TextView
    private lateinit var pop: TextView
    private lateinit var warn: TextView
    private lateinit var keys: LinearLayout
    private var brahmiFont: Typeface? = null
    private val ui = Handler(Looper.getMainLooper())

    private var layer = "abc"            // abc | num | emoji | lang | lang2
    private var shift = 0                // 0 off, 1 once, 2 caps lock
    private var lastShift = 0L
    private var password = false
    /** Quick English: held Mode key switches to English in, English out, and back again. */
    private var plain = false

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

        outer = FrameLayout(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        outer.addView(root, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        outer.setOnApplyWindowInsetsListener { _, ins ->
            navInset = if (Build.VERSION.SDK_INT >= 30) ins.getInsets(WindowInsets.Type.navigationBars()).bottom else ins.systemWindowInsetBottom
            applyFrame(); ins
        }
        val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(4), dp(8), dp(4)) }
        tag = TextView(this).apply { textSize = 11f; setPadding(dp(6), dp(1), dp(6), dp(1)) }
        // One fixed-width line that always shows the end of the word. (A scroll view around a wrap-content
        // text left the newest letter clipped until the next key was pressed.)
        prev = TextView(this).apply {
            textSize = 20f; setSingleLine(true); ellipsize = android.text.TextUtils.TruncateAt.START
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), 0, 0, 0); typeface = brahmiFont
        }
        strip.addView(tag); strip.addView(prev, LinearLayout.LayoutParams(0, dp(40), 1f))
        pop = banner("#B3261E", Color.WHITE); warn = banner("#F5A623", Color.parseColor("#1D1B16"))
        keys = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(3), dp(4), dp(3), dp(4)) }
        root.addView(strip); root.addView(pop); root.addView(warn); root.addView(keys)
        rebuild()
        outer.requestApplyInsets()
        return outer
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
        if (::outer.isInitialized) outer.requestApplyInsets()
    }

    override fun onFinishInput() { commit(null); super.onFinishInput() }

    private fun syncFromPrefs() {
        engine.mode = prefs.mode; engine.lang = prefs.lang; engine.script = prefs.script; engine.smart = prefs.smart
        engine.lcIn = prefs.lcIn; engine.lcOut = prefs.lcOut
        if (engine.lcIn != "English" && engine.lcIn !in engine.nativeLanguages()) engine.lcIn = "English"
        if (!Data.tables(this).scripts.containsKey(engine.lang)) engine.lang = "Hindi"
        if (plain) { engine.mode = "lc"; engine.lcIn = "English"; engine.lcOut = "English" }
    }

    private fun togglePlain() {
        commit(null)
        plain = !plain
        syncFromPrefs(); layer = "abc"; shift = 0; rebuild()
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

    /**
     * Go: move the word waiting in the preview into the app, and nothing else (no Enter, the keyboard stays open).
     * When nothing is waiting, run the app's own action (Go, Search or Send). Apps decide whether they accept it.
     */
    private fun go() {
        if (engine.buf.isNotEmpty()) { commit(null); return }
        val ic = currentInputConnection ?: return
        val a = (currentInputEditorInfo?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val act = when (a) { EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_SEARCH, EditorInfo.IME_ACTION_SEND -> a; else -> EditorInfo.IME_ACTION_SEND }
        ic.performEditorAction(act)
    }

    private fun nativeIn() = engine.mode == "lc" && engine.lcIn != "English"
    /** Pure Brahmi layout: used in PB, in Brahmi mode (language letters) and in Local mode with a native input language. */
    private fun gridMode() = engine.mode == "pb" || engine.mode == "br" || nativeIn()

    private fun typeKey(raw: String) {
        var ch = raw
        if (gridMode()) { /* no shift in this layout */ }
        else if (shift != 0) { ch = ch.uppercase(); if (shift == 1) { shift = 0; rebuild() } }
        val r = engine.add(ch)
        r.pop?.let { showBanner(pop, it, 3200) }
        r.warn?.let { showBanner(warn, it, 4200) }
        drawPreview()
    }

    private fun handle(k: String) {
        when {
            k.startsWith("c:") -> typeKey(k.substring(2))
            k.startsWith("L:") -> {
                val n = k.substring(2)
                plain = false; syncFromPrefs()
                if (engine.mode == "lc") { engine.lcOut = n; prefs.lcOut = n }
                else { engine.lang = n; engine.script = ""; prefs.lang = n; prefs.script = "" }
                commit(null); layer = "abc"; rebuild()
            }
            k.startsWith("I:") -> { commit(null); plain = false; syncFromPrefs(); engine.lcIn = k.substring(2); prefs.lcIn = engine.lcIn; layer = "abc"; shift = 0; rebuild() }
            else -> when (k) {
                "sh" -> if (!gridMode()) {
                    val n = System.currentTimeMillis()
                    shift = if (shift == 2) 0 else if (shift == 1 && n - lastShift < 350) 2 else if (shift != 0) 0 else 1
                    lastShift = n; rebuild()
                }
                "num" -> { layer = if (layer == "num") "abc" else "num"; rebuild() }
                "abc" -> { layer = "abc"; rebuild() }
                "emoji" -> { layer = "emoji"; rebuild() }
                "lang" -> { layer = "lang"; rebuild() }
                "lang2" -> { layer = "lang2"; rebuild() }
                "mode" -> if (plain) togglePlain() else {
                    commit(null)
                    engine.mode = when (engine.mode) { "lc" -> "br"; "br" -> "pb"; else -> "lc" }
                    prefs.mode = engine.mode; layer = "abc"; shift = 0; rebuild()
                }
                "set" -> startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "sp" -> commit(" ")
                // Enter: with a word waiting, send it and stop. With nothing waiting, press the phone's Enter key.
                "en" -> if (engine.buf.isNotEmpty()) commit(null) else sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                "go" -> go()
                "bk" -> backspace()
                "del" -> deleteWord()
            }
        }
    }

    // ---------- preview and messages ----------
    private fun drawPreview() {
        if (!::tag.isInitialized) return
        tag.text = modeName()
        prev.text = if (password) "*".repeat(engine.buf.codePointCount(0, engine.buf.length)) else engine.preview()
        prev.requestLayout(); prev.invalidate()
    }

    private fun modeName() = if (plain) "ENG" else when (engine.mode) { "lc" -> "CUS"; else -> engine.mode.uppercase() }

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
    // Row 2 keeps the joiner (virama) in the middle so conjuncts can be typed: consonant, joiner, consonant.
    private val pr2: List<Any> = listOf(0x11023, 0x11014, 0x11038, 0x1103A, 0x1103B, 0x1103C, 0x1103D, 0x11046, 0x11042, 0x11043, 0x11044, 0x11045, 0x11001, 0x11016, 0x11025, "bk")
    private val pr3: List<Any> = listOf(0x1101D, 0x11020, 0x1100A, 0x11008, 0x1102D, 0x11022, 0x1102C, 0x11009, 0x11007, 0x11011, 0x11027, 0x11010, 0x11017, "en")
    private val pr4: List<Any> = listOf(0x11006, 0x1100F, 0x1101E, 0x11005, 0x11032, 0x11024, 0x11028, 0x11015, 0x11033, 0x1101A, 0x11013, 0x1102E, 0x11034, 0x1101C)
    private val pr5: List<Any> = listOf("num", 0x1101F, 0x1101B, 0x11019, 0x11018, 0x1102F, 0x11029, 0x11026, 0x1102B, 0x1102A, 0x11021, ".", ",", "?")

    // ा ि ी ु ू ् े ै ो ौ ं : the joiner (halant) sits in the middle, as in Pure Brahmi.
    private val signRow = "ािीुू्ेैोौं".toList()

    private fun comb(c: Int) = c in 0x11000..0x11004 || c in 0x11038..0x11046 || c == 0x11070 || c == 0x11073 || c == 0x11074
    private fun devSign(c: Char) = c.code in 0x900..0x903 || c.code in 0x93A..0x94F || c.code == 0x962 || c.code == 0x963
    private fun sg(c: Int) = String(Character.toChars(c))

    private fun small(main: String, cap: String): CharSequence {
        if (cap.isEmpty()) return main
        val s = SpannableString(main + "\n" + cap)
        s.setSpan(RelativeSizeSpan(0.55f), main.length, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }

    /** Caption under a roman key: what it types in the output script (Brahmi mode: preferred language, Local mode: output language). */
    private fun cap2(c: String): String {
        val target = when (engine.mode) { "br" -> engine.lang; "lc" -> if (engine.lcOut == "English") "" else engine.lcOut; else -> "" }
        if (target.isEmpty()) return ""
        val ch = if (shift != 0 && !c[0].isDigit()) c.uppercase() else c
        val d = engine.toDev(ch)
        if (d == ch || Regex("[ऋऌॠॡृॄॢॣःँॅॆ]").containsMatchIn(d)) return ""
        val sign = d.isNotEmpty() && (d[0].code in 0x900..0x903 || d[0].code in 0x93E..0x94D)
        return (if (sign) "◌" else "") + engine.toScript(d, target)
    }

    private fun cap(c: Int): String {
        val t = engine.toScript(engine.fromBr(sg(c)), engine.lang)
        return if (t.isNotEmpty() && t != sg(c)) t else ""
    }

    /**
     * One key of the Pure Brahmi layout.
     * PB: the Brahmi letter, with the preferred language's letter in small type under it.
     * BR: the preferred language's letter (what you type), with the Brahmi letter in small type under it.
     * Local with a native input language: that language's letter, with the output script's letter under it.
     */
    private fun pbKey(x: Any): K = when (x) {
        "del" -> K("del", "Del", "fn"); "bk" -> K("bk", "⌫", "fn"); "en" -> K("en", "↵", "fn"); "num" -> numKey()
        is String -> K("c:$x", x)
        else -> gridKey(x as Int)
    }

    private fun gridKey(c: Int): K {
        if (engine.mode == "pb") return K("c:" + sg(c), small((if (comb(c)) "◌" else "") + sg(c), cap(c)), "br")
        val from = if (engine.mode == "br") engine.lang else engine.lcIn
        val to = if (engine.mode == "br") "" else if (engine.lcOut == "English" || engine.lcOut == engine.lcIn) "" else engine.lcOut
        if (c in 0x11052..0x1105B) {                       // number keys 1..9 and the 10 sign (typed as 0)
            val i = c - 0x11052 + 1; val d = if (i == 10) 0 else i
            val dev = String(Character.toChars(0x966 + d))
            val id = if (engine.mode == "br") d.toString() else dev       // Brahmi mode sends 0-9 so the Ashokan numeral rules apply
            val shadow = if (engine.mode == "br") (if (i < 10) sg(c) else "") else if (to.isEmpty()) "" else engine.toScript(dev, to)
            return K("c:$id", small(engine.toScript(dev, from), shadow), "br")
        }
        val dev = engine.fromBr(sg(c))
        val sign = dev.isNotEmpty() && devSign(dev[0])
        val mark = if (sign) "◌" else ""
        val shadow = if (engine.mode == "br") (if (comb(c)) "◌" else "") + sg(c) else if (to.isEmpty()) "" else mark + engine.toScript(dev, to)
        return K("c:$dev", small(mark + engine.toScript(dev, from), shadow), "br")
    }

    /** The 123 key also shows a small smiley: the emoji picker is behind it. */
    private fun numKey() = K("num", small("123", "☺"), "fn")

    private fun ab(l: String) = if (l == "English") "Eng" else l.take(3)

    /** From and to, so the user sees what is converted into what. */
    private fun langLabel() = when (engine.mode) {
        "br" -> ab(engine.lang) + "→Brh"
        "pb" -> "Brh→" + ab(engine.lang)
        else -> ab(engine.lcIn) + "→" + ab(engine.lcOut)
    }

    private fun layout(): List<List<K>> {
        val sh = K("sh", if (shift == 2) "⇪" else "⇧", if (shift != 0) "fnon" else "fn")
        val modeK = K("mode", small("Mode", if (plain) "eng" else when (engine.mode) { "lc" -> "cus"; else -> engine.mode }), "fn")
        val langK = K("lang", langLabel(), "fn")
        val goK = K("go", "Go", "fn")
        val setK = K("set", "⚙", "fn")
        val sp = K("sp", "space", "sp")
        if (layer == "lang") {
            val names = (if (engine.mode == "lc") listOf("English") else emptyList()) + Data.tables(this).languages
            val cur = if (engine.mode == "lc") engine.lcOut else engine.lang
            val top = if (engine.mode == "lc") listOf(listOf(K("lang2", "Typing with: " + engine.lcIn + " ▸", "fn"))) else emptyList()
            return top + names.chunked(4).map { r -> r.map { K("L:$it", it, if (it == cur) "fnon" else "fn") } } + listOf(listOf(K("abc", "← back", "fn")))
        }
        if (layer == "lang2") {
            val names = listOf("English") + engine.nativeLanguages()
            return names.chunked(4).map { r -> r.map { K("I:$it", it, if (it == engine.lcIn) "fnon" else "fn") } } + listOf(listOf(K("abc", "← back", "fn")))
        }
        if (layer == "emoji") return listOf(listOf(K("abc", "ABC", "fn"), sp, K("bk", "⌫", "fn")))
        if (layer == "num") {
            fun r(s: String) = s.map { K("c:$it", it.toString()) }
            return listOf(r("1234567890"), r("@#₹%&*-+()"), r("!\"':;/?_=~"),
                listOf(K("abc", "ABC", "fn")) + r(",.€£^<>") + K("bk", "⌫", "fn"),
                listOf(K("abc", "← back", "fn")) + K("emoji", "😊", "fn") + sp + K("en", "↵", "fn") + goK)
        }
        if (gridMode()) {
            val bottom = listOf(sh, modeK, setK, pbKey(0x11030), sp, pbKey(0x11031), langK, goK)
            return listOf(pr1, pr2, pr3, pr4, pr5).map { r -> r.map { pbKey(it) } } + listOf(bottom)
        }
        val out = ArrayList<List<K>>()
        val top = listOf(K("c:@", "@")) + "1234567890".map { K("c:$it", it.toString()) } + listOf(K("c:#", "#"), K("c:%", "%"), K("del", "Del", "fn"))
        out.add(top)
        // English letters going to another script: a sign row (vowel signs, anusvara, joiner) for words like नोकेश्वर.
        if (engine.lcOut != "English") out.add(signRow.map { K("c:$it", "◌" + engine.toScript(it.toString(), engine.lcOut)) })
        run {
            fun cs(c: Char) = if (shift != 0 && !c.isDigit()) c.uppercaseChar() else c
            fun letter(c: Char) = K("c:$c", small(cs(c).toString(), cap2(c.toString())))
            out.add("qwertyuiop".map { letter(it) })
            out.add("asdfghjkl".map { letter(it) } + K("en", "↵", "fn"))
            out.add(listOf(numKey()) + "zxcvbnm".map { letter(it) } + listOf(K("c:.", "."), K("c:,", ","), K("c:?", "?"), K("bk", "⌫", "fn")))
        }
        out.add(listOf(sh, modeK, setK, sp, langK, goK))
        return out
    }

    // ---------- drawing ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private class Pal(val bg: Int, val key: Int, val fn: Int, val tx: Int, val ac: Int, val acTx: Int, val strip: Int, val stripTx: Int = tx)

    private fun pal(): Pal {
        val dark = when (prefs.theme) { "dark" -> true; "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES }
        val base = if (dark) Pal(Color.parseColor("#2D2A21"), Color.parseColor("#3A362B"), Color.parseColor("#4A4535"), Color.parseColor("#F0EBDD"), Color.parseColor("#E0A458"), Color.parseColor("#1B1914"), Color.parseColor("#26231C"))
        else Pal(Color.parseColor("#DCD6C6"), Color.WHITE, Color.parseColor("#C9C2AE"), Color.parseColor("#1D1B16"), Color.parseColor("#8A4B14"), Color.parseColor("#F4F1EA"), Color.parseColor("#F4F1EA"))
        // Colours chosen in Settings override the theme.
        fun mine(h: String): Int? = try { if (h.isEmpty()) null else Color.parseColor(h) } catch (e: Exception) { null }
        val kbc = mine(prefs.kbColor); val kc = mine(prefs.keyColor); val tc = mine(prefs.textColor)
        fun shade(c: Int, t: Float): Int {
            val light = (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000 > 140
            val to = if (light) 0 else 255
            fun m(a: Int) = (a + (to - a) * t).toInt()
            return Color.rgb(m(Color.red(c)), m(Color.green(c)), m(Color.blue(c)))
        }
        return Pal(kbc ?: base.bg, kc ?: base.key, if (kc != null) shade(kc, 0.14f) else base.fn, tc ?: base.tx, base.ac, base.acTx, mine(prefs.stripColor) ?: if (kbc != null) shade(kbc, 0.08f) else base.strip, mine(prefs.stripText) ?: tc ?: base.tx)
    }

    private fun rounded(c: Int, r: Int = 7) = GradientDrawable().apply { setColor(c); cornerRadius = dp(r).toFloat() }

    /** Width, side and bottom spacing from Settings, plus the phone's own navigation bar. */
    private fun applyFrame() {
        if (!::outer.isInitialized) return
        val sw = resources.displayMetrics.widthPixels
        val w = (sw * prefs.widthPct.coerceIn(60, 100) / 100f).toInt()
        val g = when (prefs.align) { "left" -> Gravity.START; "right" -> Gravity.END; else -> Gravity.CENTER_HORIZONTAL }
        root.layoutParams = FrameLayout.LayoutParams(w, -2, Gravity.BOTTOM or g)
        outer.setPadding(0, 0, 0, (if (prefs.autoInset) navInset else 0) + dp(prefs.gap))
    }

    private fun rebuild() {
        if (!::root.isInitialized) return
        val p = pal()
        outer.setBackgroundColor(p.bg)
        root.setBackgroundColor(p.bg)
        applyFrame()
        (tag.parent as View).setBackgroundColor(p.strip)
        tag.background = rounded(p.ac, 4); tag.setTextColor(p.acTx); prev.setTextColor(p.stripTx)
        keys.removeAllViews()
        val pb = gridMode() && layer == "abc"
        val h = prefs.keyH.coerceIn(34, 72)
        if (layer == "emoji") { buildEmoji(p, h); drawPreview(); return }
        for (row in layout()) keys.addView(keyRow(row, p, h, pb), LinearLayout.LayoutParams(-1, -2))
        drawPreview()
    }

    private fun keyRow(row: List<K>, p: Pal, h: Int, pb: Boolean): LinearLayout {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (k in row) {
            val fn = k.kind == "fn" || k.kind == "fnon"
            val v = TextView(this).apply {
                text = k.label; gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 2
                textSize = if (fn) 12f else if (k.kind == "br") (if (pb) 17f else 20f) else 16f
                if (k.kind == "br" || (pb && !fn)) typeface = brahmiFont
                setTextColor(if (k.kind == "fnon") p.acTx else p.tx)
                background = rounded(if (k.kind == "fnon") p.ac else if (fn) p.fn else p.key).also {
                    // Mode, Go and the joiner are the important keys: amber border.
                    if (k.id == "mode" || k.id == "go" || k.id == "c:" + sg(0x11046) || k.id == "c:\u094D") it.setStroke(dp(2), Color.parseColor("#F5A623"))
                }
                setOnTouchListener { view, e -> touch(view, e, k.id); true }
            }
            val w = when (k.kind) { "sp" -> 4f; "fn", "fnon" -> 1.4f; else -> 1f }
            r.addView(v, LinearLayout.LayoutParams(0, dp(if (pb) h - 4 else h), w).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        }
        return r
    }

    // ---------- emoji ----------
    private var emojiGroup = 0          // 0 = recently used, 1.. = Emoji.groups

    private fun pickEmoji(e: String) {
        commit(null)
        currentInputConnection?.commitText(e, 1)
        prefs.recentEmoji = (listOf(e) + prefs.recentEmoji.split(" ").filter { it.isNotEmpty() && it != e }).take(24).joinToString(" ")
    }

    private fun buildEmoji(p: Pal, h: Int) {
        fun tab(label: String, idx: Int) = TextView(this).apply {
            text = label; textSize = 18f; gravity = Gravity.CENTER
            setTextColor(if (idx == emojiGroup) p.acTx else p.tx)
            background = rounded(if (idx == emojiGroup) p.ac else p.fn)
            setOnClickListener { emojiGroup = idx; rebuild() }
        }
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val labels = listOf("🕘") + Emoji.groups.map { it.icon }
        labels.forEachIndexed { i, l -> tabs.addView(tab(l, i), LinearLayout.LayoutParams(0, dp(h - 6), 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }) }
        keys.addView(tabs, LinearLayout.LayoutParams(-1, -2))

        val list = if (emojiGroup == 0) prefs.recentEmoji.split(" ").filter { it.isNotEmpty() } else Emoji.list(emojiGroup - 1)
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (list.isEmpty()) grid.addView(TextView(this).apply { text = "Emoji you use will appear here."; setTextColor(p.tx); setPadding(dp(12), dp(12), dp(12), dp(12)) })
        for (row in list.chunked(8)) {
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (i in 0 until 8) {
                val e = row.getOrNull(i)
                r.addView(TextView(this).apply {
                    text = e ?: ""; textSize = 24f; gravity = Gravity.CENTER; includeFontPadding = false
                    if (e != null) setOnClickListener { pickEmoji(e) }
                }, LinearLayout.LayoutParams(0, dp(h), 1f))
            }
            grid.addView(r, LinearLayout.LayoutParams(-1, -2))
        }
        keys.addView(ScrollView(this).apply { addView(grid) }, LinearLayout.LayoutParams(-1, dp(h) * 4))
        keys.addView(keyRow(layout()[0], p, h, false), LinearLayout.LayoutParams(-1, -2))
    }

    private val repeat = object : Runnable { var id = ""; override fun run() { handle(id); ui.postDelayed(this, 60) } }

    private var longFired = false
    private val modeLong = Runnable { longFired = true; togglePlain() }

    private fun touch(v: View, e: MotionEvent, id: String) {
        if (id == "mode") {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { v.alpha = 0.7f; longFired = false; ui.postDelayed(modeLong, 500) }
                MotionEvent.ACTION_UP -> { v.alpha = 1f; ui.removeCallbacks(modeLong); if (!longFired) handle("mode") }
                MotionEvent.ACTION_CANCEL -> { v.alpha = 1f; ui.removeCallbacks(modeLong) }
            }
            return
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.alpha = 0.7f; handle(id)
                if (id == "bk") { repeat.id = id; ui.postDelayed(repeat, 400) }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.alpha = 1f; ui.removeCallbacks(repeat) }
        }
    }
}
