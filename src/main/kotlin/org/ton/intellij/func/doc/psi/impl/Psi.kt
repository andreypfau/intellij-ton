package org.ton.intellij.func.doc.psi.impl

import com.intellij.psi.impl.source.tree.AstBufferUtil
import com.intellij.psi.impl.source.tree.CompositePsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.func.doc.psi.*
import org.ton.intellij.util.childOfType

abstract class FuncDocElementImpl(type: IElementType) : CompositePsiElement(type), FuncDocElement {
    protected open fun <T : Any> notNullChild(child: T?): T =
        child ?: error("$text parent=${parent.text}")

    override val containingDoc: FuncDocComment
        get() = PsiTreeUtil.getParentOfType(this, FuncDocComment::class.java, true)
            ?: error("FuncDocElement cannot leave outside of the doc comment! `${text}`")

    override val markdownValue: String
        get() = AstBufferUtil.getTextSkippingWhitespaceComments(this)


    override fun toString(): String = "${javaClass.simpleName}($elementType)"
}

class FuncDocGapImpl(type: IElementType, val text: CharSequence) : LeafPsiElement(type, text), FuncDocGap {
    override fun getTokenType(): IElementType = elementType
}

class FuncDocInlineLinkImpl(type: IElementType) : FuncDocElementImpl(type), FuncDocInlineLink {
    override val linkText: FuncDocLinkText
        get() = notNullChild(childOfType())

    override val linkDestination: FuncDocLinkDestination
        get() = notNullChild(childOfType())
}

class FuncDocLinkReferenceShortImpl(type: IElementType) : FuncDocElementImpl(type), FuncDocLinkReferenceShort {
    override val linkLabel: FuncDocLinkLabel
        get() = notNullChild(childOfType())
}

class FuncDocLinkReferenceFullImpl(type: IElementType) : FuncDocElementImpl(type), FuncDocLinkReferenceFull {
    override val linkText: FuncDocLinkText
        get() = notNullChild(childOfType())

    override val linkLabel: FuncDocLinkLabel
        get() = notNullChild(childOfType())
}

class FuncDocLinkDefinitionImpl(type: IElementType) : FuncDocElementImpl(type), FuncDocLinkDefinition {
    override val linkLabel: FuncDocLinkLabel
        get() = notNullChild(childOfType())

    override val linkDestination: FuncDocLinkDestination
        get() = notNullChild(childOfType())
}


class FuncDocLinkTextImpl(type: IElementType) : FuncDocElementImpl(type), FuncDocLinkText
class FuncDocLinkLabelImpl(type: IElementType) : FuncDocElementImpl(type), FuncDocLinkLabel
class FuncDocLinkTitleImpl(type: IElementType) : FuncDocElementImpl(type), FuncDocLinkTitle
class FuncDocLinkDestinationImpl(type: IElementType) : FuncDocElementImpl(type), FuncDocLinkDestination


class FuncDocCodeSpanImpl(type: IElementType) : FuncDocElementImpl(type), FuncDocCodeSpan
