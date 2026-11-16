package com.offloop.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.offloop.app.R

val Lime = Color(0xFFC8F56A)
val Coral = Color(0xFFFF8A65)

@Composable
fun OffloopBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Lime.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.12f),
                        radius = size.width * 0.9f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Lime.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = size.width * 0.8f
                    )
                )
            },
        content = content
    )
}

@Composable
fun OffloopLogo(size: Dp) {
    // The adaptive-icon layers are 108 units wide with the artwork in the
    // central 72, so draw them oversized and let the clip crop the bleed.
    val layerSize = size * 1.5f
    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Lime.copy(alpha = 0.35f), Color.Transparent),
                        radius = this.size.width
                    ),
                    radius = this.size.width
                )
            }
            .clip(RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.requiredSize(layerSize)
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Offloop logo",
            modifier = Modifier.requiredSize(layerSize)
        )
    }
}

/** "Offloop" with the double o drawn as the logo's broken infinity. */
@Composable
fun OffloopWordmark(
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    val text = buildAnnotatedString {
        append("Offl")
        appendInlineContent(WORDMARK_LOOP_ID, "oo")
        append("p")
    }
    val loop = InlineTextContent(
        Placeholder(
            width = 1.134.em,
            height = 0.661.em,
            placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_wordmark_loop),
            contentDescription = null,
            tint = Lime,
            modifier = Modifier.fillMaxSize()
        )
    }

    Text(
        text = text,
        inlineContent = mapOf(WORDMARK_LOOP_ID to loop),
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}

private const val WORDMARK_LOOP_ID = "loop"

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 2,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier.fillMaxWidth()
    )
}
