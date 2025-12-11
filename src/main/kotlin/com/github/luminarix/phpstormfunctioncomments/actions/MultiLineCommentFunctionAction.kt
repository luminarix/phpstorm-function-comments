package com.github.luminarix.phpstormfunctioncomments.actions

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class MultiLineCommentFunctionAction : CommentFunctionAction() {

    override fun commentFunction(project: Project, editor: Editor, function: PsiElement) {
        val document = editor.document
        val (startOffset, endOffset) = getFunctionTextRange(function)

        val functionText = document.getText(TextRange(startOffset, endOffset))

        if (isAlreadyCommented(functionText)) {
            uncommentFunction(document, startOffset, endOffset, functionText)
        } else {
            commentFunctionWithBlock(document, startOffset, endOffset)
        }
    }

    private fun isAlreadyCommented(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("/*") && trimmed.endsWith("*/")
    }

    private fun commentFunctionWithBlock(
        document: Document,
        startOffset: Int,
        endOffset: Int
    ) {
        document.insertString(endOffset, " */")
        document.insertString(startOffset, "/* ")
    }

    private fun uncommentFunction(
        document: Document,
        startOffset: Int,
        endOffset: Int,
        functionText: String
    ) {
        val openIndex = functionText.indexOf("/*")
        val closeIndex = functionText.lastIndexOf("*/")

        if (openIndex == -1 || closeIndex == -1) return

        val openEnd = if (functionText.length > openIndex + 2 && functionText[openIndex + 2] == ' ') {
            openIndex + 3
        } else {
            openIndex + 2
        }

        val closeStart = if (closeIndex > 0 && functionText[closeIndex - 1] == ' ') {
            closeIndex - 1
        } else {
            closeIndex
        }

        document.deleteString(startOffset + closeStart, startOffset + closeIndex + 2)
        document.deleteString(startOffset + openIndex, startOffset + openEnd)
    }
}
