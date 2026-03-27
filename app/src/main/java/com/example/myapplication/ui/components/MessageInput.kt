package com.example.myapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.R
import com.example.myapplication.viewmodel.LocalAttachment

@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    onStopClick: () -> Unit,
    onImageClick: () -> Unit,
    onFileClick: () -> Unit,
    isLoading: Boolean,
    selectedAttachment: LocalAttachment?,
    onClearAttachment: () -> Unit,
    isDarkMode: Boolean // Added to support dynamic theme changes
) {
    var messageText by remember { mutableStateOf("") }
    var isForceStopped by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    // Dynamic theme colors
    val themePurple = Color(0xFF8A5DCF)
    val inputBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF3E5F5)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val hintColor = if (isDarkMode) Color.LightGray else Color.Gray

    LaunchedEffect(isLoading) {
        if (isLoading) {
            isForceStopped = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Attachment Menu Overlay ---
        AnimatedVisibility(
            visible = showAttachmentMenu,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            AttachmentMenu(
                themePurple = themePurple,
                isDarkMode = isDarkMode,
                onImageClick = {
                    onImageClick()
                    showAttachmentMenu = false
                },
                onFileClick = {
                    onFileClick()
                    showAttachmentMenu = false
                }
            )
        }

        // --- Attachment Preview ---
        AnimatedVisibility(
            visible = selectedAttachment != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
        ) {
            if (selectedAttachment != null) {
                AttachmentPreview(
                    attachment = selectedAttachment,
                    themePurple = themePurple,
                    isDarkMode = isDarkMode,
                    onClear = onClearAttachment
                )
            }
        }

        // --- Main Input Area ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .defaultMinSize(minHeight = 56.dp),
            shape = RoundedCornerShape(28.dp),
            color = inputBackgroundColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showAttachmentMenu = !showAttachmentMenu },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_attach_clip),
                        contentDescription = "Attach Options",
                        tint = if (showAttachmentMenu) textColor else themePurple,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (messageText.isEmpty()) {
                        Text("Chat with Lumo", color = hintColor)
                    }
                    BasicTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        textStyle = TextStyle(fontSize = 16.sp, color = textColor),
                        cursorBrush = SolidColor(themePurple),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isLoading && !isForceStopped) {
                    IconButton(
                        onClick = {
                            onStopClick()
                            isForceStopped = true
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(themePurple, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.White
                        )
                    }
                } else if (messageText.isNotBlank() || selectedAttachment != null) {
                    IconButton(
                        onClick = {
                            onSendMessage(messageText)
                            messageText = ""
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(themePurple, CircleShape),
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Text(
            text = "Lumo is AI and can make mistakes.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
fun AttachmentMenu(themePurple: Color, isDarkMode: Boolean, onImageClick: () -> Unit, onFileClick: () -> Unit) {
    val menuBgColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White

    Surface(
        color = menuBgColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentMenuItem(Icons.Default.Image, "Gallery", themePurple, isDarkMode, onImageClick)
            AttachmentMenuItem(Icons.Default.FilePresent, "Files", themePurple, isDarkMode, onFileClick)
        }
    }
}

@Composable
fun AttachmentMenuItem(icon: ImageVector, label: String, themePurple: Color, isDarkMode: Boolean, onClick: () -> Unit) {
    val textColor = if (isDarkMode) Color.LightGray else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {

        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(themePurple.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = themePurple,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = textColor,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun AttachmentPreview(attachment: LocalAttachment, themePurple: Color, isDarkMode: Boolean, onClear: () -> Unit) {
    val previewBgColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFE1F5FE)
    val textColor = if (isDarkMode) Color.White else Color.Black

    Surface(
        color = previewBgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.wrapContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val isImage = attachment.mimeType?.startsWith("image/") == true
            if (isImage) {
                Image(
                    painter = rememberAsyncImagePainter(attachment.uri),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.FilePresent, contentDescription = null, tint = themePurple, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(attachment.name, fontSize = 12.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 150.dp))
            Spacer(Modifier.width(8.dp))

            Icon(
                Icons.Default.Close,
                contentDescription = "Clear Attachment",
                tint = textColor,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { onClear() }
            )
        }
    }
}