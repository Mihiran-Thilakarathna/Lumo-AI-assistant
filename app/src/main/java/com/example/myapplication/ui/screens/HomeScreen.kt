package com.example.myapplication.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.MessageInput
import com.example.myapplication.ui.components.MessageList
import com.example.myapplication.viewmodel.ChatViewModel
import com.example.myapplication.viewmodel.LocalAttachment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: ChatViewModel, onSignOut: () -> Unit) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()

    // Observe the selected attachment from the ViewModel
    val selectedAttachment by viewModel.selectedAttachment.collectAsState()

    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showProfileMenu by remember { mutableStateOf(false) }

    // --- Theme State ---
    // Defaults to Light Mode (false). Toggling this changes dynamic colors.
    var isDarkMode by remember { mutableStateOf(false) }

    // --- Dynamic Colors based on Theme State ---
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color.White
    val drawerBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF8F9FA)
    val popupMenuColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val mainTextColor = if (isDarkMode) Color.White else Color(0xFF2D2D2D)
    val themePurple = Color(0xFF8A5DCF)
    val themeLightPurple = Color(0xFFF3E5F5)

    // --- FILE PICKER LAUNCHERS ---

    // Direct Gallery/Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val attachmentInfo = getAttachmentInfo(it, context, "image/*")
            viewModel.onAttachmentSelected(attachmentInfo)
        }
    }

    // Generic Document/File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val attachmentInfo = getAttachmentInfo(it, context, null)
            viewModel.onAttachmentSelected(attachmentInfo)
        }
    }

    val user = Firebase.auth.currentUser
    val fullName = user?.displayName ?: ""
    val firstName = if (fullName.isNotBlank()) fullName.substringBefore(" ") else "User"
    val email = user?.email ?: "No email"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = drawerBackgroundColor
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Lumo Chats",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = themePurple,
                    modifier = Modifier.padding(16.dp)
                )

                HorizontalDivider(color = if (isDarkMode) Color(0xFF333333) else Color.LightGray)

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = themePurple
                        )
                    },
                    label = {
                        Text(
                            text = "New Chat",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themePurple
                        )
                    },
                    selected = false,
                    onClick = {
                        viewModel.clearChat()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = if (isDarkMode) Color(0xFF2C1C45) else themeLightPurple
                    )
                )

                HorizontalDivider(color = if (isDarkMode) Color(0xFF333333) else Color.LightGray)

                Text(
                    text = "Recent Chats",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )

                // --- SCROLLABLE CHAT HISTORY AREA ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes the remaining space
                        .verticalScroll(rememberScrollState()) // Makes the list scrollable
                        .padding(bottom = 16.dp)
                ) {
                    chatSessions.forEach { session ->
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu_list),
                                    contentDescription = "Chat",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Gray
                                )
                            },
                            label = {
                                Text(
                                    text = session.title,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = mainTextColor
                                )
                            },
                            selected = false,
                            onClick = {
                                viewModel.loadChat(session.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = backgroundColor,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = backgroundColor
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lumo_logo),
                                contentDescription = "App Logo",
                                tint = themePurple,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Lumo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = themePurple
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_menu_list),
                                contentDescription = "Menu",
                                tint = themePurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    actions = {
                        // --- THEME TOGGLE BUTTON ---
                        IconButton(onClick = { isDarkMode = !isDarkMode }) {
                            Icon(
                                painter = painterResource(id = if (isDarkMode) R.drawable.ic_light_mode else R.drawable.ic_dark_mode),
                                contentDescription = "Toggle Theme",
                                tint = themePurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // --- PROFILE MENU ---
                        Box {
                            IconButton(onClick = { showProfileMenu = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_profile),
                                    contentDescription = "Profile",
                                    tint = themePurple,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            MaterialTheme(
                                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                            ) {
                                DropdownMenu(
                                    expanded = showProfileMenu,
                                    onDismissRequest = { showProfileMenu = false },
                                    modifier = Modifier
                                        .background(popupMenuColor)
                                        .padding(4.dp)
                                        .width(240.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                        Text(
                                            text = "Hi, $firstName",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = mainTextColor
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = email,
                                            fontSize = 13.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    HorizontalDivider(color = if (isDarkMode) Color(0xFF444444) else Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 4.dp))

                                    // --- SIGN OUT BUTTON ---
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFD32F2F))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Sign Out", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                                            }
                                        },
                                        onClick = {
                                            showProfileMenu = false

                                            Firebase.auth.signOut()
                                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                                            val googleSignInClient = GoogleSignIn.getClient(context, gso)

                                            googleSignInClient.signOut().addOnCompleteListener {
                                                onSignOut()
                                            }
                                        },
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDarkMode) Color(0xFF3B1A1A) else Color(0xFFFDEDED))
                                    )
                                }
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (messages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Hello, $firstName",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themePurple
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "How can Lumo help you today?",
                                    color = Color.Gray,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    } else {
                        // Updated to pass the isDarkMode parameter to the list
                        MessageList(
                            messages = messages,
                            isGenerating = isGenerating,
                            onTypingFinished = { viewModel.onTypingFinished() },
                            isDarkMode = isDarkMode
                        )
                    }
                }

                // Updated MessageInput call to pass the isDarkMode parameter
                MessageInput(
                    onSendMessage = { viewModel.sendMessage(it, context) },
                    onStopClick = { viewModel.stopGeneration() },
                    onImageClick = { imagePickerLauncher.launch("image/*") },
                    onFileClick = { filePickerLauncher.launch("*/*") },
                    isLoading = isGenerating,
                    selectedAttachment = selectedAttachment,
                    onClearAttachment = { viewModel.clearSelectedAttachment() },
                    isDarkMode = isDarkMode
                )

                if (isGenerating) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = themePurple)
                }
            }
        }
    }
}

// Helper function to extract file metadata from URI
fun getAttachmentInfo(uri: Uri, context: Context, fallbackMime: String?): LocalAttachment {
    var name = "Unknown File"
    val mimeType = fallbackMime ?: context.contentResolver.getType(uri)
    var size = 0L

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) name = cursor.getString(nameIndex)

            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
        }
    }
    return LocalAttachment(uri, name, mimeType, size)
}