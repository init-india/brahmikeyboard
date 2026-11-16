package org.brahmikeyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every key followed by every key, in every mode and language: the engine must keep both, and the preview must show them. */
class KeyPairTest {
    private val tables = Tables(File("../../data/tables.json").readText())
    private fun engine(mode: String) = BrahmiEngine(tables).also { it.mode = mode; it.smart = false }

    private val english = (('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('.', ',', '?', '@', '#', '%')).map { it.toString() }
    private val devanagari = (0x0905..0x0914).map { String(Character.toChars(it)) } + (0x0915..0x0939).map { String(Character.toChars(it)) } +
        (0x093E..0x094D).map { String(Character.toChars(it)) }
    private val brahmi = (0x11005..0x11046).map { String(Character.toChars(it)) }

    /** Types a then b. When neither key was refused, both must be in the word and visible in the preview. */
    private fun check(e: BrahmiEngine, a: String, b: String, label: String) {
        e.clear()
        val r1 = e.add(a); val p1 = e.buf
        if (r1.pop != null) return
        val before = e.preview()
        val r2 = e.add(b)
        if (r2.pop != null) { assertEquals("$label $a$b: refused key changed the word", p1, e.buf); return }
        assertTrue("$label $a$b: second key missing from the word", e.buf.endsWith(b))
        assertTrue("$label $a$b: preview empty", e.preview().isNotEmpty())
        assertTrue("$label $a$b: preview did not change after the second key", e.preview() != before || before.isEmpty() && e.preview().isNotEmpty() || b.isNotEmpty())
    }

    @Test fun englishKeysEnglishOutput() {
        val e = engine("lc")
        for (a in english) for (b in english) {
            e.clear(); e.add(a); e.add(b)
            assertEquals("$a$b", e.buf); assertEquals("$a$b", e.preview()); assertEquals("$a$b", e.commitText())
        }
    }

    @Test fun englishKeysEveryOutputLanguage() {
        for (lang in tables.languages) {
            val e = engine("lc"); e.lcOut = lang
            for (a in english) for (b in english) {
                e.clear(); e.add(a); e.add(b)
                assertEquals("$lang $a$b", a + b, e.buf)
                assertTrue("$lang $a$b: preview empty", e.preview().isNotEmpty())
            }
        }
    }

    @Test fun nativeKeysEveryInputAndOutputLanguage() {
        val e0 = engine("lc")
        for (lin in e0.nativeLanguages()) for (lout in listOf("English") + tables.languages) {
            val e = engine("lc"); e.lcIn = lin; e.lcOut = lout
            for (a in devanagari) for (b in devanagari) check(e, a, b, "$lin→$lout")
        }
    }

    @Test fun brahmiModeEveryLanguage() {
        for (lang in tables.languages) {
            val e = engine("br"); e.lang = lang
            for (a in devanagari) for (b in devanagari) check(e, a, b, "br $lang")
            for (a in english) for (b in english) check(e, a, b, "br $lang (roman)")
        }
    }

    @Test fun pureBrahmiEveryLanguage() {
        for (lang in tables.languages) {
            val e = engine("pb"); e.lang = lang
            for (a in brahmi) for (b in brahmi) check(e, a, b, "pb $lang")
        }
    }
}
