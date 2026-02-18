package com.example.shortstop.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun AppIcon(
    drawable: Drawable,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(drawable) {
        drawable.toBitmap(width = 120, height = 120)
    }
    
    Image(
        painter = BitmapPainter(bitmap.asImageBitmap()),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun SimpleBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.second } ?: 1
    
    Column(modifier = modifier) {
        // Bar chart
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            val barWidth = size.width / data.size
            val barSpacing = barWidth * 0.1f
            val actualBarWidth = barWidth - barSpacing
            
            data.forEachIndexed { index, (label, value) ->
                val barHeight = if (maxValue > 0) (value.toFloat() / maxValue) * size.height * 0.85f else 0f
                val x = index * barWidth + barSpacing / 2
                val y = size.height - barHeight
                
                // Draw bar
                drawRect(
                    color = androidx.compose.ui.graphics.Color(0xFF2196F3),
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(actualBarWidth, barHeight)
                )
            }
        }
        
        // X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AchievementCard(
    title: String,
    description: String,
    emoji: String,
    isUnlocked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                Color(0xFFFFD700).copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isUnlocked) emoji else "🔒",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
