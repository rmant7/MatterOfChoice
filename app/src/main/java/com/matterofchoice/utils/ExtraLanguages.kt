package com.matterofchoice.utils

data class LanguageDefinition(val displayName: String, val code: String)

val extraLanguages = listOf(
    LanguageDefinition("Acehnese", "ace"),
    LanguageDefinition("Mesopotamian Arabic", "acm"), // Or "ar-IQ" for a broader region
    LanguageDefinition("Ta'izzi-Adeni Arabic", "arz-YE"), // This is more specific, "arz" is Egyptian Arabic usually
    LanguageDefinition("Tunisian Arabic", "aeb"), // Or "ar-TN"
    LanguageDefinition("South Levantine Arabic", "ajp"), // Or "ar-JO", "ar-PS"
    LanguageDefinition("North Levantine Arabic", "apc"), // Or "ar-SY", "ar-LB"
    LanguageDefinition("Najdi Arabic", "ars"), // Or "ar-SA" (though SA has many dialects)
    LanguageDefinition("Moroccan Arabic", "ary"), // Or "ar-MA"
    LanguageDefinition("Egyptian Arabic", "arz"), // Or "ar-EG"
    LanguageDefinition("Awadhi", "awa"),
    LanguageDefinition("Aymara", "aym"), // Or "ay"
    LanguageDefinition("South Azerbaijani", "azb"), // Or "az-IR"
    LanguageDefinition("Banjar", "bjn"),
    LanguageDefinition("Tibetan", "bod"), // Or "bo"
    LanguageDefinition("Chokwe", "cjk"),
    LanguageDefinition("Central Kurdish (Sorani)", "ckb"),
    LanguageDefinition("Crimean Tatar", "crh"),
    LanguageDefinition("Southwestern Dinka", "dik"), // Dinka has many variants, this is one code
    LanguageDefinition("Dyula", "dyu"),
    LanguageDefinition("Dzongkha", "dzo"), // Or "dz"
    LanguageDefinition("Nigerian Fulfulde", "fuv"), // Fulfulde has many variants (e.g., "ff")
    LanguageDefinition("West Central Oromo", "gaz"), // Oromo has variants (e.g., "om")
    LanguageDefinition("Chhattisgarhi", "hne"),
    LanguageDefinition("Ilocano", "ilo"),
    LanguageDefinition("Javanese", "jav"), // Or "jv"
    LanguageDefinition("Kabyle", "kab"),
    LanguageDefinition("Jingpho (Kachin)", "kac"),
    LanguageDefinition("Kamba", "kam"),
    LanguageDefinition("Kashmiri", "kas"), // Or "ks"
    LanguageDefinition("Georgian", "kat"), // Or "ka"
    LanguageDefinition("Kanuri", "kau"), // Or "kr"
    LanguageDefinition("Kabiyè", "kbp"),
    LanguageDefinition("Kabuverdianu (Cape Verde Creole)", "kea"),
    LanguageDefinition("Kimbundu", "kmb"),
    LanguageDefinition("Kongo", "kon"), // Or "kg"
    LanguageDefinition("Northern Kurdish (Kurmanji)", "kmr"),
    LanguageDefinition("Standard Latvian", "lvs"), // Often just "lv" is used for general Latvian
    LanguageDefinition("Ligurian", "lij"),
    LanguageDefinition("Limburgish", "lim"), // Or "li"
    LanguageDefinition("Lingala", "lin"), // Or "ln"
    LanguageDefinition("Lithuanian", "lit"), // Or "lt"
    LanguageDefinition("Lombard", "lmo"),
    LanguageDefinition("Latgalian", "ltg"),
    LanguageDefinition("Luba-Lulua", "lua"),
    LanguageDefinition("Ganda", "lug"), // Or "lg"
    LanguageDefinition("Luo", "luo"), // (Dholuo)
    LanguageDefinition("Mizo (Lushai)", "lus"),
    LanguageDefinition("Magahi", "mag"),
    LanguageDefinition("Maithili", "mai"),
    LanguageDefinition("Minangkabau", "min"),
    LanguageDefinition("Plateau Malagasy", "plt"), // Malagasy has variants (e.g., "mg")
    LanguageDefinition("Manipuri (Meitei)", "mni"),
    LanguageDefinition("Halh Mongolian", "khk"), // Or just "mn" for Mongolian
    LanguageDefinition("Mooré", "mos"),
    LanguageDefinition("Nepali (India)", "npi"), // Standard Nepali is "ne"
    LanguageDefinition("Northern Sotho", "nso"),
    LanguageDefinition("Nuer", "nus"),
    LanguageDefinition("Nyanja (Chichewa)", "nya"), // Or "ny"
    LanguageDefinition("Pangasinan", "pag"),
    LanguageDefinition("Papiamento", "pap"),
    LanguageDefinition("Dari Persian", "prs"), // Or "fa-AF"
    LanguageDefinition("Southern Pashto", "pbt"), // Pashto is "ps"
    LanguageDefinition("Ayacucho Quechua", "quy"), // Quechua has many variants (e.g., "qu")
    LanguageDefinition("Rundi (Kirundi)", "run"), // Or "rn"
    LanguageDefinition("Sango", "sag"), // Or "sg"
    LanguageDefinition("Santali", "sat"),
    LanguageDefinition("Sicilian", "scn"),
    LanguageDefinition("Shan", "shn"), // Or "sh"
    LanguageDefinition("Tosk Albanian", "als"), // Standard Albanian is "sq"
    LanguageDefinition("Swati", "ssw"), // Or "ss"
    LanguageDefinition("Sundanese", "sun"), // Or "su"
    LanguageDefinition("Silesian", "szl"),
    LanguageDefinition("Tamasheq (Latin)", "tmh-Latn"), // Or just "tmh" if script isn't critical for your fallback
    LanguageDefinition("Tamasheq (Tifinagh)", "tmh-Tfng"),
    LanguageDefinition("Tok Pisin", "tpi"),
    LanguageDefinition("Tumbuka", "tum"),
    LanguageDefinition("Twi", "twi"), // Or "ak" (Akan, which Twi is a dialect of)
    LanguageDefinition("Central Atlas Tamazight", "tzm"),
    LanguageDefinition("Umbundu", "umb"),
    LanguageDefinition("Venetian", "vec"),
    LanguageDefinition("Waray", "war"), // (Waray-Waray)
    LanguageDefinition("Wolof", "wol"), // Or "wo"
    LanguageDefinition("Eastern Yiddish", "ydd"), // General Yiddish is "yi"
    LanguageDefinition("Cantonese", "yue") // Or "zh-HK", "zh-YUE"
)
