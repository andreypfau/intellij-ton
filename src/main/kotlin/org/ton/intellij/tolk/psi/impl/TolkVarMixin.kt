package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.psi.TolkVar

abstract class TolkVarMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkVar {
    override fun getName(): String = identifier.text

    override fun setName(name: String): PsiElement {
        identifier.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getTextOffset(): Int = identifier.textOffset
}
