package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.theme.FreshTextVariant

@Composable
fun AiTrainingHeader(
    @Suppress("UNUSED_PARAMETER") icon: String,
    @Suppress("UNUSED_PARAMETER") title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = description,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = FreshTextVariant
    )
}
