@file:Suppress("NOTHING_TO_INLINE")

package org.ton.intellij.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil

fun loadTextResource(ctx: Class<*>, resource: String): String {
    val classLoader = requireNotNull(ctx.classLoader) { "Can't load class loader for $ctx" }
    val stream =
        requireNotNull(classLoader.getResourceAsStream(resource)) { "Can't find resource: `$resource` in context: $ctx" }
    return stream.use {
        it.readAllBytes().decodeToString()
    }
}

val Project.psiManager get() = PsiManager.getInstance(this)

inline fun <reified T : PsiElement> PsiElement.childOfType(strict: Boolean = true): T? =
    childOfType(T::class.java, strict)

fun <T : PsiElement> PsiElement.childOfType(type: Class<T>, strict: Boolean = true): T? =
    PsiTreeUtil.findChildOfType(this, type, strict)

inline fun <reified T : PsiElement> PsiElement.parentOfType(strict: Boolean = true, minStartOffset: Int = -1): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, strict, minStartOffset)

inline fun <reified T : PsiElement> PsiElement.collectElements() =
    PsiTreeUtil.collectElementsOfType(this, T::class.java)

inline fun TokenSet(vararg elements: IElementType) = TokenSet.create(*elements)

fun PsiElement.processElements(processor: (PsiElement) -> Boolean) = PsiTreeUtil.processElements(this, processor)

inline fun <T> nullIfError(action: () -> T): T? = try {
    action()
} catch (e: Throwable) {
    null
}
