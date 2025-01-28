package org.ton.intellij.tolk.type

import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder
import org.ton.intellij.util.printPsi

fun PresentationTreeBuilder.printTolkType(type: TolkType) {
    when (type) {
        is TolkType.ParameterType -> {
            printPsi(type.psiElement, type.name)
        }

        is TolkNamedType -> text(type.name)
        is TolkType.Function -> {
            printTolkType(type.inputType)
            text(" -> ")
            printTolkType(type.returnType)
        }

        is TolkType.Unknown -> {
            text("?")
        }

        is TolkType.Tensor -> {
            text("(")
            val iterator = type.elements.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                printTolkType(element)
                if (iterator.hasNext()) {
                    text(", ")
                }
            }
            text(")")
        }

        is TolkType.TypedTuple -> {
            text("[")
            val iterator = type.elements.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                printTolkType(element)
                if (iterator.hasNext()) {
                    text(", ")
                }
            }
            text("]")
        }

        is TolkType.UnionType -> {
            if (type.isNullable) {
                printTolkType(type.elements.first { it != TolkType.Null })
                text("?")
                return
            }
            val iterator = type.elements.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                printTolkType(element)
                if (iterator.hasNext()) {
                    text(" | ")
                }
            }
        }
    }
}