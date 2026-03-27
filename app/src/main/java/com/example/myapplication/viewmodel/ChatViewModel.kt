package com.example.myapplication.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.ChatMessage
import com.example.myapplication.repository.GeminiService
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

/**
 * Data class representing a chat session for the sidebar history.
 */
data class ChatSession(val id: String, val title: String)

/**
 * Data class to hold temporary information about a selected attachment.
 */
data class LocalAttachment(val uri: Uri, val name: String, val mimeType: String?, val size: Long)

class ChatViewModel(private val geminiService: GeminiService) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    // State for recent chat history list in the sidebar
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions

    // State for the currently selected file or image attachment preview
    private val _selectedAttachment = MutableStateFlow<LocalAttachment?>(null)
    val selectedAttachment: StateFlow<LocalAttachment?> = _selectedAttachment

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var generationJob: Job? = null

    private var currentChatId: String = UUID.randomUUID().toString()
    private var isNewChat = true

    // New flag to ensure history loads correctly even after re-login
    private var isListenerAttached = false

    init {
        loadRecentChats()
    }

    fun onAttachmentSelected(attachment: LocalAttachment) {
        _selectedAttachment.value = attachment
    }

    fun clearSelectedAttachment() {
        _selectedAttachment.value = null
    }

    /**
     * Listens to Firestore updates to show the list of recent chats.
     * Made robust to ensure it attaches correctly and updates the sidebar.
     */
    private fun loadRecentChats() {
        val userId = auth.currentUser?.uid ?: return
        if (isListenerAttached) return // Prevent multiple duplicate listeners

        isListenerAttached = true

        // Calculate the timestamp for exactly 30 days ago
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val thirtyDaysAgo = System.currentTimeMillis() - thirtyDaysInMillis

        db.collection("users").document(userId).collection("chats")
            .whereGreaterThanOrEqualTo("timestamp", thirtyDaysAgo) // Filters out older chats
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isListenerAttached = false
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    val title = doc.getString("title") ?: "New Chat"
                    ChatSession(doc.id, title)
                } ?: emptyList()
                _chatSessions.value = sessions
            }
    }

    /**
     * Core function to handle sending messages, supporting text, images, and PDF documents.
     */
    fun sendMessage(text: String, context: Context) {
        // Validation: Block empty submissions
        if (text.isBlank() && _selectedAttachment.value == null) return

        // Ensure chat history listener is active (fixes the missing history bug)
        loadRecentChats()

        val attachment = _selectedAttachment.value
        val actualPromptText = text.ifBlank { "Please analyze this attached file." }

        // Initialize chat session on the first message
        if (isNewChat) {
            saveChatSession(currentChatId, actualPromptText)
            isNewChat = false
        }

        // Determine attachment types for metadata handling
        val isImage = attachment?.mimeType?.startsWith("image/") == true
        val isPdf = attachment?.mimeType?.contains("pdf") == true

        // Construct the message object with metadata for UI thumbnails
        val userMessage = ChatMessage(
            role = "user",
            content = actualPromptText,
            imageUri = if (isImage) attachment?.uri?.toString() else null,
            fileName = if (isPdf) attachment?.name else null,
            fileUri = if (isPdf) attachment?.uri?.toString() else null
        )

        // UI Update and database persistence
        _messages.value = _messages.value + userMessage
        saveMessageToFirestore(userMessage)

        // Clear input state immediately
        clearSelectedAttachment()

        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            try {
                val response = when {
                    attachment != null && isImage -> {
                        // Handle multimodal Image analysis (with background processing)
                        val bitmap = uriToBitmap(attachment.uri, context)
                        if (bitmap != null) {
                            geminiService.getResponseWithImage(actualPromptText, bitmap)
                        } else {
                            "Error: Failed to process the image data."
                        }
                    }
                    attachment != null -> {
                        // Handle Document analysis (Offloaded to IO thread to prevent ANR)
                        val extractedText = extractTextFromFile(attachment.uri, context, attachment.mimeType)
                        val documentPrompt = """
                            Document Name: ${attachment.name}
                            --- CONTENT ---
                            $extractedText
                            --- END ---
                            
                            User Instruction: $actualPromptText
                        """.trimIndent()
                        geminiService.getResponse(documentPrompt)
                    }
                    else -> {
                        // Standard text-only conversation
                        geminiService.getResponse(actualPromptText)
                    }
                }

                val modelMessage = ChatMessage(role = "model", content = response)
                _messages.value = _messages.value + modelMessage
                saveMessageToFirestore(modelMessage)

            } catch (e: Exception) {
                _isGenerating.value = false
            }

        }
    }

    /**
     * Extracts text content from PDF/Text files using Dispatchers.IO
     * to prevent the Main UI thread from freezing.
     */
    private suspend fun extractTextFromFile(uri: Uri, context: Context, mimeType: String?): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""
            when {
                mimeType?.contains("pdf") == true || mimeType == "application/pdf" -> {
                    val document = PDDocument.load(inputStream)
                    val stripper = PDFTextStripper()
                    val text = stripper.getText(document)
                    document.close()
                    text.take(30000) // Truncate content for API token safety
                }
                mimeType?.contains("text") == true || mimeType?.contains("csv") == true -> {
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val text = reader.readText()
                    reader.close()
                    text.take(30000)
                }
                else -> "Unsupported file type: $mimeType. Please attach a PDF or TXT file."
            }
        } catch (e: Exception) {
            "Error extracting content: ${e.message}"
        }
    }

    /**
     * Resolves a Uri into a Bitmap using Dispatchers.IO background thread.
     */
    private suspend fun uriToBitmap(uri: Uri, context: Context): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves the initial metadata for a new chat session.
     */
    private fun saveChatSession(chatId: String, firstMessage: String) {
        val userId = auth.currentUser?.uid ?: return
        val title = if (firstMessage.length > 25) firstMessage.take(25) + "..." else firstMessage

        db.collection("users").document(userId).collection("chats").document(chatId)
            .set(mapOf(
                "title" to title,
                "timestamp" to System.currentTimeMillis()
            ))
    }

    /**
     * Persists a single message into Firestore, including multimodal metadata if present.
     */
    private fun saveMessageToFirestore(message: ChatMessage) {
        val userId = auth.currentUser?.uid ?: return

        val data = mutableMapOf<String, Any>(
            "role" to message.role,
            "content" to message.content,
            "timestamp" to System.currentTimeMillis()
        )
        // Store attachment metadata for rendering history in future sessions
        message.imageUri?.let { data["imageUri"] = it }
        message.fileName?.let { data["fileName"] = it }
        message.fileUri?.let { data["fileUri"] = it }

        db.collection("users").document(userId)
            .collection("chats").document(currentChatId)
            .collection("messages")
            .add(data)
    }

    /**
     * Fetches historical messages for a specific chat ID, including multimodal metadata.
     */
    fun loadChat(chatId: String) {
        currentChatId = chatId
        isNewChat = false

        // Ensure chat history listener is active when loading an old chat
        loadRecentChats()

        val userId = auth.currentUser?.uid ?: return

        _messages.value = emptyList()

        db.collection("users").document(userId)
            .collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val loadedMessages = snapshot.documents.mapNotNull { doc ->
                    val role = doc.getString("role") ?: return@mapNotNull null
                    val content = doc.getString("content") ?: return@mapNotNull null

                    ChatMessage(
                        role = role,
                        content = content,
                        imageUri = doc.getString("imageUri"),
                        fileName = doc.getString("fileName"),
                        fileUri = doc.getString("fileUri")
                    )
                }
                _messages.value = loadedMessages
            }
    }

    fun onTypingFinished() {
        _isGenerating.value = false
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
    }

    /**
     * Resets the UI and generates a new session ID for a fresh conversation.
     */
    fun clearChat() {
        generationJob?.cancel()
        _messages.value = emptyList()
        _isGenerating.value = false
        currentChatId = UUID.randomUUID().toString()
        isNewChat = true

        // Ensure chat history listener is active when clearing
        loadRecentChats()
    }
}