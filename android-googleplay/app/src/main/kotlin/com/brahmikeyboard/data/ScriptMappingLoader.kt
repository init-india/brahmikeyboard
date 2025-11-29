package com.brahmikeyboard.data

import android.content.res.AssetManager
import kotlinx.serialization.json.Json
import java.io.InputStream

class ScriptMappingLoader(private val assets: AssetManager) {
    
    private val scriptMappings = mutableMapOf<String, Map<String, String>>()
    private val romanToIndianMappings = mutableMapOf<String, Map<String, String>>()
    private val wordLevelMappings = mutableMapOf<String, Map<String, String>>()
    
    // Brahmi combinations sorted by length (longest first)
    private val brahmiCombinations = listOf(
        "aa", "ee", "uu", "ei", "ou",
        "kh", "gh", "nga", "ch", "jh", "yn",
        "Th", "Dh", "N", "th", "dh", "ph", "bh", "L"
    ).sortedByDescending { it.length }

    // Complete Brahmi to Roman mapping
    private val brahmiToRomanMap = mapOf(
        // Vowels
        "𑀅" to "a", "𑀆" to "aa", "𑀇" to "i", "𑀈" to "ee",
        "𑀉" to "u", "𑀊" to "uu", "𑀏" to "e", "𑀐" to "ei",
        "𑀑" to "o", "𑀒" to "ou",
        
        // Consonants
        "𑀓" to "k", "𑀔" to "kh", "𑀕" to "g", "𑀖" to "gh",
        "𑀗" to "nga", "𑀘" to "c", "𑀙" to "ch", "𑀚" to "j",
        "𑀛" to "jh", "𑀜" to "yn", "𑀝" to "T", "𑀞" to "Th",
        "𑀟" to "D", "𑀠" to "Dh", "𑀡" to "N", "𑀢" to "t",
        "𑀣" to "th", "𑀤" to "d", "𑀥" to "dh", "𑀦" to "n",
        "𑀧" to "p", "𑀨" to "ph", "𑀩" to "b", "𑀪" to "bh",
        "𑀫" to "m", "𑀬" to "y", "𑀭" to "r", "𑀮" to "l",
        "𑀯" to "v", "𑀰" to "sh", "𑀱" to "Sh", "𑀲" to "s",
        "𑀳" to "h", "𑀴" to "L",
        
        // Vowel signs
        "𑀺" to "i", "𑀻" to "ee", "𑀼" to "u", "𑀽" to "uu",
        "𑁀" to "e", "𑁁" to "ei", "𑁂" to "e", "𑁃" to "ei",
        "𑁄" to "o", "𑁅" to "ou",
        
        // Special marks
        "𑁆" to "", // halant/virama
        "𑀀" to "", // anusvara
        "𑀁" to "", // anusvara
        "𑀂" to "", // visarga
        "𑀃" to "", // visarga
        
        // Numerals
        "𑁧" to "1", "𑁨" to "2", "𑁩" to "3", "𑁪" to "4",
        "𑁫" to "5", "𑁬" to "6", "𑁭" to "7", "𑁮" to "8",
        "𑁯" to "9", "𑁦" to "0"
    )

