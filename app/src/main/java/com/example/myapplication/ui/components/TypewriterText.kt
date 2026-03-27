package com.example.myapplication.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Black
) {
    // State to hold the text currently being displayed
    var displayedText by rememberSaveable { mutableStateOf("") }
    // State to remember if typing has already finished for this specific text
    var isTypingComplete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(text) {
        if (!isTypingComplete) {
            displayedText = ""
            for (i in text.indices) {
                displayedText += text[i]
                delay(10) // Typing speed (10 milliseconds per character)
            }
            isTypingComplete = true
        } else {
            // If already complete, just show the full text immediately (useful for scrolling)
            displayedText = text
        }
    }

    Text(
        text = displayedText,
        modifier = modifier,
        color = textColor,
        fontSize = 16.sp
    )
}