package com.goevent.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** The language the screens are showing right now: "en", "af" or "xh". MainActivity sets it. */
val LocalAppLanguage = compositionLocalOf { "en" }

/** Turns the saved language name into its code. */
fun languageCode(savedName: String): String = when (savedName) {
    "Afrikaans" -> "af"
    "isiXhosa" -> "xh"
    else -> "en"
}

/** Keeps the chosen language for code that is not inside a screen (toasts). */
object AppLanguage {
    @Volatile
    var code: String = "en"
}

/**
 * Translates text shown on screen. The English text is the key.
 * Use {0}, {1}... inside the text for values, for example tr("Only {0} left", 5).
 * If a translation is missing, the English text is shown.
 */
@Composable
fun tr(text: String, vararg args: Any): String = translate(LocalAppLanguage.current, text, *args)

/** Same as [tr], for code that is not composable (for example toast messages). */
fun trPlain(text: String, vararg args: Any): String = translate(AppLanguage.code, text, *args)

private fun translate(language: String, text: String, vararg args: Any): String {
    val table = when (language) {
        "af" -> Translations.afrikaans
        "xh" -> Translations.isiXhosa
        else -> null
    }
    var result = table?.get(text) ?: text
    args.forEachIndexed { index, value ->
        result = result.replace("{$index}", value.toString())
    }
    return result
}