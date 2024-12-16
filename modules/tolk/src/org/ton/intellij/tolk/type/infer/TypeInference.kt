package org.ton.intellij.tolk.type.infer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.tolk.diagnostics.TolkDiagnostic
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.TolkIncludeDefinitionMixin
import org.ton.intellij.tolk.psi.impl.resolveFile
import org.ton.intellij.tolk.type.TolkType
import org.ton.intellij.util.parentOfType
import org.ton.intellij.util.recursionGuard
import java.util.*

val TolkElement.inference: TolkInferenceResult?
    get() {
        val context: TolkInferenceContextOwner? =
            this as? TolkInferenceContextOwner ?: context?.parentOfType<TolkInferenceContextOwner>()
        return if (context != null) {
            inferTypesIn(context)
        } else {
            null
        }
    }

fun inferTypesIn(element: TolkInferenceContextOwner): TolkInferenceResult {
    val ctx = TolkInferenceContext(element.project, element)
    return recursionGuard(element, memoize = false) { ctx.infer(element) }
        ?: throw CyclicReferenceException(element)
}

class CyclicReferenceException(val element: TolkInferenceContextOwner) :
    IllegalStateException("Can't do inference on cyclic inference context owner: $element")

interface TolkInferenceData {
    val returnStatements: List<TolkReturnStatement>

    fun getResolvedRefs(element: TolkReferenceExpression): OrderedSet<PsiElementResolveResult>

    fun getType(element: TolkExpression): TolkType?

    fun getType(element: TolkVar): TolkType?
}

private val EMPTY_RESOLVED_SET = OrderedSet<PsiElementResolveResult>()

data class TolkInferenceResult(
    private val resolvedRefs: Map<TolkReferenceExpression, OrderedSet<PsiElementResolveResult>>,
    private val expressionTypes: Map<TolkExpression, TolkType>,
    private val varTypes: Map<TolkVar, TolkType>,
    override val returnStatements: List<TolkReturnStatement>,
) : TolkInferenceData {
    val timestamp = System.nanoTime()

    override fun getResolvedRefs(element: TolkReferenceExpression): OrderedSet<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }

    override fun getType(element: TolkExpression): TolkType? {
        return expressionTypes[element]
    }

    override fun getType(element: TolkVar): TolkType? {
        return varTypes[element]
    }

    companion object {
    }
}

class TolkInferenceContext(
    val project: Project,
    val owner: TolkInferenceContextOwner
) : TolkInferenceData {
    private val resolvedRefs = HashMap<TolkReferenceExpression, OrderedSet<PsiElementResolveResult>>()
    private val diagnostics = LinkedList<TolkDiagnostic>()
    private val varTypes = HashMap<TolkVar, TolkType>()
    private val expressionTypes = HashMap<TolkExpression, TolkType>()
    override val returnStatements = LinkedList<TolkReturnStatement>()

    override fun getResolvedRefs(element: TolkReferenceExpression): OrderedSet<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }

    fun setResolvedRefs(element: TolkReferenceExpression, refs: Collection<PsiElementResolveResult>) {
        resolvedRefs[element] = OrderedSet(refs)
    }

    override fun getType(element: TolkExpression): TolkType? {
        return when (element) {
            is TolkBinExpression -> element.type
            is TolkPrefixExpression -> element.type
            is TolkLiteralExpression -> element.type
            is TolkUnitExpression -> TolkType.Unit
            else -> expressionTypes[element]
        }
    }

    override fun getType(element: TolkVar): TolkType? {
        return varTypes[element]
    }

    fun <T : TolkType> setType(element: TolkVar, type: T?): T? {
        if (type != null) {
            varTypes[element] = type
        }
        return type
    }

    fun <T : TolkType> setType(element: TolkExpression, type: T?): T? {
        if (type != null) {
            expressionTypes[element] = type
        }
        return type
    }

    fun addReturnStatement(element: TolkReturnStatement) {
        returnStatements.add(element)
    }

    fun addDiagnostic(diagnostic: TolkDiagnostic) {
        if (diagnostic.element.containingFile.isPhysical) {
            diagnostics.add(diagnostic)
        }
    }

    fun <T : TolkTyFoldable<T>> resolveTypeVarsIfPossible(ty: T): T {
        TODO("Not yet implemented")
    }

    fun infer(element: TolkInferenceContextOwner): TolkInferenceResult {
        when (element) {
            is TolkFunction -> {
                val walker = TolkInferenceWalker(this)
                val tolkFile = element.containingFile as? TolkFile

                tolkFile?.let {
                    val commonStdlib =
                        TolkIncludeDefinitionMixin.resolveTolkImport(element.project, tolkFile, "@stdlib/common")
                    if (commonStdlib != null) {
                        val tolkCommonStdlib = commonStdlib.findPsiFile(element.project) as? TolkFile
                        if (tolkCommonStdlib != null && tolkFile != tolkCommonStdlib) {
                            walker.inferFile(tolkCommonStdlib, false)
                        }
                    }
                    walker.inferFile(tolkFile)
                }
                walker.inferFunction(element)
            }
        }

        return TolkInferenceResult(resolvedRefs, expressionTypes, varTypes, returnStatements)
    }
}