    private fun loadRomanToIndianMappings(): Map<String, Map<String, String>> {
        return try {
            val inputStream: InputStream = assets.open("script-mappings/roman-to-indian-scripts.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            Json.decodeFromString<Map<String, Map<String, String>>>(jsonString)
        } catch (e: Exception) {
            // Return comprehensive default mappings if file doesn't exist
            createCompleteRomanToIndianMappings()
        }
    }
    
    // COMPLETE DEFAULT MAPPINGS FOR ALL INDIAN LANGUAGES
    private fun createCompleteRomanToIndianMappings(): Map<String, Map<String, String>> {
        val defaultMappings = mutableMapOf<String, Map<String, String>>()
        
        // Devanagari (Hindi, Marathi, Sanskrit, Nepali)
        val devanagariMap = mapOf(
            // Vowels
            "a" to "अ", "aa" to "आ", "i" to "इ", "ee" to "ई",
            "u" to "उ", "uu" to "ऊ", "e" to "ए", "ei" to "ऐ",
            "o" to "ओ", "ou" to "औ",
            
            // Consonants
            "k" to "क", "kh" to "ख", "g" to "ग", "gh" to "घ",
            "nga" to "ङ", "c" to "च", "ch" to "छ", "j" to "ज",
            "jh" to "झ", "yn" to "ञ", "T" to "ट", "Th" to "ठ",
            "D" to "ड", "Dh" to "ढ", "N" to "ण", "t" to "त",
            "th" to "थ", "d" to "द", "dh" to "ध", "n" to "न",
            "p" to "प", "ph" to "फ", "b" to "ब", "bh" to "भ",
            "m" to "म", "y" to "य", "r" to "र", "l" to "ल",
            "v" to "व", "sh" to "श", "Sh" to "ष", "s" to "स",
            "h" to "ह", "L" to "ळ"
        )
        
        // Bengali (Bangla)
        val bengaliMap = mapOf(
            // Vowels
            "a" to "অ", "aa" to "আ", "i" to "ই", "ee" to "ঈ",
            "u" to "উ", "uu" to "ঊ", "e" to "এ", "ei" to "ঐ",
            "o" to "ও", "ou" to "ঔ",
            
            // Consonants
            "k" to "ক", "kh" to "খ", "g" to "গ", "gh" to "ঘ",
            "nga" to "ঙ", "c" to "চ", "ch" to "ছ", "j" to "জ",
            "jh" to "ঝ", "yn" to "ঞ", "T" to "ট", "Th" to "ঠ",
            "D" to "ড", "Dh" to "ঢ", "N" to "ণ", "t" to "ত",
            "th" to "থ", "d" to "দ", "dh" to "ধ", "n" to "ন",
            "p" to "প", "ph" to "ফ", "b" to "ব", "bh" to "ভ",
            "m" to "ম", "y" to "য", "r" to "র", "l" to "ল",
            "v" to "ৱ", "sh" to "শ", "Sh" to "ষ", "s" to "স",
            "h" to "হ", "L" to "ল"
        )
        
        // Tamil
        val tamilMap = mapOf(
            // Vowels
            "a" to "அ", "aa" to "ஆ", "i" to "இ", "ee" to "ஈ",
            "u" to "உ", "uu" to "ஊ", "e" to "ஏ", "ei" to "ஐ",
            "o" to "ஓ", "ou" to "ஔ",
            
            // Consonants
            "k" to "க", "kh" to "க", "g" to "க", "gh" to "க",
            "nga" to "ங", "c" to "ச", "ch" to "ச", "j" to "ஜ",
            "jh" to "ஜ", "yn" to "ஞ", "T" to "ட", "Th" to "ட",
            "D" to "ட", "Dh" to "ட", "N" to "ண", "t" to "த",
            "th" to "த", "d" to "த", "dh" to "த", "n" to "ந",
            "p" to "ப", "ph" to "ப", "b" to "ப", "bh" to "ப",
            "m" to "ம", "y" to "ய", "r" to "ர", "l" to "ல",
            "v" to "வ", "sh" to "ஷ", "Sh" to "ஸ", "s" to "ச",
            "h" to "ஹ", "L" to "ள"
        )
        
        // Telugu
        val teluguMap = mapOf(
            // Vowels
            "a" to "అ", "aa" to "ఆ", "i" to "ఇ", "ee" to "ఈ",
            "u" to "ఉ", "uu" to "ఊ", "e" to "ఏ", "ei" to "ఐ",
            "o" to "ఓ", "ou" to "ఔ",
            
            // Consonants
            "k" to "క", "kh" to "ఖ", "g" to "గ", "gh" to "ఘ",
            "nga" to "ఙ", "c" to "చ", "ch" to "ఛ", "j" to "జ",
            "jh" to "ఝ", "yn" to "ఞ", "T" to "ట", "Th" to "ఠ",
            "D" to "డ", "Dh" to "ఢ", "N" to "ణ", "t" to "త",
            "th" to "థ", "d" to "ద", "dh" to "ధ", "n" to "న",
            "p" to "ప", "ph" to "ఫ", "b" to "బ", "bh" to "భ",
            "m" to "మ", "y" to "య", "r" to "ర", "l" to "ల",
            "v" to "వ", "sh" to "శ", "Sh" to "ష", "s" to "స",
            "h" to "హ", "L" to "ళ"
        )
        
        // Kannada
        val kannadaMap = mapOf(
            // Vowels
            "a" to "ಅ", "aa" to "ಆ", "i" to "ಇ", "ee" to "ಈ",
            "u" to "ಉ", "uu" to "ಊ", "e" to "ಏ", "ei" to "ಐ",
            "o" to "ಓ", "ou" to "ಔ",
            
            // Consonants
            "k" to "ಕ", "kh" to "ಖ", "g" to "ಗ", "gh" to "ಘ",
            "nga" to "ಙ", "c" to "ಚ", "ch" to "ಛ", "j" to "ಜ",
            "jh" to "ಝ", "yn" to "ಞ", "T" to "ಟ", "Th" to "ಠ",
            "D" to "ಡ", "Dh" to "ಢ", "N" to "ಣ", "t" to "ತ",
            "th" to "ಥ", "d" to "ದ", "dh" to "ಧ", "n" to "ನ",
            "p" to "ಪ", "ph" to "ಫ", "b" to "ಬ", "bh" to "ಭ",
            "m" to "ಮ", "y" to "ಯ", "r" to "ರ", "l" to "ಲ",
            "v" to "ವ", "sh" to "ಶ", "Sh" to "ಷ", "s" to "ಸ",
            "h" to "ಹ", "L" to "ಳ"
        )
        
        // Malayalam
        val malayalamMap = mapOf(
            // Vowels
            "a" to "അ", "aa" to "ആ", "i" to "ഇ", "ee" to "ഈ",
            "u" to "ഉ", "uu" to "ഊ", "e" to "ഏ", "ei" to "ഐ",
            "o" to "ഓ", "ou" to "ഔ",
            
            // Consonants
            "k" to "ക", "kh" to "ഖ", "g" to "ഗ", "gh" to "ഘ",
            "nga" to "ങ", "c" to "ച", "ch" to "ഛ", "j" to "ജ",
            "jh" to "ഝ", "yn" to "ഞ", "T" to "ട", "Th" to "ഠ",
            "D" to "ഡ", "Dh" to "ഢ", "N" to "ണ", "t" to "ത",
            "th" to "ഥ", "d" to "ദ", "dh" to "ധ", "n" to "ന",
            "p" to "പ", "ph" to "ഫ", "b" to "ബ", "bh" to "ഭ",
            "m" to "മ", "y" to "യ", "r" to "ര", "l" to "ല",
            "v" to "വ", "sh" to "ശ", "Sh" to "ഷ", "s" to "സ",
            "h" to "ഹ", "L" to "ള"
        )
        
        // Gujarati
        val gujaratiMap = mapOf(
            // Vowels
            "a" to "અ", "aa" to "આ", "i" to "ઇ", "ee" to "ઈ",
            "u" to "ઉ", "uu" to "ઊ", "e" to "એ", "ei" to "ઐ",
            "o" to "ઓ", "ou" to "ઔ",
            
            // Consonants
            "k" to "ક", "kh" to "ખ", "g" to "ગ", "gh" to "ઘ",
            "nga" to "ઙ", "c" to "ચ", "ch" to "છ", "j" to "જ",
            "jh" to "ઝ", "yn" to "ઞ", "T" to "ટ", "Th" to "ઠ",
            "D" to "ડ", "Dh" to "ઢ", "N" to "ણ", "t" to "ત",
            "th" to "થ", "d" to "દ", "dh" to "ધ", "n" to "ન",
            "p" to "પ", "ph" to "ફ", "b" to "બ", "bh" to "ભ",
            "m" to "મ", "y" to "ય", "r" to "ર", "l" to "લ",
            "v" to "વ", "sh" to "શ", "Sh" to "ષ", "s" to "સ",
            "h" to "હ", "L" to "ળ"
        )
        
        // Odia (Oriya)
        val odiaMap = mapOf(
            // Vowels
            "a" to "ଅ", "aa" to "ଆ", "i" to "ଇ", "ee" to "ଈ",
            "u" to "ଉ", "uu" to "ଊ", "e" to "ଏ", "ei" to "ଐ",
            "o" to "ଓ", "ou" to "ଔ",
            
            // Consonants
            "k" to "କ", "kh" to "ଖ", "g" to "ଗ", "gh" to "ଘ",
            "nga" to "ଙ", "c" to "ଚ", "ch" to "ଛ", "j" to "ଜ",
            "jh" to "ଝ", "yn" to "ଞ", "T" to "ଟ", "Th" to "ଠ",
            "D" to "ଡ", "Dh" to "ଢ", "N" to "ଣ", "t" to "ତ",
            "th" to "ଥ", "d" to "ଦ", "dh" to "ଧ", "n" to "ନ",
            "p" to "ପ", "ph" to "ଫ", "b" to "ବ", "bh" to "ଭ",
            "m" to "ମ", "y" to "ୟ", "r" to "ର", "l" to "ଲ",
            "v" to "ଵ", "sh" to "ଶ", "Sh" to "ଷ", "s" to "ସ",
            "h" to "ହ", "L" to "ଳ"
        )
        
        // Punjabi (Gurmukhi)
        val punjabiMap = mapOf(
            // Vowels
            "a" to "ਅ", "aa" to "ਆ", "i" to "ਇ", "ee" to "ਈ",
            "u" to "ਉ", "uu" to "ਊ", "e" to "ਏ", "ei" to "ਐ",
            "o" to "ਓ", "ou" to "ਔ",
            
            // Consonants
            "k" to "ਕ", "kh" to "ਖ", "g" to "ਗ", "gh" to "ਘ",
            "nga" to "ਙ", "c" to "ਚ", "ch" to "ਛ", "j" to "ਜ",
            "jh" to "ਝ", "yn" to "ਞ", "T" to "ਟ", "Th" to "ਠ",
            "D" to "ਡ", "Dh" to "ਢ", "N" to "ਣ", "t" to "ਤ",
            "th" to "ਥ", "d" to "ਦ", "dh" to "ਧ", "n" to "ਨ",
            "p" to "ਪ", "ph" to "ਫ", "b" to "ਬ", "bh" to "ਭ",
            "m" to "ਮ", "y" to "ਯ", "r" to "ਰ", "l" to "ਲ",
            "v" to "ਵ", "sh" to "ਸ਼", "Sh" to "ਸ਼", "s" to "ਸ",
            "h" to "ਹ", "L" to "ਲ"
        )
        
        // Assamese
        val assameseMap = mapOf(
            // Vowels
            "a" to "অ", "aa" to "আ", "i" to "ই", "ee" to "ঈ",
            "u" to "উ", "uu" to "ঊ", "e" to "এ", "ei" to "ঐ",
            "o" to "ও", "ou" to "ঔ",
            
            // Consonants
            "k" to "ক", "kh" to "খ", "g" to "গ", "gh" to "ঘ",
            "nga" to "ঙ", "c" to "চ", "ch" to "ছ", "j" to "জ",
            "jh" to "ঝ", "yn" to "ঞ", "T" to "ট", "Th" to "ঠ",
            "D" to "ড", "Dh" to "ঢ", "N" to "ণ", "t" to "ত",
            "th" to "থ", "d" to "দ", "dh" to "ধ", "n" to "ন",
            "p" to "প", "ph" to "ফ", "b" to "ব", "bh" to "ভ",
            "m" to "ম", "y" to "য", "r" to "ৰ", "l" to "ল",
            "v" to "ৱ", "sh" to "শ", "Sh" to "ষ", "s" to "স",
            "h" to "হ", "L" to "ল"
        )
        
        // Marathi (uses Devanagari with some variations)
        val marathiMap = devanagariMap.toMutableMap().apply {
            // Marathi specific variations can be added here
            put("L", "ळ")  // Marathi has specific character for L
        }
        
        // Sanskrit (uses Devanagari)
        val sanskritMap = devanagariMap
        
        // Add all language mappings
        defaultMappings["devanagari"] = devanagariMap
        defaultMappings["bengali"] = bengaliMap
        defaultMappings["tamil"] = tamilMap
        defaultMappings["telugu"] = teluguMap
        defaultMappings["kannada"] = kannadaMap
        defaultMappings["malayalam"] = malayalamMap
        defaultMappings["gujarati"] = gujaratiMap
        defaultMappings["odia"] = odiaMap
        defaultMappings["punjabi"] = punjabiMap
        defaultMappings["assamese"] = assameseMap
        defaultMappings["marathi"] = marathiMap
        defaultMappings["sanskrit"] = sanskritMap
        
        // Additional languages mentioned in your list
        defaultMappings["awadhi"] = devanagariMap  // Uses Devanagari
        defaultMappings["bhojpuri"] = devanagariMap  // Uses Devanagari
        defaultMappings["chhattisgarhi"] = devanagariMap  // Uses Devanagari
        defaultMappings["dogri"] = devanagariMap  // Uses Devanagari
        defaultMappings["harayanvi"] = devanagariMap  // Uses Devanagari
        defaultMappings["kashmiri"] = devanagariMap  // Uses Devanagari
        defaultMappings["konkani"] = devanagariMap  // Uses Devanagari
        defaultMappings["maithili"] = devanagariMap  // Uses Devanagari
        defaultMappings["manipuri"] = bengaliMap  // Uses Bengali script
        defaultMappings["nepali"] = devanagariMap  // Uses Devanagari
        defaultMappings["rajasthani"] = devanagariMap  // Uses Devanagari
        defaultMappings["sindhi"] = devanagariMap  // Uses Devanagari
        
        return defaultMappings
    }
    
    // Rest of the methods remain the same as previous version...
    private fun loadScriptMapping(script: String): Map<String, String> {
        return scriptMappings.getOrPut(script) {
            try {
                val inputStream: InputStream = assets.open("script-mappings/${script}.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val mappingData = Json.decodeFromString<ScriptMappingData>(jsonString)
                createFullMapping(mappingData)
            } catch (e: Exception) {
                // Return empty map if file doesn't exist
                emptyMap()
            }
        }
    }
    
    private fun loadWordLevelMappings(): Map<String, Map<String, String>> {
        return try {
            val inputStream: InputStream = assets.open("script-mappings/word-level-mappings.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            Json.decodeFromString<Map<String, Map<String, String>>>(jsonString)
        } catch (e: Exception) {
            // Return empty map if file doesn't exist, will be populated with common words
            mutableMapOf<String, Map<String, String>>().apply {
                // Initialize with empty maps for different conversion types
                put("roman_to_brahmi_words", createCommonWordMappings())
                put("brahmi_to_roman_words", createReverseWordMappings())
                // Language-specific word mappings will be added dynamically
            }
        }
    }
    
    private fun createFullMapping(data: ScriptMappingData): Map<String, String> {
        val fullMap = mutableMapOf<String, String>()
        
        // Add all mappings from the JSON data
        data.brahmi_mappings.vowels?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.consonants?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.vowel_marks?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.special_marks?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.numerals?.forEach { (key, value) -> fullMap[key] = value }
        
        return fullMap
    }
    
    // CHARACTER-LEVEL METHODS
    fun romanToIndianScript(romanText: String, targetScript: String): String {
        val mappings = loadRomanToIndianMappings()
        var result = StringBuilder()
        var i = 0
        
        while (i < romanText.length) {
            var matched = false
            
            // Check for combinations first (longest match)
            for (combination in brahmiCombinations) {
                if (i + combination.length <= romanText.length) {
                    val test = romanText.substring(i, i + combination.length).lowercase()
                    if (test == combination) {
                        val scriptMapping = mappings[targetScript]
                        val mapping = scriptMapping?.get(test) ?: test
                        result.append(mapping)
                        i += combination.length
                        matched = true
                        break
                    }
                }
            }
            
            // Single character mapping
            if (!matched) {
                val char = romanText[i].toString()
                val scriptMapping = mappings[targetScript]
                val mapping = scriptMapping?.get(char.lowercase()) ?: char
                result.append(mapping)
                i += 1
            }
        }
        
        return result.toString()
    }
    
    fun romanToBrahmiScript(romanText: String, targetScript: String): String {
        // First convert to Indian script, then to Brahmi
        val indianScript = romanToIndianScript(romanText, targetScript)
        return scriptToBrahmi(indianScript, targetScript)
    }
    
    fun scriptToBrahmi(scriptText: String, sourceScript: String): String {
        val mapping = loadScriptMapping(sourceScript)
        var result = StringBuilder()
        
        for (char in scriptText) {
            val brahmiChar = mapping[char.toString()] ?: char.toString()
            result.append(brahmiChar)
        }
        
        return result.toString()
    }
    
    fun brahmiToScript(brahmiText: String, targetScript: String): String {
        val mapping = loadScriptMapping(targetScript)
        val reverseMapping = mapping.entries.associate { (k, v) -> v to k }
        var result = StringBuilder()
        
        for (char in brahmiText) {
            val scriptChar = reverseMapping[char.toString()] ?: char.toString()
            result.append(scriptChar)
        }
        
        return result.toString()
    }
    
    // WORD-LEVEL METHODS
    fun romanToBrahmiWordLevel(romanWord: String, targetScript: String): String {
        val wordMappings = loadWordLevelMappings()
        val brahmiWordMapping = wordMappings["roman_to_brahmi_words"]
        
        return brahmiWordMapping?.get(romanWord.lowercase()) 
            ?: romanToBrahmiScript(romanWord, targetScript)
    }
    
    fun romanToIndianWordLevel(romanWord: String, targetScript: String): String {
        val wordMappings = loadWordLevelMappings()
        val indianWordMapping = wordMappings["roman_to_${targetScript}_words"]
        
        return indianWordMapping?.get(romanWord.lowercase())
            ?: romanToIndianScript(romanWord, targetScript)
    }
    
    fun brahmiToRomanWordLevel(brahmiWord: String, sourceScript: String): String {
        val wordMappings = loadWordLevelMappings()
        val romanWordMapping = wordMappings["brahmi_to_roman_words"]
        
        return romanWordMapping?.get(brahmiWord) ?: brahmiToRoman(brahmiWord)
    }
    
    // Complete Brahmi to Roman character mapping
    private fun brahmiToRoman(brahmiText: String): String {
        var result = StringBuilder()
        for (char in brahmiText) {
            val romanChar = brahmiToRomanMap[char.toString()] ?: char.toString()
            result.append(romanChar)
        }
        return result.toString()
    }
    
    // Common word mappings for better accuracy
    private fun createCommonWordMappings(): Map<String, String> {
        return mapOf(
            // Common greetings and words
            "namaste" to "𑀦𑀫𑀲𑁆𑀢𑁂",
            "hello" to "𑀳𑁂𑀮𑁄",
            "thank" to "𑀣𑀦𑁆𑀓",
            "you" to "𑀬𑁄𑀉",
            "yes" to "𑀬𑁂𑀲",
            "no" to "𑀦𑁄",
            "please" to "𑀧𑁆𑀮𑀷𑀲",
            
            // Common Indian words
            "bharat" to "𑀪𑀭𑀢",
            "india" to "𑀇𑀦𑁆𑀟𑀺𑀬",
            "hindi" to "𑀳𑀺𑀦𑁆𑀤𑀷",
            "sanskrit" to "𑀲𑀦𑁆𑀲𑁆𑀓𑁃𑀢",
            "tamil" to "𑀢𑀫𑀺𑀮",
            "telugu" to "𑀢𑁂𑀮𑀼𑀕𑀼",
            "bengali" to "𑀩𑁂𑀗𑁆𑀕𑀸𑀮𑀷",
            "kannada" to "𑀓𑀦𑁆𑀦𑀟",
            "malayalam" to "𑀫𑀮𑀬𑀸𑀮𑀫",
            "gujarati" to "𑀕𑀼𑀚𑀭𑀸𑀢𑀷",
            
            // Numbers
            "one" to "𑀅𑀓",
            "two" to "𑀤𑁄",
            "three" to "𑀢𑀺𑀦",
            "four" to "𑀘𑀸𑀭",
            "five" to "𑀧𑀸𑀦𑁆𑀘",
            "six" to "𑀱𑀸𑀱",
            "seven" to "𑀲𑀧𑁆𑀢",
            "eight" to "𑀅𑀱𑁆𑀝",
            "nine" to "𑀦𑀯",
            "ten" to "𑀤𑀲"
        )
    }
    
    private fun createReverseWordMappings(): Map<String, String> {
        val commonMappings = createCommonWordMappings()
        return commonMappings.entries.associate { (k, v) -> v to k }
    }
}

// Data classes remain the same...
@kotlinx.serialization.Serializable
data class ScriptMappingData(
    val script: String,
    val brahmi_mappings: BrahmiMappings
)

@kotlinx.serialization.Serializable
data class BrahmiMappings(
    val vowels: Map<String, String>? = null,
    val consonants: Map<String, String>? = null,
    val vowel_marks: Map<String, String>? = null,
    val special_marks: Map<String, String>? = null,
    val numerals: Map<String, String>? = null
)
