package com.fffskintools.skin.tools.emotes.data.local

import com.fffskintools.skin.tools.emotes.R
import com.fffskintools.skin.tools.emotes.domain.model.Tongue

/** Languages the app is translated into; keep in sync with res/xml/locales_config.xml. */
val SUPPORTED_LANGUAGES = listOf(
    Tongue("en", "🇬🇧", "Aa", R.string.lang_en, R.string.lang_en),
    Tongue("hi", "🇮🇳", "अ", R.string.lang_hi, R.string.lang_hi_native),
    Tongue("id", "🇮🇩", "Id", R.string.lang_id, R.string.lang_id_native),
    Tongue("tr", "🇹🇷", "Tr", R.string.lang_tr, R.string.lang_tr_native),
    Tongue("vi", "🇻🇳", "Vi", R.string.lang_vi, R.string.lang_vi_native),
    Tongue("ar", "🇸🇦", "ع", R.string.lang_ar, R.string.lang_ar_native),
    Tongue("fr", "🇫🇷", "Fr", R.string.lang_fr, R.string.lang_fr_native),
    Tongue("de", "🇩🇪", "De", R.string.lang_de, R.string.lang_de_native),
    Tongue("ja", "🇯🇵", "日", R.string.lang_ja, R.string.lang_ja_native),
)
