package com.example.shortstop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnboardingScreen {
                getSharedPreferences("shortstop_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("has_completed_onboarding", true)
                    .apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF6200EE))) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPage(page)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Text("Back", color = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == i) 10.dp else 8.dp)
                                .background(
                                    if (pagerState.currentPage == i) Color.White else Color.White.copy(alpha = 0.3f),
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
                
                Button(
                    onClick = {
                        if (pagerState.currentPage == 2) {
                            onComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(
                        if (pagerState.currentPage == 2) "Get Started" else "Next",
                        color = Color(0xFF6200EE),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(page: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page) {
            0 -> {
                Text("🛑", fontSize = 80.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Text("Stop", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "The moment you open Instagram, TikTok, YouTube — or any app you chose to block — ShortStop immediately covers the screen with a 30-second pause. You cannot scroll. You cannot tap through. The app is still there, but you get 30 seconds first.",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
            1 -> {
                Text("🧘", fontSize = 80.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Text("Reflect", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "During those 30 seconds, you see a motivational message — not a lecture, just a nudge. Your brain shifts from autopilot to awareness. You remember why you installed this app. That tiny gap is where habits change.",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
            2 -> {
                Text("✅", fontSize = 80.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Text("Make a Conscious Decision", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "After the pause, you decide. Exit the app and earn reward points — or continue with full awareness that you made a mindful choice.",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }

        }
    }
}
