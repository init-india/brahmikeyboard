package org.brahmikeyboard

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Runs every case in data/tests.json (generated from the web prototype) against the Kotlin engine. */
class EngineTest {
    private val data = File("../../data")

    @Test fun sharedCases() {
        val engine = BrahmiEngine(Tables(File(data, "tables.json").readText()))
        val cases = JSONObject(File(data, "tests.json").readText()).getJSONArray("cases")
        val failures = ArrayList<String>()
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            engine.mode = c.getString("mode"); engine.lang = c.getString("lang"); engine.script = ""
            engine.clear()
            val keys = c.getJSONArray("keys")
            val got = ArrayList<String>()
            for (k in 0 until keys.length()) {
                val r = engine.add(keys.getString(k))
                r.pop?.let { got.add("pop:$it") }; r.warn?.let { got.add("warn:$it") }
            }
            val want = ArrayList<String>()
            val m = c.getJSONArray("msgs")
            for (k in 0 until m.length()) want.add(m.getJSONArray(k).let { "${it.getString(0)}:${it.getString(1)}" })
            val label = "${c.getString("mode")}/${c.getString("lang")}/${keys.join("")}"
            if (engine.buf != c.getString("buf")) failures.add("$label buf ${engine.buf} != ${c.getString("buf")}")
            if (engine.commitText() != c.getString("commit")) failures.add("$label commit ${engine.commitText()} != ${c.getString("commit")}")
            if (engine.preview() != c.getString("preview")) failures.add("$label preview ${engine.preview()} != ${c.getString("preview")}")
            if (got != want) failures.add("$label msgs $got != $want")
        }
        assertEquals(failures.take(20).joinToString("\n"), 0, failures.size)
    }
}
