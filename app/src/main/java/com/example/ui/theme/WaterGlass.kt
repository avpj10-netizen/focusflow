package com.example.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Liquid water glass refraction colors
val WaterGlassPrimary = Color(0xFF38BDF8) // Aqua / Cyan light reflection
val WaterGlassFill = Color(0x1F2A3B53) // Translucent liquid slate
val WaterGlassBorder = Color(0x35FFFFFF) // Frosted glass rim
val WaterGlassHighlight = Color(0x55FFFFFF) // Specular highlight rim

val WaterBackdropBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F172A), // Midnight slate
        Color(0xFF090E17), // Deep oceanic navy
        Color(0xFF05080F)  // Abyss
    )
)

val LiquidCardBrush = Brush.linearGradient(
    colors = listOf(
        Color(0x28FFFFFF),
        Color(0x12FFFFFF),
        Color(0x1C38BDF8)
    ),
    start = Offset(0f, 0f),
    end = Offset(400f, 800f)
)

val WaterBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color(0x66FFFFFF),
        Color(0x1AFFFFFF),
        Color(0x4038BDF8)
    ),
    start = Offset(0f, 0f),
    end = Offset(400f, 600f)
)

/**
 * Water translucent frosted card with liquid glass border reflections
 */
@Composable
fun WaterGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = shape,
        color = Color(0x1C1E293B), // Translucent water fill
        shadowElevation = 6.dp,
        modifier = modifier
            .border(1.dp, WaterBorderBrush, shape)
            .background(LiquidCardBrush, shape)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * True iOS-Style Floating Water-Translucent Tab Bar
 * - Translucent liquid glass blur container
 * - Hairline specular border
 * - No M3 horizontal pills
 * - iOS icon/label layout with spring bounce and cyan/indigo active tint
 */
@Composable
fun <T> IosWaterTabBar(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    getIcon: (T, Boolean) -> ImageVector,
    getLabel: (T) -> String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("ios_water_tab_bar")
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xD90F172A), // Translucent liquid frosted glass
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = WaterBorderBrush,
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp)
            ) {
                items.forEach { item ->
                    val isSelected = item == selectedItem
                    val label = getLabel(item)
                    val icon = getIcon(item, isSelected)

                    val animatedScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "tab_scale"
                    )

                    val activeTint = Color(0xFF38BDF8) // iOS Liquid Aqua
                    val inactiveTint = Color(0x9994A3B8) // Muted slate

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onItemSelected(item)
                            }
                            .testTag("ios_tab_${label.lowercase().replace(" ", "_")}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                // Subtle liquid glow dot behind active icon
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(activeTint.copy(alpha = 0.15f), CircleShape)
                                )
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) activeTint else inactiveTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) activeTint else inactiveTint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
