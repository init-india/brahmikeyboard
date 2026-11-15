package org.brahmikeyboard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class LocalModeTest {
    private fun engine() = BrahmiEngine(Tables(File("../../data/tables.json").readText())).also { it.mode = "lc" }

    private fun typed(e: BrahmiEngine, s: String): String { e.clear(); s.forEach { e.add(it.toString()) }; return e.commitText() }

    @Test fun englishToEnglishIsUnchanged() {
        val e = engine(); assertEquals("hello", typed(e, "hello")); assertEquals("hello", e.preview())
    }
    @Test fun englishToKannada() {
        val e = engine(); e.lcOut = "Kannada"
        assertEquals("ನಮಸ್ತೇ", typed(e, "namaste"))
        assertEquals("namaste → ನಮಸ್ತೇ", e.preview())
    }
    @Test fun hindiKeysToTamil() {
        val e = engine(); e.lcIn = "Hindi"; e.lcOut = "Tamil"
        assertEquals("நமஸ்தே", typed(e, "नमस्ते"))
    }
    @Test fun hindiKeysToEnglish() {
        val e = engine(); e.lcIn = "Hindi"; e.lcOut = "English"
        assertEquals("namaste", typed(e, "नमस्ते"))
    }
    @Test fun localModeHasNoAshokanRules() {
        val e = engine(); e.lcOut = "Hindi"
        assertEquals("क्र", typed(e, "kra"))   // क्र is allowed outside Brahmi modes
    }
    @Test fun urduOutputHasNoHiddenMarks() {
        val e = engine(); e.lcOut = "Urdu"
        assertEquals(false, typed(e, "kitab").contains('⁨'))
    }
    @Test fun nativeLanguageList() {
        val l = engine().nativeLanguages()
        assertEquals(true, "Hindi" in l && "Kannada" in l && "Tamil" in l); assertEquals(false, "Urdu" in l)
    }
}

class SignRowTest {
    private fun engine(mode: String) = BrahmiEngine(Tables(File("../../data/tables.json").readText())).also { it.mode = mode }
    // n + ो , k + े , sh + joiner + v , a , r  (the sign-row keys send Devanagari signs into the typed word)
    private val keys = listOf("n", "ो", "k", "े", "s", "h", "्", "v", "a", "r")

    private fun type(e: BrahmiEngine): String { e.clear(); keys.forEach { e.add(it) }; return e.commitText() }

    @Test fun brahmiMode() {
        val e = engine("br"); e.lang = "Hindi"
        assertEquals("𑀦𑁄𑀓𑁂𑀰𑁆𑀯𑀭", type(e))
        assertEquals("नोकेश्वर", e.preview().substringBefore(" →"))
    }
    @Test fun localModeToTamil() {
        val e = engine("lc"); e.lcOut = "Tamil"
        assertEquals("நோகேஶ்வர", type(e))
    }
    @Test fun nativeInputToTamil() {
        val e = engine("lc"); e.lcIn = "Hindi"; e.lcOut = "Tamil"
        e.clear(); "नोकेश्वर".forEach { e.add(it.toString()) }
        assertEquals("நோகேஶ்வர", e.commitText())
    }
}

/** Brahmi mode now types the language's own letters (the Pure Brahmi layout with language keys). */
class LanguageKeysTest {
    private fun engine() = BrahmiEngine(Tables(File("../../data/tables.json").readText())).also { it.mode = "br"; it.lang = "Hindi" }
    private fun type(e: BrahmiEngine, keys: String): AddResult? { var last: AddResult? = null; e.clear(); keys.forEach { last = e.add(it.toString()) }; return last }

    @Test fun hindiLettersToBrahmi() {
        val e = engine(); type(e, "नोकेश्वर")
        assertEquals("𑀦𑁄𑀓𑁂𑀰𑁆𑀯𑀭", e.commitText())
        assertEquals("नोकेश्वर", e.preview().substringBefore(" →"))
    }
    @Test fun tamilPreview() {
        val e = engine(); e.lang = "Tamil"; type(e, "नमस्ते")
        assertEquals("நமஸ்தே → 𑀦𑀫𑀲𑁆𑀢𑁂", e.preview())
    }
    @Test fun raConjunctStillBlocked() {
        val e = engine(); type(e, "क"); val r = e.add("्"); val r2 = e.add("र")
        assertEquals(true, r2.pop != null)
    }
    @Test fun digitsMakeAshokanNumerals() {
        val e = engine(); type(e, "256"); assertEquals("𑁓𑁿𑁤𑁟𑁗", e.commitText())
    }
    @Test fun aiAuWarnsButKeepsLetters() {
        val e = engine(); val r = type(e, "ऐ"); assertEquals("𑀐", e.commitText()); assertEquals(true, r!!.warn != null)
    }
}

/** Numbers in Pure Brahmi mode obey the Ashokan limits too; Custom mode does not. */
class NumberLimitTest {
    private fun engine(mode: String) = BrahmiEngine(Tables(File("../../data/tables.json").readText())).also { it.mode = mode; it.lang = "Hindi" }
    private fun type(e: BrahmiEngine, keys: List<String>): List<String?> { e.clear(); return keys.map { e.add(it).pop } }
    private val thousand = String(Character.toChars(0x11065))

    @Test fun pureBrahmiDigitsBecomeAshokanNumerals() {
        val e = engine("pb"); type(e, "256".map { it.toString() })
        assertEquals("𑁓𑁿𑁤𑁟𑁗", e.commitText())
    }
    @Test fun pureBrahmiRefusesOver9999() {
        val e = engine("pb"); val r = type(e, "10000".map { it.toString() })
        assertEquals("Ashokan numerals here go up to 9999.", r.last()); assertEquals("1000", e.buf)
    }
    @Test fun pureBrahmiRefusesZero() {
        val e = engine("pb"); assertEquals("Ashokan Brahmi has no zero.", type(e, listOf("0")).last())
    }
    @Test fun pureBrahmiNumeralSignsStopAt9999() {
        val e = engine("pb"); val r = type(e, List(10) { thousand })
        assertEquals(null, r[8]); assertEquals("Ashokan numerals here go up to 9999.", r[9]); assertEquals(9, e.buf.codePointCount(0, e.buf.length))
    }
    @Test fun brahmiModeStillRefusesOver9999() {
        val e = engine("br"); assertEquals("Ashokan numerals here go up to 9999.", type(e, "10000".map { it.toString() }).last())
    }
    @Test fun customModeAllowsAnyNumber() {
        val e = engine("lc"); val r = type(e, "1234567890".map { it.toString() })
        assertEquals(true, r.all { it == null }); assertEquals("1234567890", e.commitText())
    }
}
