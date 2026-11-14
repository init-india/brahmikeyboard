package org.brahmikeyboard

/** Result of typing one key in Brahmi or Pure Brahmi mode. */
data class AddResult(val pop: String? = null, val warn: String? = null)

/**
 * Pure conversion engine. No Android classes, so it runs in plain JVM unit tests.
 * It is a line-by-line port of the web prototype (web/index.html); data/tests.json is
 * generated from the prototype and must pass here unchanged.
 */
class BrahmiEngine(private val t: Tables) {
    var mode = "en"          // "en" | "br" | "pb"
    var lang = "Hindi"
    var script = ""
    var smart = false
    var buf = ""
        private set

    // ---------- helpers ----------
    private fun cps(s: String): List<Int> = s.codePoints().toArray().toList()
    private fun str(c: Int) = String(Character.toChars(c))
    private fun lastCp(s: String): Int? = if (s.isEmpty()) null else s.codePointBefore(s.length)

    // ---------- roman -> Devanagari ----------
    private fun has(k: String) = k in t.cons || k in t.vow || k in t.sign

    fun toDev(input: String): String {
        val s = input.map { if (it in "AEIOU") it.lowercaseChar() else it }.joinToString("")
        val o = StringBuilder(); var pc = false; var i = 0
        while (i < s.length) {
            var k: String? = if (i + 2 <= s.length) s.substring(i, i + 2) else s.substring(i)
            if (!has(k!!)) k = if (has(s[i].toString())) s[i].toString() else null
            if (k == null) { o.append(s[i++]); pc = false; continue }
            i += k.length
            if (k in t.cons) {
                if (pc) o.append(0x94D.toChar())
                o.append(t.cons[k]!!.toChar()); pc = true
            } else if (k in t.vow) {
                val (v, m) = t.vow[k]!!
                if (pc) { if (m != 0) o.append(m.toChar()) } else o.append(v.toChar())
                pc = false
            } else { o.append(t.sign[k]!!.toChar()); pc = false }
        }
        return o.toString()
    }

    // ---------- Devanagari <-> Brahmi ----------
    fun fold(s: String): String {
        var a = s
        for ((x, y) in t.fold) a = a.replace(x, y)
        return a
    }
    fun toBr(s: String) = buildString { for (c in cps(s)) append(str(t.devToBrahmi[c] ?: c)) }
    fun fromBr(s: String) = buildString { for (c in cps(s)) append(str(t.brahmiToDev[c] ?: c)) }

    // ---------- Devanagari -> preferred script ----------
    private fun resolvedScript(l: String): String =
        if (script.isNotEmpty() && l == lang) script else (t.scripts[l] ?: l)

    private fun tbl(dev: String, tb: ScriptTable): String {
        val a = cps(dev); val o = StringBuilder(); var i = 0
        while (i < a.size) {
            val n = a[i]
            val c = tb.c[n]
            if (c != null) {
                o.append(c)
                val y = if (i + 1 < a.size) a[i + 1] else null
                val mm = if (y != null) tb.m[y] else null
                if (mm != null) { o.append(mm); i++ }
                else if (y == 0x94D) { o.append(tb.s[0x94D] ?: ""); i++ }
                else if (tb.inherent.isNotEmpty() && ((y != null && y != 0) || tb.full)) o.append(tb.inherent)
            } else if (tb.v[n] != null) o.append(tb.v[n])
            else if (tb.s[n] != null) o.append(tb.s[n])
            else if (n in 0x966..0x96F) o.append(str(tb.digitBase + n - 0x966))
            else o.append(str(n))
            i++
        }
        return o.toString()
    }

    fun toScript(devIn: String, l: String): String {
        val sc = resolvedScript(l)
        var dev = devIn
        if (sc == "Tamil" || sc == "Gurmukhi") for ((x, y) in t.fold.take(8)) dev = dev.replace(x, y)
        t.langTable[sc]?.let { return "⁨" + tbl(dev, t.tables[it]!!) + "⁩" }
        val b = t.langOffset[sc] ?: 0x900
        val o = StringBuilder()
        for (c in cps(dev)) {
            var n = c
            if (n < 0x900 || n > 0x97F) { o.append(str(c)); continue }
            if (b > 0xFFFF && n >= 0x964) { o.append(str(c)); continue }
            if (sc == "Tamil") t.tamilFold[n]?.let { n = it }
            o.append(str(b + n - 0x900))
        }
        return o.toString()
    }

    // ---------- numerals ----------
    private fun ashNum(s: String): String {
        val n = s.toIntOrNull() ?: return s
        if (!(n in 1..9999)) return s
        fun d(k: Int) = str(0x11052 + k - 1)
        val j = str(0x1107F)
        val th = n / 1000; val h = n % 1000 / 100; val te = n % 100 / 10; val u = n % 10
        val o = StringBuilder()
        if (th > 0) { if (th > 1) o.append(d(th)).append(j); o.append(str(0x11065)) }
        if (h > 0) { if (h > 1) o.append(d(h)).append(j); o.append(str(0x11064)) }
        if (te > 0) o.append(str(if (te == 1) 0x1105B else 0x1105C + te - 2))
        if (u > 0) o.append(d(u))
        return o.toString()
    }
    private val digits = Regex("[0-9]+")
    fun num2br(s: String) = digits.replace(s) { ashNum(it.value) }

