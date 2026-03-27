package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.data.ChatMessage

@Composable
fun MessageBubble(
    message: ChatMessage,
    isGenerating: Boolean,
    onTypingFinished: () -> Unit = {},
    isDarkMode: Boolean // Added for theme support
) {
    val isUser = message.role == "user"

    // Theme Colors
    val themeDarkPurple = Color(0xFF8A5DCF)
    val aiBubbleColor = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF3E5F5)
    val aiTextColor = if (isDarkMode) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isUser) themeDarkPurple else aiBubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {

                // --- 1. IMAGE ATTACHMENT DISPLAY ---
                // Renders the image if the message contains an image URI
                message.imageUri?.let { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .heightIn(max = 250.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(bottom = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                // --- 2. DOCUMENT/PDF ATTACHMENT DISPLAY ---
                // Renders a stylized pill showing the filename and a document icon
                message.fileName?.let { fileName ->
                    val pdfBgColor = if (isUser) Color.White.copy(alpha = 0.2f) else if (isDarkMode) Color.White.copy(alpha = 0.1f) else themeDarkPurple.copy(alpha = 0.1f)
                    val pdfIconTint = if (isUser) Color.White else themeDarkPurple
                    val pdfTextColor = if (isUser) Color.White else aiTextColor

                    Row(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .background(color = pdfBgColor, shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "File Icon",
                            tint = pdfIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = fileName,
                            color = pdfTextColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 180.dp)
                        )
                    }
                }

                // --- 3. MESSAGE CONTENT DISPLAY ---
                if (isUser) {
                    // Standard text display for user messages
                    if (message.content.isNotBlank()) {
                        Text(
                            text = message.content,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    // Typewriter effect display for model responses
                    StyledTypewriterText(
                        text = message.content,
                        isGenerating = isGenerating,
                        textColor = aiTextColor,
                        onFinished = onTypingFinished
                    )
                }
            }
        }
    }
}