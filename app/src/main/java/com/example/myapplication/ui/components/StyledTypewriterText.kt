package com.example.myapplication.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.regex.Pattern

data class StyledSegment(val text: String, val isBold: Boolean)

fun parseMarkdownSegments(text: String): List<StyledSegment> {
    val segments = mutableListOf<StyledSegment>()
    val pattern = Pattern.compile("\\*\\*([^*]+)\\*\\*")
    val matcher = pattern.matcher(text)
    var currentIdx = 0
    while (matcher.find()) {
        if (currentIdx < matcher.start()) segments.add(StyledSegment(text.substring(currentIdx, matcher.start()), false))
        segments.add(StyledSegment(matcher.group(1) ?: "", true))
        currentIdx = matcher.end()
    }
    if (currentIdx < text.length) segments.add(StyledSegment(text.substring(currentIdx), false))
    return segments
}

fun createStyledAnnotatedString(segments: List<StyledSegment>, visibleCount: Int): AnnotatedString {
    var count = 0
    return buildAnnotatedString {
        for (seg in segments) {
            if (count >= visibleCount) break
            val chars = minOf(seg.text.length, visibleCount - count)
            if (seg.isBold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(seg.text.substring(0, chars)) }
            else append(seg.text.substring(0, chars))
            count += chars
        }
    }
}

@Composable
fun StyledTypewriterText(
    text: String,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Black,
    onFinished: () -> Unit = {}
) {
    val segments = remember(text) { parseMarkdownSegments(text) }
    val totalChars = remember(segments) { segments.sumOf { it.text.length } }
    var visibleCount by rememberSaveable { mutableIntStateOf(0) }
    var isTypingComplete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(text, isGenerating) {
        if (isTypingComplete) return@LaunchedEffect

        if (isGenerating) {
            for (i in visibleCount..totalChars) {
                if (!isGenerating) {
                    isTypingComplete = true
                    onFinished()
                    break
                }
                visibleCount = i
                delay(10)
            }
            if (visibleCount >= totalChars) {
                isTypingComplete = true
                onFinished()
            }
        } else {
            // If we load an old chat (not generating) and visible count is 0,
            // show the full text instantly.
            if (visibleCount == 0) {
                visibleCount = totalChars
            }
            isTypingComplete = true
            onFinished()
        }
    }

    val styledText = remember(segments, visibleCount) { createStyledAnnotatedString(segments, visibleCount) }
    Text(text = styledText, modifier = modifier, color = textColor, fontSize = 15.sp, lineHeight = 22.sp)
}