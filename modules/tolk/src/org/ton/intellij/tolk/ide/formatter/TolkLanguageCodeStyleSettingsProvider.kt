package org.ton.intellij.tolk.ide.formatter

import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.ton.intellij.tolk.TolkLanguage

class TolkLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = TolkLanguage

    override fun getCodeSample(settingsType: SettingsType): String = ""

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions
    ) {
        commonSettings.RIGHT_MARGIN = 100

        commonSettings.LINE_COMMENT_AT_FIRST_COLUMN = false
        commonSettings.LINE_COMMENT_ADD_SPACE = true
        commonSettings.BLOCK_COMMENT_AT_FIRST_COLUMN = false
    }
}
