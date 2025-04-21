package com.github.takemikami.intellij.plugin.pytestparametrize

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyDecorator
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyListLiteralExpression
import com.jetbrains.python.psi.PyParenthesizedExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.impl.PyKeywordArgumentImpl

@Suppress("UnstableApiUsage")
class PytestParametrizeInlayHintsCollector(
    editor: Editor,
    private val settings: PytestParametrizeInlayHintsProvider.Settings,
) : FactoryInlayHintsCollector(editor) {
    override fun collect(
        element: PsiElement,
        editor: Editor,
        sink: InlayHintsSink,
    ): Boolean {
        if (!element.isValid || element.project.isDefault || !element.containingFile.name.startsWith("test")) {
            return false
        }

        if (element is PyDecorator &&
            "pytest.mark.parametrize".equals(element.qualifiedName.toString()) &&
            element.hasArgumentList() &&
            element.arguments.size >= 2
        ) {
            val names = element.arguments[0]
            val valList = element.arguments[1]

            if (valList !is PyListLiteralExpression) {
                return true
            }
            val nameKeys =
                when (names) {
                    is PyStringLiteralExpression,
                    -> names.stringValue.split(",").map { it.trim() }
                    is PyListLiteralExpression,
                    -> names.children.map { it.text.removeSurrounding("\"").removeSurrounding("\'") }
                    is PyParenthesizedExpression,
                    ->
                        names.children.joinToString { it.text }.split(",")
                            .map { it.trim().removeSurrounding("\"").removeSurrounding("\'") }
                    else -> null
                } ?: return true
            val idsArguments = element.arguments.filter { it.name.equals("ids") }
            val ids =
                if (idsArguments.isEmpty()) {
                    listOf<String>()
                } else {
                    idsArguments.first()
                        .children.joinToString { it.children.joinToString { it.text } }
                        .split(",").map {
                            it.trim().removeSurrounding("\"").removeSurrounding("\'")
                        }.filter { it.isNotEmpty() }
                }

            val hintName: InlayPresentation = factory.seq()
            for ((idx, paramset) in valList.elements.withIndex()) {
                if (paramset !is PyParenthesizedExpression &&
                    !(paramset is PyCallExpression && "pytest.param" == paramset.firstChild.text)
                ) {
                    continue
                }

                // Sequence number of parameter set
                if (settings.showParametrizeOrderHints) {
                    val idxHintOfPytestParam =
                        if (paramset is PyCallExpression && "pytest.param" == paramset.firstChild.text) {
                            val texts =
                                paramset.lastChild.children
                                    .filter { it is PyKeywordArgumentImpl && "id" == it.firstChild.text }
                                    .map { it.lastChild.text }
                            if (texts.isNotEmpty()) {
                                texts.first().removeSurrounding("\"").removeSurrounding("\'")
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    val idxHint = idxHintOfPytestParam ?: if (idx < ids.size) ids[idx] else idx
                    sink.addInlineElement(
                        paramset.textOffset,
                        false,
                        factory.roundWithBackground(factory.seq(factory.smallText("$idxHint:"), hintName)),
                        false,
                    )
                }

                if (!settings.showParametrizeNameHints) {
                    continue
                }
                val paramsetTuple =
                    when (paramset) {
                        is PyParenthesizedExpression,
                        -> paramset.children.first()
                        is PyCallExpression,
                        -> paramset.children.last()
                        else -> null
                    } ?: return true
                for ((pidx, param) in paramsetTuple.children.withIndex()) {
                    if (pidx >= nameKeys.size || param !is PyExpression) {
                        break
                    }

                    // parameter name
                    sink.addInlineElement(
                        param.textOffset,
                        false,
                        factory.roundWithBackground(factory.seq(factory.smallText("${nameKeys[pidx]}:"), hintName)),
                        false,
                    )
                }
            }
        }

        return true
    }
}
