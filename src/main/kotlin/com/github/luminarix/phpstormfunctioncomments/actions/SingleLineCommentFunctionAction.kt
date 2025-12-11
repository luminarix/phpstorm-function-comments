package com.github.luminarix.phpstormfunctioncomments.actions

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class SingleLineCommentFunctionAction : CommentFunctionAction() {

    override fun commentFunction(project: Project, editor: Editor, function: PsiElement) {
        val document = editor.document
        val (startOffset, endOffset) = getFunctionTextRange(function, document)

        val startLine = document.getLineNumber(startOffset)
        val endLine = document.getLineNumber(endOffset)

        commentLines(document, startLine, endLine)
    }

    private fun commentLines(document: Document, startLine: Int, endLine: Int) {
        for (line in endLine downTo startLine) {
            val lineStartOffset = document.getLineStartOffset(line)
            document.insertString(lineStartOffset, "// ")
        }
    }
}
