package com.github.luminarix.phpstormfunctioncomments.actions

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class SingleLineCommentFunctionAction : CommentFunctionAction() {

    override fun commentFunction(project: Project, editor: Editor, function: PsiElement) {
        val document = editor.document
        val (startOffset, endOffset) = getFunctionTextRange(function)

        val startLine = document.getLineNumber(startOffset)
        val endLine = document.getLineNumber(endOffset)

        val functionText = document.getText(TextRange(startOffset, endOffset))
        val lines = functionText.lines()

        if (isAlreadyCommented(lines)) {
            uncommentLines(document, startLine, endLine)
        } else {
            commentLines(document, startLine, endLine)
        }
    }

    private fun isAlreadyCommented(lines: List<String>): Boolean {
        return lines.all { line ->
            val trimmed = line.trim()
            trimmed.isEmpty() || trimmed.startsWith("//")
        }
    }

    private fun commentLines(document: Document, startLine: Int, endLine: Int) {
        for (line in endLine downTo startLine) {
            val lineStartOffset = document.getLineStartOffset(line)
            document.insertString(lineStartOffset, "// ")
        }
    }

    private fun uncommentLines(document: Document, startLine: Int, endLine: Int) {
        for (line in endLine downTo startLine) {
            val lineStartOffset = document.getLineStartOffset(line)
            val lineEndOffset = document.getLineEndOffset(line)
            val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))

            val commentIndex = lineText.indexOf("//")
            if (commentIndex != -1) {
                val removeEnd = if (lineText.length > commentIndex + 2 && lineText[commentIndex + 2] == ' ') {
                    commentIndex + 3
                } else {
                    commentIndex + 2
                }
                document.deleteString(lineStartOffset + commentIndex, lineStartOffset + removeEnd)
            }
        }
    }
}
