package com.example.ui.screens.coaching

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.NutriTrackApp
import com.example.data.entities.FoodLog
import com.example.data.entities.UserProfile
import com.example.data.repository.NutritionRepository
import com.example.gemini.GeminiService
import com.example.utils.getCurrentDateString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class CoachingViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val repository: NutritionRepository = (application as NutriTrackApp).repository

    val userProfile: StateFlow<UserProfile?> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("Hello there! I am your NutriTrack AI Coach. Ask me anything about your diet today, how to reach your protein goal, or recipe suggestions! 🍳", false)
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun sendMessage(msg: String) {
        if (msg.isBlank() || _isSending.value) return

        val userMsg = ChatMessage(msg, true)
        _messages.value = _messages.value + userMsg
        _isSending.value = true

        viewModelScope.launch {
            // Fetch today's context
            val profile = userProfile.value
            val today = getCurrentDateString()
            val foods = repository.getFoodsByDate(today).first()
            val totalCalories = foods.sumOf { it.calories }
            val totalProtein = foods.sumOf { it.proteinG }
            val totalCarbs = foods.sumOf { it.carbsG }
            val totalFats = foods.sumOf { it.fatG }

            val contextText = """
                User Target Budget: ${profile?.targetCalories ?: 2000} kcal (P: ${profile?.targetProtein ?: 120}g, C: ${profile?.targetCarbs ?: 220}g, F: ${profile?.targetFat ?: 65}g)
                Today's Eaten Logged: ${totalCalories.toInt()} kcal (P: ${totalProtein.toInt()}g, C: ${totalCarbs.toInt()}g, F: ${totalFats.toInt()}g)
                Fasting mode: ${profile?.fastingMode ?: false}
                Gender/Age: ${profile?.gender ?: "Male"}/${profile?.age ?: 25}
            """.trimIndent()

            val assistantReply = GeminiService.askNutritionCoach(msg, contextText, profile?.geminiApiKey)
            
            _messages.value = _messages.value + ChatMessage(assistantReply, false)
            _isSending.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachingScreen(
    onBack: () -> Unit,
    viewModel: CoachingViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()

    var textInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Nutrition Coach", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { chat ->
                    ChatBubble(chat)
                }

                if (isSending) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI Coach is formulating a response...", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Input panel
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ask your coach about protein, meals...", fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessage(textInput)
                                    textInput = ""
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send message", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(chat: ChatMessage) {
    val bColor = if (chat.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val tColor = if (chat.isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (chat.isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (chat.isUser) 16.dp else 0.dp,
                bottomEnd = if (chat.isUser) 0.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = chat.message,
                color = tColor,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
