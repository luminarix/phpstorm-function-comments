package com.github.luminarix.phpstormfunctioncomments.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.Method

abstract class CommentFunctionAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)

        val isEnabled = editor != null && psiFile != null && findEnclosingFunction(psiFile, editor) != null
        e.presentation.isEnabledAndVisible = isEnabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val project = e.project ?: return

        val function = findEnclosingFunction(psiFile, editor) ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            commentFunction(project, editor, function)
        }
    }

    protected abstract fun commentFunction(project: Project, editor: Editor, function: PsiElement)

    private fun findEnclosingFunction(psiFile: PsiFile, editor: Editor): PsiElement? {
        val offset = editor.caretModel.offset
        val elementAtCaret = psiFile.findElementAt(offset) ?: return null

        return PsiTreeUtil.getParentOfType(elementAtCaret, Method::class.java, Function::class.java)
    }

    protected fun getFunctionTextRange(function: PsiElement): Pair<Int, Int> {
        val startOffset = function.textRange.startOffset
        val endOffset = function.textRange.endOffset
        return Pair(startOffset, endOffset)
    }
}
