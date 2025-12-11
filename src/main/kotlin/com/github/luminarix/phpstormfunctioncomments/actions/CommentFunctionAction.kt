package com.github.luminarix.phpstormfunctioncomments.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.Method

abstract class CommentFunctionAction : AnAction() {

    private companion object {
        const val SINGLE_LINE_MARKER = "//"
        const val SINGLE_LINE_MARKER_LEN = 2
        const val SINGLE_LINE_PREFIX_LEN = 3
        const val MULTI_LINE_OPEN_MARKER = "/*"
        const val MULTI_LINE_CLOSE_MARKER = "*/"
        const val MULTI_LINE_MARKER_LEN = 2

        val FUNCTION_PATTERN = Regex(
            "^\\s*(public|private|protected|static|final|abstract|\\s)*\\s*function\\s+\\w+\\s*\\("
        )
        val FUNCTION_KEYWORD_PATTERN = Regex("function\\s+\\w+\\s*\\(")
        val VISIBILITY_FUNCTION_PATTERN = Regex("(public|private|protected|static)\\s+(static\\s+)?function\\s+")
        val BLOCK_COMMENT_OPEN = Regex("/\\*\\s*")
        val BLOCK_COMMENT_CLOSE = Regex("\\s*\\*/")
        val LINE_COMMENT_PREFIX = Regex("^\\s*//\\s?", RegexOption.MULTILINE)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)

        val isEnabled = editor != null && psiFile != null &&
                (findEnclosingFunction(psiFile, editor) != null || findCommentedFunction(psiFile, editor) != null)
        e.presentation.isEnabledAndVisible = isEnabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val project = e.project ?: return

        val function = findEnclosingFunction(psiFile, editor)
        val commentedFunction = findCommentedFunction(psiFile, editor)

