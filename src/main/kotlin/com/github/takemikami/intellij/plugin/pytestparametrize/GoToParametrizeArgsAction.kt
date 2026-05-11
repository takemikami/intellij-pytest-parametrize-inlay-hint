package com.github.takemikami.intellij.plugin.pytestparametrize


import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.ScrollType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import com.jetbrains.python.psi.*


class GoToParametrizeArgsAction : AnAction() {
    override fun update(event: AnActionEvent) {
        val vf = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val active = "py".equals(vf.extension) && vf.name.startsWith("test")
        event.presentation.isEnabledAndVisible = active
    }

    class TestVisitor() : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is PyDecorator
                && "pytest.mark.parametrize".equals(element.qualifiedName.toString())
                && element.hasArgumentList()
                && element.arguments.size >= 2
            ) {
                val valList = element.arguments[1]
                if (valList !is PyListLiteralExpression) return

                // detect selected id
                val idsArguments = element.arguments.filter { it.name.equals("ids") }
                if (idsArguments.isEmpty()) return

                val targetIndex = idsArguments.first().children.first().children.map {
                    currentOffset >= it.startOffset && currentOffset <= it.endOffset
                }.indexOfFirst { it }
                if (targetIndex == -1) return

                // detect args offset
                val argsOffsets = valList.elements.map { it.startOffset }
                if (targetIndex + 1 > argsOffsets.size) return
                offset = argsOffsets[targetIndex]
            }
            super.visitElement(element)
        }

        var offset = -1
        var currentOffset = -1
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val caretModel: CaretModel = editor?.caretModel ?: return
        val logicalPosition = caretModel.logicalPosition

        // get offset to goto
        val psiFile = event.getData(PlatformDataKeys.PSI_FILE)
        val visitor: TestVisitor = TestVisitor()
        visitor.currentOffset = caretModel.offset
        psiFile?.accept(visitor)
        if (visitor.offset == -1) return

        // move caret
        logicalPosition.leanForward(true);
        caretModel.moveToOffset(visitor.offset)
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER_DOWN)
        editor.selectionModel.removeSelection()
    }
}
