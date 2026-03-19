package com.yuerchu.remoteask.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yuerchu.remoteask.data.model.QuestionEntity
import com.yuerchu.remoteask.ui.theme.AnsweredGreen
import com.yuerchu.remoteask.ui.theme.PendingOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuestionCard(
    question: QuestionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPending = question.status == "pending"

    val containerColor by animateColorAsState(
        targetValue = if (isPending) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = spring(),
        label = "cardColor"
    )

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPending) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isPending) Icons.Rounded.HelpOutline else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = if (isPending) PendingOrange else AnsweredGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isPending) "待回答" else "已回答",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isPending) PendingOrange else AnsweredGreen
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatTime(question.receivedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (question.answer != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "回答：${question.answer}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
