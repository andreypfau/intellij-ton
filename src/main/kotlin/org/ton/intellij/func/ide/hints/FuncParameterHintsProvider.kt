package org.ton.intellij.func.ide.hints

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.*

@Suppress("UnstableApiUsage")
class FuncParameterHintsProvider : InlayParameterHintsProvider {
    override fun getDefaultBlackList() = emptySet<String>()

    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        val applyExpression = element as? FuncApplyExpression ?: return emptyList()
        val referenceExpression = applyExpression.left as? FuncReferenceExpression ?: return emptyList()
        val name = referenceExpression.name ?: return emptyList()
        val arguments = when (val right = applyExpression.right) {
            is FuncTensorExpression -> right.expressionList
            is FuncParenExpression -> listOf(right.expression)
            else -> return emptyList()
        }
        if (arguments.isEmpty()) return emptyList()

        val function = referenceExpression.reference?.resolve() as? FuncFunction ?: return emptyList()
        val isSpecialCall = name.startsWith('.') || name.startsWith('~')
        val offset = if (isSpecialCall) 1 else 0

        val params = function.functionParameterList
        val result = ArrayList<InlayInfo>()
        arguments.forEachIndexed { index, arg ->
            val param = params.getOrNull(index + offset) ?: return result
            val paramName = param.name
            if (paramName != null && !(arg is FuncReferenceExpression && arg.name == paramName)) {
                result.add(InlayInfo(paramName, arg.textOffset))
            }
        }

        return result
    }
}
