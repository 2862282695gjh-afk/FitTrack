package com.fittrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrack.ui.theme.FitBlue
import com.fittrack.ui.theme.FitGreen
import com.fittrack.ui.theme.FitOrange
import com.fittrack.ui.theme.FitPurple

@Composable
fun GoalChip(label: String, modifier: Modifier = Modifier) {
    val color = when (label) {
        "增肌" -> FitGreen
        "减脂" -> FitBlue
        "力量" -> FitOrange
        "耐力" -> FitPurple
        else -> MaterialTheme.colorScheme.primary
    }
    SuggestionChip(
        onClick = { },
        modifier = modifier,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.12f)
        ),
        border = null
    )
}

@Composable
fun StatusChip(text: String, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

fun getGoalLabel(goal: String): String {
    return when (goal) {
        "muscle_gain" -> "增肌"
        "fat_loss" -> "减脂"
        "strength" -> "力量"
        "endurance" -> "耐力"
        else -> "综合"
    }
}
