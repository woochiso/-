package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.FreshDeepIndigo

@Composable
fun ChildNavigationBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showWoochisoBrand: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "$title 이전 화면")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (showWoochisoBrand) Modifier.weight(1f) else Modifier
        )
        if (showWoochisoBrand) {
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.img_uchiso_app_icon_1785309286513),
                contentDescription = "우치소 로고",
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "우치소",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FreshDeepIndigo,
                maxLines = 1
            )
            Spacer(Modifier.width(12.dp))
        }
    }
}
