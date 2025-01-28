package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkNullableType
import org.ton.intellij.tolk.type.TolkType

abstract class TolkNullableTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkNullableType {
    override val type: TolkType? get() {
        return TolkType.nullable(typeExpression.type ?: return null)
    }
}