class TolkInferenceWalker(
    val ctx: TolkInferenceContext,
    val parent: TolkInferenceWalker? = null,
    val throwableElements: MutableList<TolkThrowStatement> = LinkedList(),
) {
    private val symbolDefinitions = HashMap<String, Symbol>()

    class Symbol(
        val element: PsiElement,
        val type: TolkType? = null,
    )

    fun inferFile(element: TolkFile, useIncludes: Boolean = true) {
        val project = element.project
        element.functions.forEach { function ->
            symbolDefinitions[function.name?.removeSurrounding("`") ?: return@forEach] = Symbol(
                function,
                // can't resolve type of recursion functions without a provided return type
                if (ctx.owner == function && function.typeExpression == null) null else function.type
            )
        }
        element.globalVars.forEach { globalVar ->
            symbolDefinitions[globalVar.name?.removeSurrounding("`") ?: return@forEach] = Symbol(globalVar)
        }
        element.constVars.forEach { constVar ->
            symbolDefinitions[constVar.name?.removeSurrounding("`") ?: return@forEach] = Symbol(constVar)
        }
        element.includeDefinitions.forEach {
            val resolvedFile = it.resolveFile(project)
            if (resolvedFile != null) {
                val resolvedTolkFile = resolvedFile.findPsiFile(element.project) as? TolkFile
                if (resolvedTolkFile != null) {
                    inferFile(resolvedTolkFile, false)
                }
            }
        }
    }

    fun inferFunction(element: TolkFunction) {
        element.typeParameterList?.typeParameterList?.forEach { typeParameter ->
            symbolDefinitions[typeParameter.name ?: return@forEach] = Symbol(typeParameter)
        }
        element.parameterList?.parameterList?.forEach { functionParameter ->
            symbolDefinitions[functionParameter.name?.removeSurrounding("`") ?: return@forEach] =
                Symbol(functionParameter)
        }
        element.functionBody?.blockStatement?.let {
            TolkInferenceWalker(ctx, this).infer(it)
        }
    }

    fun infer(element: TolkCatch, throwableElements: List<TolkThrowStatement>) {
        val blockWalker = TolkInferenceWalker(ctx, this, this.throwableElements)
        element.blockStatement?.let { blockStatement ->
            blockWalker.infer(blockStatement)
        }
    }

    fun infer(element: TolkVarDefinition, typeMap: MutableMap<TolkVar, TolkType>): TolkType? {
        return when (element) {
            is TolkVar -> {
                val type = element.typeExpression?.type ?: TolkType.HoleType()
                typeMap[element] = type
                type
            }

            is TolkVarTuple -> {
                val tuple = element.varDefinitionList.map {
                    infer(it, typeMap) ?: TolkType.HoleType()
                }
                TolkType.TypedTuple(tuple)
            }

            is TolkVarTensor -> {
                val tensor = element.varDefinitionList.map {
                    infer(it, typeMap) ?: TolkType.HoleType()
                }
                TolkType.create(tensor)
            }

            else -> null
        }
    }

    private fun infer(element: TolkBlockStatement) {
        element.statementList.forEach { statement ->
            infer(statement)
        }
    }

    private fun infer(element: TolkStatement) {
        when (element) {
            is TolkReturnStatement -> infer(element)
            is TolkBlockStatement -> TolkInferenceWalker(ctx, this, throwableElements).infer(element)
            is TolkRepeatStatement -> infer(element)
            is TolkIfStatement -> infer(element)
            is TolkDoStatement -> infer(element)
            is TolkWhileStatement -> infer(element)
            is TolkAssertStatement -> infer(element)
            is TolkThrowStatement -> infer(element)
            is TolkTryStatement -> infer(element)
            is TolkVarStatement -> infer(element)
            is TolkExpressionStatement -> infer(element)
            else -> {}
        }
    }

    private fun infer(element: TolkReturnStatement) {
        element.expression?.let { expression ->
            infer(expression)
        }
        ctx.addReturnStatement(element)
    }

    private fun infer(element: TolkRepeatStatement) {
        element.expression?.let { expression ->
            infer(expression)
        }
        element.blockStatement?.let { blockStatement ->
            TolkInferenceWalker(ctx, this, throwableElements).infer(blockStatement)
        }
    }

    private fun infer(element: TolkIfStatement) {
        element.condition?.let { condition ->
            infer(condition)
        }
        element.blockStatement?.let { blockStatement ->
            TolkInferenceWalker(ctx, this, throwableElements).infer(blockStatement)
        }
        element.elseBranch?.statement?.let { statement ->
            infer(statement)
        }
    }

    private fun infer(element: TolkDoStatement) {
        element.blockStatement?.let { blockStatement ->
            val walker = TolkInferenceWalker(ctx, this, throwableElements)
            walker.infer(blockStatement)
            element.expression?.let { expression ->
                walker.infer(expression)
            }
        }
    }

    private fun infer(element: TolkWhileStatement) {
        element.condition?.let { condition ->
            infer(condition)
        }
        element.blockStatement?.let { blockStatement ->
            TolkInferenceWalker(ctx, this, throwableElements).infer(blockStatement)
        }
    }

    private fun infer(element: TolkAssertStatement) {
        element.assertCondition?.expression?.let { expression ->
            infer(expression)
        }
        element.assertExcNo?.expression?.let { excNo ->
            infer(excNo)
        }
        element.throwStatement?.let { expression ->
            infer(expression)
        }
    }


    private fun infer(element: TolkThrowStatement) {
        throwableElements.add(element)
        element.expressionList.forEachIndexed { index, expression ->
            infer(expression, if (index == 0) TolkType.Int else null)
        }
    }

    private fun infer(element: TolkTryStatement) {
        val blockWalker = TolkInferenceWalker(ctx, this)
        element.blockStatement?.let { blockStatement ->
            blockWalker.infer(blockStatement)
        }
        element.catch?.let { catch ->
            infer(catch, blockWalker.throwableElements)
        }
    }

    fun unify(
        t1: TolkType,
        t2: TolkType,
    ): TolkType? {
        if (t1 == t2) return t1
        if (t1 is TolkType.Tensor && t2 is TolkType.Tensor) {
            val (tt1, tt22) = if (t1.elements.size <= t2.elements.size) t1 to t2 else t2 to t1
            tt1.elements.zip(tt22.elements).forEach { (e1, e2) ->
                if (unify(e1, e2) == null) return null
            }
            return tt1
        }
        if (t1 is TolkType.TypedTuple && t2 is TolkType.TypedTuple) {
            val (tt1, tt22) = if (t1.elements.size <= t2.elements.size) t1 to t2 else t2 to t1
            tt1.elements.zip(tt22.elements).forEach { (e1, e2) ->
                if (unify(e1, e2) == null) return null
            }
            return tt1
        }
        return null
    }

    private fun infer(element: TolkVarStatement) {
        val typeMap = HashMap<TolkVar, TolkType>()
        val varType = element.varDefinition?.let { definition ->
            infer(definition, typeMap)
        }
        val expressionType = element.expression?.let { expression ->
            infer(expression, if (varType !is TolkType.HoleType) varType else null)
        }

        fun unify(
            element: TolkVarDefinition,
            tolkType: TolkType
        ) {
            when (element) {
                is TolkVar -> {
                    typeMap[element] = tolkType
                }

                is TolkVarTuple -> {
                    if (varType is TolkType.TypedTuple) {
                        element.varDefinitionList.zip(varType.elements).forEach { (e1, e2) ->
                            unify(e1, e2)
                        }
                    } else {
                        // mismatch
                    }
                }

                is TolkVarTensor -> {
                    if (varType is TolkType.Tensor) {
                        element.varDefinitionList.zip(varType.elements).forEach { (e1, e2) ->
                            unify(e1, e2)
                        }
                    } else {
                        // mismatch
                    }
                }
            }
        }
        if (expressionType != null) {
            element.varDefinition?.let { definition ->
                unify(definition, expressionType)
            }
        }
        typeMap.forEach { (definition, type) ->
            val name = definition.name?.removeSurrounding("`") ?: return@forEach
            symbolDefinitions[name] = Symbol(definition, type)
            ctx.setType(definition, type)
//            println("set $name = $type")
        }

//        println("lhs: $varType")
//        println("rhs: $expressionType")
    }

    private fun infer(
        element: TolkExpressionStatement
    ) {
        infer(element.expression)
    }

    private fun infer(element: TolkExpression, expectedType: TolkType? = null): TolkType? {
        return when (element) {
            is TolkBinExpression -> infer(element)
            is TolkTernaryExpression -> infer(element, expectedType)
            is TolkPrefixExpression -> infer(element)
            is TolkDotExpression -> infer(element, expectedType)
            is TolkCallExpression -> infer(element, expectedType)
            is TolkTupleExpression -> infer(element)
            is TolkParenExpression -> infer(element, expectedType)
            is TolkTensorExpression -> infer(element, expectedType)
            is TolkReferenceExpression -> infer(element)
            is TolkLiteralExpression -> element.type
            is TolkUnitExpression -> TolkType.Unit
            else -> expectedType
        }
    }

    private fun infer(element: TolkBinExpression): TolkType? {
        val type = element.type
        element.right?.let { expression ->
            infer(expression, type)
        }
        infer(element.left, type)
        return type
    }

    private fun infer(element: TolkTernaryExpression, expectedType: TolkType?): TolkType? {
        infer(element.condition, TolkType.Bool)
        val thenType = element.thenBranch?.let { branch ->
            infer(branch, expectedType)
        }
        val elseType = element.elseBranch?.let { branch ->
            infer(branch, expectedType)
        }
        return thenType ?: elseType ?: expectedType
    }

    private fun infer(element: TolkPrefixExpression): TolkType? {
        val type = element.type
        element.expression?.let { expression ->
            infer(expression, type)
        }
        return type
    }

    private fun infer(
        element: TolkDotExpression,
        expectedType: TolkType? = null,
    ): TolkType? {
        infer(element.left)
        val type = element.right?.let { expression ->
            if (expression is TolkCallExpression) {
                infer(expression, withFirstArg = element.left)
            } else {
                infer(expression)
            }
        }
        return ctx.setType(element, type ?: expectedType)
    }

    private fun infer(
        element: TolkCallExpression,
        expectedType: TolkType? = null,
        withFirstArg: TolkExpression? = null
    ): TolkType? {
//        println("start infer ${element.text} expected: $expectedType")
        val expression = element.expression
        infer(expression)
        val functionType = ctx.getType(expression) as? TolkType.Function
        val parameterTypes = functionType?.inputType?.let { param ->
            when (param) {
                is TolkType.Tensor -> param.elements
                TolkType.Unit -> emptyList()
                else -> listOf(param)
            }
        }
//        println("function type: $functionType")
        val arguments = ArrayList<TolkExpression>()
        if (withFirstArg != null) {
            arguments.add(withFirstArg)
        }
        element.argumentList.argumentList.forEach {
            arguments.add(it.expression)
        }
        // can't resolve expected types, just infer without context
        if (parameterTypes == null || parameterTypes.size != arguments.size) {
//            println("params(${parameterTypes?.size}) != args(${arguments.size})")
            arguments.forEach {
                infer(it)
            }
            return ctx.setType(element, expectedType ?: functionType?.returnType)
        }

        // we can provide expected types for generic resolve
        val typeMapping = HashMap<String, TolkType>()
        parameterTypes.zip(arguments).forEach { (paramType, arg) ->
            val argType = infer(arg, expectedType = paramType)
            if (paramType is TolkType.ParameterType && argType != null && argType !is TolkType.ParameterType) {
                typeMapping[paramType.name] = argType
            }
        }

        val returnType = functionType.returnType
        val result = when (returnType) {
            is TolkType.ParameterType -> typeMapping[returnType.name] ?: expectedType ?: returnType
            else -> returnType
        }
        ctx.setType(element, result)
//        println("end infer ${element.text} = result")
        return result
    }

    private fun infer(element: TolkParenExpression, expectedType: TolkType? = null): TolkType? {
        return ctx.setType(element, element.expression?.let { expression ->
            infer(expression, expectedType)
        } ?: expectedType)
    }

    private fun infer(element: TolkTensorExpression, expectedType: TolkType? = null): TolkType? {
        val expectedTypes = (expectedType as? TolkType.Tensor)?.elements
        val actualTypes = element.expressionList.mapIndexedNotNull { index, expression ->
            val expectedType = expectedTypes?.getOrNull(index)
            if (expectedType is TolkType.HoleType) {
                infer(expression)
            } else {
                infer(expression, expectedType)
            }
        }

        return ctx.setType(
            element, if (expectedTypes == null || expectedTypes.size == actualTypes.size) {
                TolkType.Tensor(actualTypes)
            } else {
                expectedType
            }
        )
    }

    private fun infer(element: TolkTupleExpression, expectedType: TolkType? = null): TolkType? {
        val expectedTypes = (expectedType as? TolkType.TypedTuple)?.elements
        val expressions = element.expressionList
        val actualTypes = expressions.mapIndexedNotNull { index, expression ->
            val expectedType = expectedTypes?.getOrNull(index)
            if (expectedType is TolkType.HoleType) {
                infer(expression)
            } else {
                infer(expression, expectedType)
            }
        }
        return ctx.setType(
            element,
            if (expectedTypes == null || expectedTypes.size == actualTypes.size) {
                TolkType.TypedTuple(actualTypes)
            } else {
                expectedType
            }
        )
    }

    private fun infer(element: TolkReferenceExpression): TolkType? {
        val name = element.name?.removeSurrounding("`") ?: return null
        val found = resolveSymbol(name) ?: return null
        ctx.setResolvedRefs(element, listOf(PsiElementResolveResult(found.element)))
        var type = found.type
        if (type == null) {
            type = (found.element as? TolkTypedElement)?.type ?: return null
        }
        if (found.element is TolkVar) {
            ctx.setType(found.element, type)
        }
        return ctx.setType(element, type)
    }

    private fun resolveSymbol(name: String): Symbol? {
        var scope: TolkInferenceWalker? = this
        var found: Symbol? = null
        while (found == null && scope != null) {
            found = scope.symbolDefinitions[name]
            scope = scope.parent
        }
        return found
    }
}
