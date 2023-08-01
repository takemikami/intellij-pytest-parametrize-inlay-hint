package com.github.takemikami.intellij.plugin.pytestparametrize

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.*

@Suppress("UnstableApiUsage")
class PytestParametrizeInlayHintsCollector(
        editor: Editor,
        private val settings: PytestParametrizeInlayHintsProvider.Settings
) : FactoryInlayHintsCollector(editor) {

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        if (!element.isValid || element.project.isDefault || !element.containingFile.name.startsWith("test")) {
            return false
        }

        if (element is PyDecorator
                && "pytest.mark.parametrize".equals(element.qualifiedName.toString())
                && element.hasArgumentList()
                && element.arguments.size >= 2) {
            val names = element.arguments[0]
            val valList = element.arguments[1]

            if (valList !is PyListLiteralExpression) { return true }
            val nameKeys = when (names) {
                is PyStringLiteralExpression
                    -> names.stringValue.split(",").map{it.trim()}
                is PyListLiteralExpression
                    -> names.children.map{it.text.removeSurrounding("\"").removeSurrounding("\'")}
                is PyParenthesizedExpression
                    -> names.children.joinToString{it.text}.split(",")
                        .map{it.trim().removeSurrounding("\"").removeSurrounding("\'")}
                else -> null
            } ?: return true
            val ids = element.arguments.filter { it -> it.name.equals("ids") }.first()
                .children.joinToString{it.children.joinToString { it.text }}
                .split(",").map{
                    it.trim().removeSurrounding("\"").removeSurrounding("\'")
                }.filter{it -> it.isNotEmpty() }

            val hintName: InlayPresentation = factory.seq()
            for ((idx, paramset) in valList.elements.withIndex()) {
                if (paramset !is PyParenthesizedExpression) { continue }

                // Sequence number of parameter set
                if(settings.showParametrizeOrderHints) {
                    val idxHint = if (idx < ids.size) ids[idx] else idx
                    sink.addInlineElement(
                            paramset.textOffset,
                            false,
                            factory.roundWithBackground(factory.seq(factory.smallText("${idxHint}:"), hintName)),
                            false
                    )
                }

                if (!settings.showParametrizeNameHints) { continue }
                val paramsetTuple = paramset.children.first()
                for ((pidx, param) in paramsetTuple.children.withIndex()) {
                    if (pidx >= nameKeys.size || param !is PyExpression) { break }

                    // parameter name
                    sink.addInlineElement(
                            param.textOffset,
                            false,
                            factory.roundWithBackground(factory.seq(factory.smallText("${nameKeys[pidx]}:"), hintName)),
                            false
                    )
                }
            }
        }

        return true
    }
}
