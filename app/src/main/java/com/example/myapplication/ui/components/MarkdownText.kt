package com.example.myapplication.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

// Function to convert raw Markdown text to a Composable with formatting
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Black
) {
    // 1. Convert raw text to styled AnnotatedString
    val annotatedString = parseMarkdownToAnnotatedString(text)

    // 2. Display the styled text
    Text(
        text = annotatedString,
        modifier = modifier,
        color = textColor,
        fontSize = 15.sp, // Match your standard font size
        lineHeight = 22.sp // Better spacing for lists, like Gemini
    )
}

// Logic to parse "**text**" into bold styled AnnotatedString
fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        // Regex to find everything between double asterisks
        val pattern = Pattern.compile("\\*\\*([^*]+)\\*\\*")
        val matcher = pattern.matcher(text)
        var currentIdx = 0

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val boldText = matcher.group(1) ?: ""

            // Append regular text before bold match
            if (currentIdx < start) {
                append(text.substring(currentIdx, start))
            }

            // Append bold text with FontWeight.Bold style
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(boldText)
            }
            currentIdx = end
        }

        // Append remaining text after last match
        if (currentIdx < text.length) {
            append(text.substring(currentIdx))
        }
    }
}