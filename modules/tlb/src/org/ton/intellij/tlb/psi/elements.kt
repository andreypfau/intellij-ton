package org.ton.intellij.tlb.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

interface TlbElement : PsiElement
abstract class TlbElementImpl(node: ASTNode) : ASTWrapperPsiElement(node)

interface TlbNamedElement : TlbElement, PsiNameIdentifierOwner {
    val identifier: PsiElement?

    override fun getNameIdentifier(): PsiElement? = identifier
}


abstract class TlbNamedElementImpl(node: ASTNode) : TlbElementImpl(node), TlbNamedElement {
    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier?.replace(project.tlbPsiFactory.createIdentifier(name))
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}

interface TlbFieldListOwner : TlbElement {
    val fieldList: TlbFieldList?
}

//interface TlbNaturalTypeExpression : TlbTypeExpression {
//
//}

fun TlbTypeExpression.unwrap(): TlbTypeExpression? {
    var current: TlbTypeExpression? = this
    while (current is TlbParenTypeExpression) {
        current = current.typeExpression
    }
    return current
}

fun TlbTypeExpression.naturalValue(): Int? {
    return when(this) {
        is TlbIntTypeExpression -> naturalValue()
        is TlbParenTypeExpression -> naturalValue()
        else -> null
    }
}

fun TlbParenTypeExpression.naturalValue(): Int? {
    return unwrap()?.naturalValue()
}

fun TlbIntTypeExpression.naturalValue(): Int? {
    return number.text.toIntOrNull()
}