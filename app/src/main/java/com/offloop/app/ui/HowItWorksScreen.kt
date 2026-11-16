package com.offloop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offloop.app.R

@Composable
fun HowItWorksScreen(onBack: () -> Unit) {
    OffloopBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            BackButton(onClick = onBack)

            Spacer(Modifier.height(20.dp))

            Text(
                text = "How it works",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Offloop steps in only when you drift into an endless short-video feed.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            SectionLabel("THE FLOW")
            Spacer(Modifier.height(16.dp))

            TimelineStep(
                time = "0s",
                title = "You open Shorts or Reels",
                body = "Offloop recognises the feed from the screen's layout. It never reads what you are watching.",
                isLast = false
            )
            TimelineStep(
                time = "2s",
                title = "A pause appears",
                body = "A calm full-screen pause covers the feed and starts a five-second countdown.",
                isLast = false
            )
            TimelineStep(
                time = "7s",
                title = "Back to your home screen",
                body = "When the countdown ends, Offloop presses Home for you. The loop is broken.",
                isLast = true
            )

            Spacer(Modifier.height(28.dp))

            SectionLabel("YOUR CHOICES")
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceCard(
                    label = "Leave now",
                    body = "Go home straight away. The Back button does the same.",
                    primary = true
                )
                ChoiceCard(
                    label = "Stay 1 more minute",
                    body = "The pause closes and returns after 60 seconds if you are still scrolling.",
                    primary = false
                )
            }

            Spacer(Modifier.height(28.dp))

            SectionLabel("NEVER INTERRUPTS")
            Spacer(Modifier.height(12.dp))

            InfoCard(
                items = listOf(
                    "Phone and video calls" to "The pause waits until the call is over.",
                    "Regular videos and Stories" to "Only the endless Shorts and Reels feeds count.",
                    "Every other app" to "Messages, maps, music and the rest are ignored."
                )
            )

            Spacer(Modifier.height(28.dp))

            SectionLabel("PRIVACY")
            Spacer(Modifier.height(12.dp))

            InfoCard(
                items = listOf(
                    "No screenshots" to "Only the names of on-screen layout elements are checked.",
                    "No internet" to "Offloop has no network permission, so nothing can leave your phone.",
                    "Nothing stored" to "No history of what you watch is ever saved."
                )
            )

            Spacer(Modifier.height(28.dp))

            SectionLabel("WHY ACCESSIBILITY?")
            Spacer(Modifier.height(12.dp))

            Text(
                text = "Android only lets an accessibility service see which screen is open and show a pause above other apps. Offloop uses that access for this one job and nothing else.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TimelineStep(time: String, title: String, body: String, isLast: Boolean) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Lime.copy(alpha = 0.14f))
                    .border(1.dp, Lime.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Lime
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(Lime.copy(alpha = 0.2f))
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.padding(top = 10.dp, bottom = if (isLast) 0.dp else 24.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChoiceCard(label: String, body: String, primary: Boolean) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, shape)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (primary) Lime else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (primary) Color(0xFF172000) else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoCard(items: List<Pair<String, String>>) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, shape)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        items.forEachIndexed { index, (title, body) ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Lime)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
