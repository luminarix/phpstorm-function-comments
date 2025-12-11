package com.github.luminarix.phpstormfunctioncomments.actions

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class MultiLineCommentFunctionAction : CommentFunctionAction() {

    override fun commentFunction(project: Project, editor: Editor, function: PsiElement) {
        val document = editor.document
        val (startOffset, endOffset) = getFunctionTextRange(function, document)
        val functionText = document.getText(TextRange(startOffset, endOffset))

        if (functionText.contains("/*") || functionText.contains("*/")) {
            val startLine = document.getLineNumber(startOffset)
            val endLine = document.getLineNumber(endOffset)
            commentWithSingleLines(document, startLine, endLine)
        } else {
            commentFunctionWithBlock(document, startOffset, endOffset)
        }
    }

    private fun commentFunctionWithBlock(document: Document, startOffset: Int, endOffset: Int) {
        document.insertString(endOffset, " */")
        document.insertString(startOffset, "/* ")
    }

    private fun commentWithSingleLines(document: Document, startLine: Int, endLine: Int) {
        for (line in endLine downTo startLine) {
            val lineStartOffset = document.getLineStartOffset(line)
            document.insertString(lineStartOffset, "// ")
        }
    }
}
