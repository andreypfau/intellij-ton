package org.ton.intellij.tolk.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.ton.intellij.tolk.psi.*

class TolkReference(
    element: TolkReferenceExpression,
    rangeInElement: TextRange,
) : PsiReferenceBase.Poly<TolkReferenceExpression>(element, rangeInElement, false) {
    val identifier: PsiElement get() = element.identifier

    private val resolver = ResolveCache.PolyVariantResolver<TolkReference> { t, incompleteCode ->
        if (!myElement.isValid) return@PolyVariantResolver ResolveResult.EMPTY_ARRAY

        val inference = element.inference
        val inferenceResolved = inference?.getResolvedRefs(element)
        if (!inferenceResolved.isNullOrEmpty()) {
            return@PolyVariantResolver inferenceResolved.toTypedArray()
        }
//        var name = identifier.text.let {
//            if (it.startsWith('.')) it.substring(1) else it
//        }.removeSurrounding("`")
//        if (name.startsWith("~") && name.length > 1) {
//            name = "~"+name.substring(1).removeSurrounding("`")
//        }
//        val file = element.containingFile as? TolkFile
//        if (file != null) {
//            val includes = file.collectIncludedFiles(true)
//
//            includes.forEach { includedFile ->
//                includedFile.constVars.forEach { constVar ->
//                    if (constVar.name?.removeSurrounding("`") == name) {
//                        return@PolyVariantResolver arrayOf(PsiElementResolveResult(constVar))
//                    }
//                }
//                includedFile.globalVars.forEach { globalVar ->
//                    if (globalVar.name?.removeSurrounding("`") == name) {
//                        return@PolyVariantResolver arrayOf(PsiElementResolveResult(globalVar))
//                    }
//                }
//                includedFile.functions.forEach { function ->
//                    val functionName = function.name?.removeSurrounding("`")
//                    if (functionName == name) {
//                        return@PolyVariantResolver arrayOf(PsiElementResolveResult(function))
//                    }
//                    if (name.startsWith("~") && functionName != null && !functionName.startsWith("~")) {
//                        val tensorList = (function.typeReference as? TolkTensorType)?.typeReferenceList
//                        if (tensorList?.size == 2 && functionName == name.substring(1)) {
//                            return@PolyVariantResolver arrayOf(PsiElementResolveResult(function))
//                        }
//                    }
//                }
//            }
//        }

        ResolveResult.EMPTY_ARRAY
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!myElement.isValid) return ResolveResult.EMPTY_ARRAY
        return ResolveCache.getInstance(myElement.project).resolveWithCaching(this, resolver, false, incompleteCode)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.identifier.replace(TolkPsiFactory[element.project].createIdentifier(newElementName))
    }
}
