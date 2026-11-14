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