    // ---------- what gets sent / shown ----------
    fun conv(b: String): String = when (mode) {
        "br" -> toBr(num2br(fold(toDev(b))))
        else -> b
    }
    fun commitText(): String = conv(buf)

    fun preview(): String = when (mode) {
        "br" -> if (buf.isEmpty()) "" else { val d = toDev(buf); toScript(d, lang) + " → " + toBr(num2br(fold(d))) }
        "pb" -> if (buf.isEmpty()) "" else buf + " → " + toScript(fromBr(buf), lang)
        else -> buf
    }

    // ---------- typing rules ----------
    private val voc = Regex("[ऋऌॠॡृॄॢॣ]")
    private val absRe = Regex("[ँः़॒॑ऽॅॆॉॊऍऎऑऒ]")
    private val wRe = Regex("[शषऐऔैौ]")
    private val raRe = Regex("्र|र्")
    private val raMsg = "Not in Ashokan Brahmi: conjuncts with ra, such as kra, tra and rka, cannot be typed."
    private val t2 = mapOf("ऐ" to "ए", "औ" to "ओ", "ै" to "े", "ौ" to "ो")
    private val mt = mapOf(
        0x11006 to 0x11038, 0x11007 to 0x1103A, 0x11008 to 0x1103B, 0x11009 to 0x1103C, 0x1100A to 0x1103D,
        0x1100B to 0x1103E, 0x1100C to 0x1103F, 0x1100D to 0x11040, 0x1100E to 0x11041, 0x1100F to 0x11042,
        0x11010 to 0x11043, 0x11011 to 0x11044, 0x11012 to 0x11045,
    )
    private fun isCons(c: Int) = c in 0x11013..0x11034

    /** Type one key (already upper-cased by the UI when Shift is on). Returns a pop-up or warning to show. */
    fun add(ch: String): AddResult {
        if (mode == "pb" && smart) {
            val cp = ch.codePointAt(0); val l = lastCp(buf)
            if ((mt[cp] != null || cp == 0x11005) && l != null && isCons(l)) {
                mt[cp]?.let { buf += str(it) }
                return AddResult()
            }
        }
        if (mode == "br") {
            val dn = toDev(buf + ch); val dp = toDev(buf)
            if (voc.containsMatchIn(dn) && !voc.containsMatchIn(dp)) return AddResult(pop = "Not in Ashokan Brahmi: ṛ and ḷ cannot be typed.")
            if (absRe.containsMatchIn(dn) && !absRe.containsMatchIn(dp))
                return AddResult(pop = "Not in Ashokan Pali: visarga, candrabindu, nukta, accents, avagraha and the candra and short vowels cannot be typed.")
            if (ch.length == 1 && ch[0] in '0'..'9') {
                val tail = Regex("[0-9]*$").find(buf)!!.value
                val n = (tail + ch).toLong()
                if (n == 0L) return AddResult(pop = "Ashokan Brahmi has no zero.")
                if (n > 9999) return AddResult(pop = "Ashokan numerals here go up to 9999.")
            }
        }
        if (mode == "br" && raRe.containsMatchIn(toDev(buf + ch)) && !raRe.containsMatchIn(toDev(buf))) return AddResult(pop = raMsg)
        if (mode == "pb") {
            val ra = str(0x1102D); val vir = str(0x11046)
            if ((ch == ra && buf.endsWith(vir)) || (ch == vir && buf.endsWith(ra))) return AddResult(pop = raMsg)
        }
        var warn: String? = null
        if (mode == "br" || mode == "pb") {
            val wa = if (mode == "br") toDev(buf + ch) else fromBr(buf + ch)
            val wb = if (mode == "br") toDev(buf) else fromBr(buf)
            if (wRe.findAll(wa).count() > wRe.findAll(wb).count()) {
                val wc = wRe.findAll(wa).last().value
                val sign = wc == "ै" || wc == "ौ"
                fun lab(c: String) = (if (sign) "◌" else "") + toScript(c, lang)
                val to = t2[wc]
                warn = if (to != null) "Ashokan script treats ${lab(wc)} as ${lab(to)}. You still get the actual ${lab(wc)}."
                else "Post-Ashokan: ${lab(wc)} is not found in Ashokan script. You can still type it."
            }
        }
        buf += ch
        return AddResult(warn = warn)
    }

    fun backspace() { if (buf.isNotEmpty()) buf = buf.substring(0, buf.offsetByCodePoints(buf.length, -1)) }
    fun clear() { buf = "" }
}
