package com.example.myapplication.data

/**
 * Data model representing a single chat message.
 * This class stores text content along with metadata for images and document attachments.
 */
data class ChatMessage(
    val role: String,           // Identity of the sender: "user" or "model"
    val content: String,        // Text content of the message
    val imageUri: String? = null, // Local URI for attached images
    val fileName: String? = null, // Original name of the attached document (e.g., "manual.pdf")
    val fileUri: String? = null    // Local URI for attached documents
)