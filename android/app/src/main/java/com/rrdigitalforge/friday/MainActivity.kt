package com.rrdigitalforge.friday

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

private val Bg = Color(0xFF090B12)
private val Glass = Color(0xCC151925)
private val White = Color(0xFFF5F7FF)
private val Muted = Color(0xFF9BA4BB)
private val Accent = Color(0xFF8793FF)
private val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS).callTimeout(70, TimeUnit.SECONDS).build()

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(0.94f)
            }
        }
        setContent { FridayScreen { text -> tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FRIDAY_REPLY") } }
    }
    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
}

private suspend fun askFriday(message: String): String = withContext(Dispatchers.IO) {
    val payload = JSONObject().put("message", message).put("language", Locale.getDefault().toLanguageTag())
    val request = Request.Builder().url(BuildConfig.BACKEND_BASE_URL.trimEnd('/') + "/chat")
        .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
    http.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw IllegalStateException("AI server returned HTTP ${response.code}")
        JSONObject(body).optString("reply").ifBlank { "I couldn't generate a reply. Please try again." }
    }
}

@Composable
fun FridayScreen(speak: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("नमस्कार! मी FRIDAY आहे. How can I help you today?") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Your personal AI agent") }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { input = it }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to FRIDAY")
        })
    }
    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Glass)) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF202750), Bg, Bg), radius = 1100f))) {
            Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("FRIDAY", color = White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                        Text("PERSONAL AI AGENT", color = Accent, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                    Text("AI ASSISTANT", color = Muted, fontSize = 10.sp)
                }
                Spacer(Modifier.height(24.dp))
                Text(status, color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                val pulse = rememberInfiniteTransition(label = "orb")
                val scale by pulse.animateFloat(1f, 1.07f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "scale")
                Box(Modifier.size(184.dp).scale(scale).background(
                    Brush.radialGradient(listOf(Accent, Color(0xFF3A4CB0), Color.Transparent)), CircleShape
                ).border(1.dp, Accent.copy(alpha=.55f), CircleShape).clickable {
                    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to FRIDAY")
                        })
                    } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, "Speak to FRIDAY", tint = Color.White, modifier = Modifier.size(52.dp))
                }
                Spacer(Modifier.height(24.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Glass, tonalElevation = 5.dp) {
                    Column(Modifier.padding(19.dp)) {
                        Text(if (busy) "THINKING…" else "FRIDAY", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(reply, color = White, fontSize = 16.sp, lineHeight = 24.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth().background(Glass, RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha=.08f), RoundedCornerShape(24.dp)).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    TextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("Message FRIDAY…", color = Muted) }, maxLines = 3,
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent, focusedTextColor = White, unfocusedTextColor = White))
                    IconButton(enabled = !busy && input.isNotBlank(), onClick = {
                        val question = input.trim()
                        busy = true
                        status = "Connecting to secure AI backend…"
                        scope.launch {
                            try {
                                val answer = askFriday(question)
                                reply = answer
                                status = "Ready when you are"
                                speak(answer)
                            } catch (_: Exception) {
                                reply = "I couldn't connect to the AI server. Check the backend URL and internet connection, then try again."
                                status = "Backend not connected"
                            } finally { busy = false }
                        }
                    }) { Icon(Icons.Default.Send, "Send", tint = Accent) }
                }
                Spacer(Modifier.height(12.dp))
                Text("Multilingual input • Voice output • Secure backend", color = Muted, fontSize = 10.sp)
            }
        }
    }
}
