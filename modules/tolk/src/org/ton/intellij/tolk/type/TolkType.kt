package org.ton.intellij.tolk.type


import org.ton.intellij.tolk.psi.TolkTypeParameter

sealed interface TolkType {

    fun substitute(substitution: Map<TolkTypeParameter, TolkType>): TolkType {
        return when (this) {
            is Function -> Function(inputType.substitute(substitution), returnType.substitute(substitution))
            is Tensor -> Tensor(elements.map { it.substitute(substitution) })
            is TypedTuple -> TypedTuple(elements.map { it.substitute(substitution) })
            is UnionType -> UnionType(elements.map { it.substitute(substitution) })
            is ParameterType -> substitution[this.psiElement] ?: this
            else -> this
        }
    }

    class ParameterType(
        val psiElement: TolkTypeParameter
    ) : TolkNamedType {
        override val name: String
            get() = psiElement.name ?: ""

        override fun toString(): String = name
    }

    class Function(
        val inputType: TolkType,
        val returnType: TolkType
    ) : TolkType {

        override fun toString(): String = "$inputType -> $returnType"
    }

    class Tensor(
        val elements: List<TolkType>
    ) : TolkType {
        override fun toString(): String = "(${elements.joinToString()})"
    }

    class TypedTuple(
        val elements: List<TolkType>
    ) : TolkType {
        override fun toString(): String = "[${elements.joinToString()}]"
    }

    object Unknown : TolkType {
        override fun toString(): String = "?"
    }

    class UnionType(
        val elements: List<TolkType>
    ) : TolkType {
        init {
            check(elements.size > 1) { "UnionType must have at least two elements, got ${elements.size}: $elements" }
        }
        val hasNull = elements.any { it == Null }
        val isNullable = elements.size == 2 && hasNull && !elements.all { it == Null }

        override fun toString(): String {
            return if (isNullable) {
                "${elements.first { it != Null }}?"
            } else {
                elements.joinToString(" | ")
            }
        }
    }

    companion object {
        val Int = TolkPrimitiveType.Int
        val Cell = TolkPrimitiveType.Cell
        val Builder = TolkPrimitiveType.Builder
        val Slice = TolkPrimitiveType.Slice
        val Continuation = TolkPrimitiveType.Continuation
        val Tuple = TolkPrimitiveType.Tuple
        val Unit = TolkPrimitiveType.Unit
        val Bool = TolkPrimitiveType.Bool
        val Null = TolkPrimitiveType.Null

        fun nullable(element: TolkType): TolkType {
            return if (element is UnionType) {
                if (element.hasNull) element
                else UnionType(element.elements + Null)
            } else if (element != Null) {
                UnionType(listOf(element, Null))
            } else {
                element
            }
        }

        fun create(vararg elements: TolkType): TolkType {
            return create(elements.toList())
        }

        fun create(elements: Collection<TolkType>): TolkType {
            return when {
                elements.isEmpty() -> Unit
                elements.size == 1 -> {
                    val element = elements.first()
                    if (element is Tensor) {
                        create(element.elements)
                    } else element
                }

                else -> Tensor(elements.toList())
            }
        }
    }
}

interface TolkNamedType : TolkType {
    val name: String
}

sealed class TolkPrimitiveType(
    override val name: String
) : TolkNamedType {
    object Int : TolkPrimitiveType("int")
    object Cell : TolkPrimitiveType("cell")
    object Builder : TolkPrimitiveType("builder")
    object Slice : TolkPrimitiveType("slice")
    object Continuation : TolkPrimitiveType("continuation")
    object Tuple : TolkPrimitiveType("tuple")
    object Unit : TolkPrimitiveType("()")
    object Bool : TolkPrimitiveType("bool")
    object Null : TolkPrimitiveType("null")

    override fun toString(): String = name
}



