package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.ton.intellij.func.psi.FuncReferenceExpression
import org.ton.intellij.func.psi.FuncVisitor

class FuncUnresolvedReferenceInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitReferenceExpression(o: FuncReferenceExpression) {
            super.visitReferenceExpression(o)
            val reference = o.reference ?: return
            val resolved = reference.resolve()
            if (resolved != null) return

            val id = o.identifier
            val range = TextRange.from(id.startOffsetInParent, id.textLength)
            holder.registerProblem(
                o,
                "Unresolved reference <code>#ref</code>",
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                range
            )
        }
    }
}
