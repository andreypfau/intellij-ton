package org.ton.intellij.tolk.refactor

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import org.ton.intellij.tolk.psi.TOLK_KEYWORDS
import org.ton.intellij.tolk.psi.TolkTokenType

class TolkNamesValidator : NamesValidator {
    private val KEYWORD_SET = TOLK_KEYWORDS.types.filterIsInstance<TolkTokenType>().map { it.name }.toSet()

    override fun isKeyword(name: String, project: Project?): Boolean {
        return KEYWORD_SET.contains(name)
    }

    override fun isIdentifier(name: String, project: Project?): Boolean {
        if (name.isBlank()) return false

        if (KEYWORD_SET.contains(name)) return false

        if (name.first() == '\"') return false
        if (name.first() == '`' && name.last() == '`') {
            return name.length >= 3
        }
        if (name.startsWith("{-")) return false
        if (name.startsWith("\"\"\"") && name.endsWith("\"\"\"")) return false
        for ((index, currentChar) in name.withIndex()) {
            if (currentChar.isWhitespace()) return false
            if (index == 0 && currentChar == '~') continue
            if (currentChar in BAN_CHARS) return false
        }
        return true
    }

    companion object {
        private const val BAN_CHARS = ";.().~"
    }
}
