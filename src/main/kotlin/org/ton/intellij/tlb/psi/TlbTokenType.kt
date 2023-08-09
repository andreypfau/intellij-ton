package org.ton.intellij.tlb.psi

import com.intellij.psi.tree.IElementType
import org.ton.intellij.tlb.TlbLanguage

class TlbTokenType(debugName: String) : IElementType(debugName, TlbLanguage) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TlbTokenType
        if (index != other.index) return false
        return true
    }

    override fun hashCode(): Int = index.toInt()
}
