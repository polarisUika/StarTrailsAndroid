package com.startrails.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.startrails.core.designsystem.theme.Spacing

/** 首次启动时展示一次存储与隐私说明，确认后不再出现。 */
@Composable
fun PrivacyNoticeDialog(onAcknowledge: () -> Unit) {
    AlertDialog(
        onDismissRequest = onAcknowledge,
        icon = {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("存储与隐私") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                PrivacyLine(
                    icon = Icons.Filled.Lock,
                    text = "原片始终以只读方式访问，不会被覆盖",
                )
                PrivacyLine(
                    icon = Icons.Filled.Save,
                    text = "结果先写入应用私有目录，成功后才发布到相册或你指定的位置",
                )
                PrivacyLine(
                    icon = Icons.Filled.WifiOff,
                    text = "数据只在本地处理",
                )
                PrivacyLine(
                    icon = Icons.Filled.Folder,
                    text = "重启后仍可访问已授权的文件夹；授权失效时需要重新选择",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) { Text("我知道了") }
        },
    )
}

@Composable
private fun PrivacyLine(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
