package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

object CoverArtUtils {
    /**
     * Extracts track initials for English, Bangla (\u0980-\u09FF), Hindi (\u0900-\u097F),
     * and other regional scripts.
     */
    fun getTrackInitials(title: String): String {
        val clean = title.trim()
            .replace(Regex("^[\\W_]+"), "")
        if (clean.isEmpty()) return "SP"

        val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (words.size >= 2) {
            val first = extractFirstCharOrGrapheme(words[0])
            val second = extractFirstCharOrGrapheme(words[1])
            "$first$second"
        } else {
            val single = words[0]
            if (single.length >= 2) {
                single.take(2).uppercase()
            } else {
                single.uppercase()
            }
        }
    }

    private fun extractFirstCharOrGrapheme(word: String): String {
        if (word.isEmpty()) return ""
        val firstCodePoint = word.codePointAt(0)
        val chars = Character.toChars(firstCodePoint)
        var str = String(chars)
        // If Bengali or Devanagari combining mark follows, preserve for proper script representation
        if (word.length > str.length) {
            val nextChar = word[str.length]
            if (nextChar in '\u0980'..'\u09FF' || nextChar in '\u0900'..'\u097F') {
                val type = Character.getType(nextChar).toByte()
                if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK) {
                    str += nextChar
                }
            }
        }
        return str.uppercase()
    }

    fun getDominantGradientColors(dominantColorHex: Long?, seed: String): List<Color> {
        val baseColor = if (dominantColorHex != null && dominantColorHex != 0L) {
            Color(dominantColorHex)
        } else {
            Color(generatePaletteColor(seed))
        }
        val darker = Color(
            red = (baseColor.red * 0.45f).coerceIn(0f, 1f),
            green = (baseColor.green * 0.45f).coerceIn(0f, 1f),
            blue = (baseColor.blue * 0.45f).coerceIn(0f, 1f),
            alpha = 1f
        )
        return listOf(baseColor, darker, Color(0xFF121212))
    }

    fun generatePaletteColor(seed: String): Long {
        val palette = listOf(
            0xFF1DB954L, // Spotify Green
            0xFF1E3264L, // Deep Indigo
            0xFF8C1932L, // Crimson
            0xFF503750L, // Plum
            0xFFE8115BL, // Vivid Pink
            0xFFBA5D07L, // Amber Orange
            0xFF2D46B9L, // Royal Blue
            0xFF148A08L, // Forest Green
            0xFF006450L, // Dark Teal
            0xFF7D4B32L  // Warm Copper
        )
        val hash = kotlin.math.abs(seed.hashCode())
        return palette[hash % palette.size]
    }
}

/**
 * SpotifyCoverArt: Stage 4 Quaternary Cover Art component with automatic fallback
 * to vibrant gradients, soundwave/vinyl discs, and multi-language track initials.
 */
@Composable
fun SpotifyCoverArt(
    artworkUrl: String?,
    title: String,
    artist: String,
    dominantColorHex: Long?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    initialsSize: TextUnit = 14.sp,
    showInitials: Boolean = true
) {
    val gradientColors = remember(dominantColorHex, title) {
        CoverArtUtils.getDominantGradientColors(dominantColorHex, title)
    }
    val initials = remember(title) {
        CoverArtUtils.getTrackInitials(title)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = artworkUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    CoverArtPlaceholder(
                        gradientColors = gradientColors,
                        initials = initials,
                        initialsSize = initialsSize,
                        showInitials = showInitials
                    )
                },
                error = {
                    CoverArtPlaceholder(
                        gradientColors = gradientColors,
                        initials = initials,
                        initialsSize = initialsSize,
                        showInitials = showInitials
                    )
                }
            )
        } else {
            CoverArtPlaceholder(
                gradientColors = gradientColors,
                initials = initials,
                initialsSize = initialsSize,
                showInitials = showInitials
            )
        }
    }
}

@Composable
private fun CoverArtPlaceholder(
    gradientColors: List<Color>,
    initials: String,
    initialsSize: TextUnit,
    showInitials: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        // Concentric vinyl / soundwave ring accents
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .clip(CircleShape)
                .background(Color(0x1AFFFFFF))
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.52f)
                .clip(CircleShape)
                .background(Color(0x18000000))
        )

        if (showInitials && initials.isNotBlank()) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = initialsSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxSize(0.45f)
            )
        }
    }
}
