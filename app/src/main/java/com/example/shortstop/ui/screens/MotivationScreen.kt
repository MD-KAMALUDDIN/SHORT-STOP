package com.example.shortstop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun MotivationScreen(
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "💪 Daily Motivation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                // In a real nav graph, back is handled by the scaffold/app bar, 
                // but for now we keep the UI similar to the original overlap
                Button(onClick = onBack) {
                    Text("Back")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            var motivationText by remember { mutableStateOf("Loading inspiration...") }
            var isLoading by remember { mutableStateOf(true) }
            var refreshTrigger by remember { mutableIntStateOf(0) }
            
            LaunchedEffect(refreshTrigger) {
                isLoading = true
                try {
                    motivationText = withContext(Dispatchers.IO) {
                        fetchDynamicQuote()
                    }
                } catch (e: Exception) {
                    motivationText = getDailyMotivation()
                } finally {
                    isLoading = false
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth().clickable { refreshTrigger++ },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = motivationText,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { refreshTrigger++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄 Get New Quote")
            }
        }
    }
}

// Moved from MainActivity
fun getDailyMotivation(): String {
    val quotes = listOf(
        "Discipline is choosing between what you want now and what you want most.",
        "Your future is created by what you do today, not tomorrow.",
        "Don't let yesterday take up too much of today.",
        "Focus on being productive instead of busy.",
        "The only way to do great work is to love what you do.",
        "Success is the sum of small efforts, repeated day in and day out.",
        "You don't have to be great to start, but you have to start to be great.",
        "Action is the foundational key to all success.",
        "The secret of getting ahead is getting started.",
        "It always seems impossible until it's done."
    )
    return quotes.random()
}

// Moved from MainActivity
suspend fun fetchDynamicQuote(): String {
    return try {
        val url = URL("https://api.quotable.io/random?tags=wisdom|inspirational")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val content = json.getString("content")
            val author = json.getString("author")
            "\"$content\"\n- $author"
        } else {
            getDailyMotivation()
        }
    } catch (e: Exception) {
        getDailyMotivation()
    }
}
