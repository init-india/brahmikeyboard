package org.brahmikeyboard

import org.json.JSONArray
import org.json.JSONObject

/** One script table for non-offset scripts (Perso-Arabic, Ol Chiki, Meitei Mayek, Sinhala, IAST ...). */
class ScriptTable(
    val c: Map<Int, String>, val v: Map<Int, String>, val m: Map<Int, String>,
    val s: Map<Int, String>, val inherent: String, val digitBase: Int, val full: Boolean,
)

/** All conversion data, loaded from data/tables.json (generated from the web prototype). */
class Tables(json: String) {
    val cons = HashMap<String, Int>()
    val vow = HashMap<String, Pair<Int, Int>>()
    val sign = HashMap<String, Int>()
    val devToBrahmi = HashMap<Int, Int>()
    val brahmiToDev = HashMap<Int, Int>()
    val fold = ArrayList<Pair<String, String>>()
    val scripts = HashMap<String, String>()           // language -> script
    val langOffset = HashMap<String, Int>()           // script -> Unicode block start
    val langTable = HashMap<String, String>()         // script -> table key
    val tamilFold = HashMap<Int, Int>()
    val tables = HashMap<String, ScriptTable>()

    init {
        val o = JSONObject(json)
        o.getJSONObject("cons").let { j -> j.keys().forEach { cons[it] = j.getInt(it) } }
        o.getJSONObject("vow").let { j -> j.keys().forEach { val a = j.getJSONArray(it); vow[it] = Pair(a.getInt(0), a.getInt(1)) } }
        o.getJSONObject("sign").let { j -> j.keys().forEach { sign[it] = j.getInt(it) } }
        o.getJSONArray("devToBrahmi").let { a ->
            for (i in 0 until a.length()) { val p = a.getJSONArray(i); devToBrahmi[p.getInt(0)] = p.getInt(1) }
        }
        devToBrahmi.forEach { (d, b) -> brahmiToDev[b] = d }
        brahmiToDev[0x11039] = 0x93E
        o.getJSONArray("fold").let { a ->
            for (i in 0 until a.length()) { val p = a.getJSONArray(i); fold.add(Pair(p.getString(0), p.getString(1))) }
        }
        o.getJSONObject("scripts").let { j -> j.keys().forEach { scripts[it] = j.getString(it) } }
        o.getJSONObject("langs").let { j ->
            j.keys().forEach { val x = j.get(it); if (x is Int) langOffset[it] = x else langTable[it] = x.toString() }
        }
        o.getJSONObject("tamilFold").let { j -> j.keys().forEach { tamilFold[it.toInt()] = j.getInt(it) } }
        o.getJSONObject("tables").let { j ->
            j.keys().forEach { k ->
                val t = j.getJSONObject(k)
                tables[k] = ScriptTable(
                    intMap(t.getJSONObject("c")), intMap(t.getJSONObject("v")), intMap(t.getJSONObject("m")),
                    intMap(t.getJSONObject("s")), t.optString("i", ""), t.optInt("d", 0x30), t.optBoolean("f", false),
                )
            }
        }
    }

    private fun intMap(j: JSONObject): Map<Int, String> {
        val r = HashMap<Int, String>()
        j.keys().forEach { r[it.toInt()] = j.getString(it) }
        return r
    }

    /** Language names shown in the picker, in display order. */
    val languages: List<String> get() = scripts.keys.sorted()
    /** Script names that can override the default. */
    val scriptNames: List<String> get() = (langOffset.keys + langTable.keys).toList()
}
