package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.ChatMessage

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    onTypingFinished: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean
) {
    val scrollState = rememberLazyListState()

    // Preserved auto-scroll logic to focus on latest messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = scrollState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(messages) { message ->
            MessageBubble(
                message = message,
                isGenerating = isGenerating,
                onTypingFinished = onTypingFinished,
                isDarkMode = isDarkMode
            )
        }
    }
}