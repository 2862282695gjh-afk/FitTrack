package com.fittrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 简易 Markdown 渲染组件
 * 支持：标题、粗体、斜体、代码块、列表
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: Int = 14
) {
    val lines = markdown.split("\n")

    Column(modifier = modifier) {
        var inCodeBlock = false
        var codeBlockContent = StringBuilder()

        lines.forEach { line ->
            when {
                // 代码块开始/结束
                line.trim().startsWith("```") -> {
                    if (inCodeBlock) {
                        // 代码块结束
                        if (codeBlockContent.isNotEmpty()) {
                            CodeBlock(
                                code = codeBlockContent.toString().trimEnd(),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            codeBlockContent = StringBuilder()
                        }
                        inCodeBlock = false
                    } else {
                        // 代码块开始
                        inCodeBlock = true
                    }
                }
                // 代码块内容
                inCodeBlock -> {
                    codeBlockContent.append(line).append("\n")
                }
                // 标题
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                // 无序列表
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                    val content = line.trim().removePrefix("- ").removePrefix("* ")
                    BulletPoint(
                        text = content,
                        color = color,
                        fontSize = fontSize
                    )
                }
                // 有序列表
                line.trim().matches(Regex("^\\d+\\.\\s.*")) -> {
                    val content = line.trim().replaceFirst(Regex("^\\d+\\.\\s"), "")
                    NumberedPoint(
                        text = content,
                        color = color,
                        fontSize = fontSize
                    )
                }
                // 分隔线
                line.trim() == "---" || line.trim() == "***" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(color.copy(alpha = 0.2f))
                            .padding(vertical = 8.dp)
                    )
                }
                // 普通文本
                line.isNotBlank() -> {
                    Text(
                        text = parseInlineMarkdown(line, color),
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize + 6).sp,
                        color = color
                    )
                }
                // 空行
                else -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * 解析行内 Markdown（粗体、斜体、行内代码）
 */
@Composable
private fun parseInlineMarkdown(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var remaining = text

        while (remaining.isNotEmpty()) {
            when {
                // 行内代码 `code`
                remaining.contains("`") -> {
                    val beforeCode = remaining.substringBefore("`")
                    val afterFirstTick = remaining.substringAfter("`")
                    val code = afterFirstTick.substringBefore("`")
                    val afterCode = afterFirstTick.substringAfter("`", missingDelimiterValue = "")

                    // 添加前面的普通文本（处理粗体斜体）
                    appendParsedText(beforeCode, baseColor)

                    // 添加代码
                    if (code.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = MaterialTheme.colorScheme.surfaceVariant,
                                fontSize = 13.sp
                            )
                        ) {
                            append(code)
                        }
                    }

                    remaining = afterCode
                }
                // 粗体 **text**
                remaining.contains("**") -> {
                    val beforeBold = remaining.substringBefore("**")
                    val afterFirstBold = remaining.substringAfter("**")
                    val boldText = afterFirstBold.substringBefore("**", missingDelimiterValue = "")
                    val afterBold = if (boldText.isNotEmpty()) {
                        afterFirstBold.substringAfter("**")
                    } else {
                        ""
                    }

                    appendParsedText(beforeBold, baseColor)

                    if (boldText.isNotEmpty()) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(boldText)
                        }
                        remaining = afterBold
                    } else {
                        append("**")
                        remaining = afterFirstBold.removePrefix("**")
                    }
                }
                // 斜体 *text* 或 _text_
                remaining.contains("*") || remaining.contains("_") -> {
                    val delimiter = if (remaining.contains("*")) "*" else "_"
                    val beforeItalic = remaining.substringBefore(delimiter)
                    val afterFirstItalic = remaining.substringAfter(delimiter)
                    val italicText = afterFirstItalic.substringBefore(delimiter, missingDelimiterValue = "")
                    val afterItalic = if (italicText.isNotEmpty()) {
                        afterFirstItalic.substringAfter(delimiter)
                    } else {
                        ""
                    }

                    appendParsedText(beforeItalic, baseColor)

                    if (italicText.isNotEmpty()) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(italicText)
                        }
                        remaining = afterItalic
                    } else {
                        append(delimiter)
                        remaining = afterFirstItalic.removePrefix(delimiter)
                    }
                }
                // 普通文本
                else -> {
                    append(remaining)
                    remaining = ""
                }
            }
        }
    }
}

/**
 * 辅助函数：追加解析后的文本
 */
private fun AnnotatedString.Builder.appendParsedText(text: String, baseColor: Color) {
    if (text.isNotEmpty()) {
        append(text)
    }
}

/**
 * 无序列表项
 */
@Composable
private fun BulletPoint(
    text: String,
    color: Color,
    fontSize: Int
) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
    ) {
        Text(
            text = "•",
            color = MaterialTheme.colorScheme.primary,
            fontSize = fontSize.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = parseInlineMarkdown(text, color),
            color = color,
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 6).sp
        )
    }
}

/**
 * 有序列表项
 */
@Composable
private fun NumberedPoint(
    text: String,
    color: Color,
    fontSize: Int
) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
    ) {
        Text(
            text = "▸",
            color = MaterialTheme.colorScheme.primary,
            fontSize = fontSize.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = parseInlineMarkdown(text, color),
            color = color,
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 6).sp
        )
    }
}

/**
 * 代码块组件
 */
@Composable
private fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}
