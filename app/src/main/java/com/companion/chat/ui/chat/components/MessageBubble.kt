package com.companion.chat.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.MessageRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    var showFullScreenImage by remember { mutableStateOf<android.net.Uri?>(null) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            AvatarIcon(isUser = false)
            Column(
                modifier = Modifier.padding(start = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                BubbleContent(
                    message = message,
                    isUser = false,
                    onImageClick = { showFullScreenImage = it }
                )
            }
        }

        if (isUser) {
            Column(
                modifier = Modifier.padding(end = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                BubbleContent(
                    message = message,
                    isUser = true,
                    onImageClick = { showFullScreenImage = it }
                )
            }
            AvatarIcon(isUser = true)
        }
    }

    showFullScreenImage?.let { uri ->
        AlertDialog(
            onDismissRequest = { showFullScreenImage = null },
            confirmButton = {
                TextButton(onClick = { showFullScreenImage = null }) {
                    Text("关闭")
                }
            },
            text = {
                AsyncImage(
                    model = uri,
                    contentDescription = "图片预览",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        )
    }
}

@Composable
private fun AvatarIcon(isUser: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                if (isUser) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
            contentDescription = if (isUser) "用户" else "AI",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BubbleContent(
    message: ChatMessage,
    isUser: Boolean,
    onImageClick: (android.net.Uri) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 16.dp
        ),
        color = if (isUser) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (message.images.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    message.images.forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "消息图片",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClick(uri) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            val contentText = if (message.content.isEmpty() && message.isStreaming) {
                "正在输入..."
            } else {
                message.content
            }
            val contentColor = if (isUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
            if (isUser) {
                Text(
                    text = contentText,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                MarkdownMessageText(
                    text = contentText,
                    color = contentColor
                )
            }

            if (!message.isStreaming) {
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (isUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondaryContainer).copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