        WriteCommandAction.runWriteCommandAction(project) {
            when {
                function != null -> commentFunction(project, editor, function)
                commentedFunction != null -> uncommentFunction(editor, commentedFunction)
            }
        }
    }

    protected abstract fun commentFunction(project: Project, editor: Editor, function: PsiElement)

    private fun uncommentFunction(editor: Editor, commentedFunction: CommentedFunctionInfo) {
        val document = editor.document
        val functionText = document.getText(TextRange(commentedFunction.startOffset, commentedFunction.endOffset))

        when (commentedFunction.style) {
            CommentStyle.SINGLE_LINE -> {
                val startLine = document.getLineNumber(commentedFunction.startOffset)
                val endLine = document.getLineNumber(commentedFunction.endOffset)
                uncommentSingleLine(document, startLine, endLine)
            }

            CommentStyle.MULTI_LINE -> {
                uncommentMultiLine(document, commentedFunction.startOffset, functionText)
            }

            CommentStyle.NONE -> {}
        }
    }

    private fun findEnclosingFunction(psiFile: PsiFile, editor: Editor): PsiElement? {
        val offset = editor.caretModel.offset

        val elementAtCaret = psiFile.findElementAt(offset)
        if (elementAtCaret != null) {
            val function = PsiTreeUtil.getParentOfType(elementAtCaret, Method::class.java, Function::class.java)
            if (function != null) return function
        }

        if (offset > 0) {
            val elementBefore = psiFile.findElementAt(offset - 1)
            if (elementBefore != null) {
                val function = PsiTreeUtil.getParentOfType(elementBefore, Method::class.java, Function::class.java)
                if (function != null) return function
            }
        }

        return null
    }

    private fun findCommentedFunction(psiFile: PsiFile, editor: Editor): CommentedFunctionInfo? {
        val offset = editor.caretModel.offset
        val elementAtCaret = psiFile.findElementAt(offset)

        if (elementAtCaret != null) {
            val blockComment = PsiTreeUtil.getParentOfType(elementAtCaret, PsiComment::class.java, false)
            if (blockComment != null && blockComment.text.trimStart().startsWith("/*") && containsFunctionPattern(
                    blockComment.text
                )
            ) {
                return CommentedFunctionInfo(
                    startOffset = blockComment.textRange.startOffset,
                    endOffset = blockComment.textRange.endOffset,
                    style = CommentStyle.MULTI_LINE
                )
            }
        }

        val document = editor.document
        val currentLine = document.getLineNumber(offset)

        val lineText = getLineText(document, currentLine)
        if (!lineText.trim().startsWith("//")) {
            return null
        }

        var blockStartLine = currentLine
        var blockEndLine = currentLine

        while (blockStartLine > 0) {
            val prevLineText = getLineText(document, blockStartLine - 1)
            if (prevLineText.trim().startsWith("//") || prevLineText.trim().isEmpty()) {
                blockStartLine--
            } else {
                break
            }
        }

        val totalLines = document.lineCount
        while (blockEndLine < totalLines - 1) {
            val nextLineText = getLineText(document, blockEndLine + 1)
            if (nextLineText.trim().startsWith("//") || nextLineText.trim().isEmpty()) {
                blockEndLine++
            } else {
                break
            }
        }

        val functionBounds = findFunctionBoundsInCommentBlock(document, blockStartLine, blockEndLine, currentLine)
            ?: return null

        val startOffset = document.getLineStartOffset(functionBounds.first)
        val endOffset = document.getLineEndOffset(functionBounds.second)

        return CommentedFunctionInfo(
            startOffset = startOffset,
            endOffset = endOffset,
            style = CommentStyle.SINGLE_LINE
        )
    }

    private fun findFunctionBoundsInCommentBlock(
        document: Document,
        blockStartLine: Int,
        blockEndLine: Int,
        cursorLine: Int
    ): Pair<Int, Int>? {
        val lineContents = buildMap {
            for (lineNum in blockStartLine..blockEndLine) {
                val text = getLineText(document, lineNum)
                val stripped = text.trim().removePrefix(SINGLE_LINE_MARKER).removePrefix(" ")
                put(lineNum, stripped)
            }
        }

        val functionKeywords = lineContents.entries
            .filter { (_, content) ->
                FUNCTION_PATTERN.containsMatchIn(content) || content.trimStart().startsWith("function ")
            }
            .map { it.key }
            .sorted()

        if (functionKeywords.isEmpty()) return null

        val functionEffectiveStarts = functionKeywords.map { keywordLine ->
            var effectiveStart = keywordLine
            for (lineNum in (blockStartLine until keywordLine).reversed()) {
                val content = lineContents[lineNum] ?: continue
                val trimmed = content.trim()
                when {
                    trimmed.startsWith("#[") || (trimmed.endsWith("]") && trimmed.contains("#[")) -> {
                        effectiveStart = lineNum
                    }

                    trimmed.isEmpty() -> continue
                    else -> break
                }
            }
            effectiveStart to keywordLine
        }

        var matchedFunction: Pair<Int, Int>? = null
        for (i in functionEffectiveStarts.indices) {
            val (effectiveStart, keywordLine) = functionEffectiveStarts[i]
            val nextEffectiveStart = functionEffectiveStarts.getOrNull(i + 1)?.first ?: (blockEndLine + 1)

            if (cursorLine in effectiveStart until nextEffectiveStart) {
                matchedFunction = effectiveStart to keywordLine
                break
            }
        }

        val (startLine, functionKeywordLine) = matchedFunction ?: return null

        var braceDepth = 0
        var foundOpenBrace = false

        for (lineNum in functionKeywordLine..blockEndLine) {
            val content = lineContents[lineNum] ?: continue
            var i = 0
            while (i < content.length) {
                when (val char = content[i]) {
                    '"', '\'' -> {
                        i++
                        while (i < content.length) {
                            if (content[i] == '\\' && i + 1 < content.length) {
                                i += 2
                            } else if (content[i] == char) {
                                break
                            } else {
                                i++
                            }
                        }
                    }

                    '{' -> {
                        braceDepth++
                        foundOpenBrace = true
                    }

                    '}' -> {
                        braceDepth--
                        if (foundOpenBrace && braceDepth == 0) {
                            return startLine to lineNum
                        }
                    }
                }
                i++
            }
        }

        return startLine to functionKeywordLine
    }

    private fun getLineText(document: Document, line: Int): String {
        val startOffset = document.getLineStartOffset(line)
        val endOffset = document.getLineEndOffset(line)
        return document.getText(TextRange(startOffset, endOffset))
    }

    private fun containsFunctionPattern(text: String): Boolean {
        val cleanedText = text
            .replace(BLOCK_COMMENT_OPEN, "")
            .replace(BLOCK_COMMENT_CLOSE, "")
            .replace(LINE_COMMENT_PREFIX, "")
            .trim()

        return FUNCTION_KEYWORD_PATTERN.containsMatchIn(cleanedText) ||
                VISIBILITY_FUNCTION_PATTERN.containsMatchIn(cleanedText)
    }

    protected fun getFunctionTextRange(function: PsiElement, document: Document): Pair<Int, Int> {
        var startOffset = function.textRange.startOffset
        val endOffset = function.textRange.endOffset

        val functionStartLine = document.getLineNumber(startOffset)
        var attributeStartLine = functionStartLine

        var checkLine = functionStartLine - 1
        while (checkLine >= 0) {
            val lineText = getLineText(document, checkLine).trim()
            if (lineText.startsWith("#[") || lineText.endsWith("]") && lineText.contains("#[")) {
                attributeStartLine = checkLine
                checkLine--
            } else if (lineText.isEmpty()) {
                checkLine--
            } else {
                break
            }
        }

        if (attributeStartLine < functionStartLine) {
            startOffset = document.getLineStartOffset(attributeStartLine)
        }

        return Pair(startOffset, endOffset)
    }

    private enum class CommentStyle {
        NONE,
        SINGLE_LINE,
        MULTI_LINE
    }

    private data class CommentedFunctionInfo(
        val startOffset: Int,
        val endOffset: Int,
        val style: CommentStyle
    )

    protected fun uncommentSingleLine(document: Document, startLine: Int, endLine: Int) {
        for (line in endLine downTo startLine) {
            val lineStartOffset = document.getLineStartOffset(line)
            val lineEndOffset = document.getLineEndOffset(line)
            val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))

            val commentIndex = lineText.indexOf(SINGLE_LINE_MARKER)
            if (commentIndex != -1) {
                val hasSpaceAfter = lineText.length > commentIndex + SINGLE_LINE_MARKER_LEN &&
                        lineText[commentIndex + SINGLE_LINE_MARKER_LEN] == ' '
                val removeEnd = commentIndex + if (hasSpaceAfter) SINGLE_LINE_PREFIX_LEN else SINGLE_LINE_MARKER_LEN
                document.deleteString(lineStartOffset + commentIndex, lineStartOffset + removeEnd)
            }
        }
    }

    protected fun uncommentMultiLine(document: Document, startOffset: Int, functionText: String) {
        val openIndex = functionText.indexOf(MULTI_LINE_OPEN_MARKER)
        val closeIndex = functionText.lastIndexOf(MULTI_LINE_CLOSE_MARKER)

        if (openIndex == -1 || closeIndex == -1) return

        val hasSpaceAfterOpen = functionText.length > openIndex + MULTI_LINE_MARKER_LEN &&
                functionText[openIndex + MULTI_LINE_MARKER_LEN] == ' '
        val openEnd = openIndex + if (hasSpaceAfterOpen) MULTI_LINE_MARKER_LEN + 1 else MULTI_LINE_MARKER_LEN

        val hasSpaceBeforeClose = closeIndex > 0 && functionText[closeIndex - 1] == ' '
        val closeStart = if (hasSpaceBeforeClose) closeIndex - 1 else closeIndex

        document.deleteString(startOffset + closeStart, startOffset + closeIndex + MULTI_LINE_MARKER_LEN)
        document.deleteString(startOffset + openIndex, startOffset + openEnd)
    }
}
