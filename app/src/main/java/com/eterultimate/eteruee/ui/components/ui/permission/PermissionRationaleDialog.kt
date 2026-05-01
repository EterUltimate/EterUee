package com.eterultimate.eteruee.ui.components.ui.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.AlertCircle
import com.eterultimate.eteruee.R
import androidx.compose.ui.graphics.RectangleShape

/**
 * 鏉冮檺璇锋眰璇存槑瀵硅瘽妗?
 */
@Composable
internal fun PermissionRationaleDialog(
    permissions: List<PermissionInfo>,
    permanentlyDeniedPermissions: List<PermissionInfo>,
    onProceed: () -> Unit,
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onCancel,
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RectangleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 鏍囬鍥炬爣
                Icon(
                    imageVector = HugeIcons.AlertCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 鏍囬
                val hasPermanentlyDenied = permanentlyDeniedPermissions.isNotEmpty()
                Text(
                    text = stringResource(R.string.permission_diaog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 璇存槑鏂囧瓧
                Text(
                    text = if (hasPermanentlyDenied) {
                        stringResource(R.string.permission_desc_goto_setting)
                    } else {
                        stringResource(R.string.permission_desc_require_permission)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 鏉冮檺鍒楄〃
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(permissions) { permissionInfo ->
                        PermissionItem(
                            permissionInfo = permissionInfo,
                            isPermanentlyDenied = permanentlyDeniedPermissions.contains(permissionInfo),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 鎸夐挳缁?
                if (hasPermanentlyDenied) {
                    // 鏈夋案涔呮嫆缁濈殑鏉冮檺锛屽彧鏄剧ず鍓嶅線璁剧疆鍜屽彇娑堟寜閽?
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = onProceed, // 杩欓噷浼氳烦杞埌璁剧疆
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.permission_go_to_settings))
                        }
                    }
                } else {
                    // 娌℃湁姘镐箙鎷掔粷鐨勬潈闄愶紝鏄剧ず姝ｅ父鐨勬巿鏉冩寜閽?
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = onProceed,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 鍗曚釜鏉冮檺椤圭粍浠?
 */
@Composable
private fun PermissionItem(
    permissionInfo: PermissionInfo,
    modifier: Modifier = Modifier,
    isPermanentlyDenied: Boolean = false,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RectangleShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 鏉冮檺鍚嶇О
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ProvideTextStyle(value = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)) {
                        permissionInfo.displayName()
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (permissionInfo.required) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RectangleShape
                        ) {
                            Text(
                                text = stringResource(R.string.permission_required),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isPermanentlyDenied) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            ),
                            shape = RectangleShape
                        ) {
                            Text(
                                text = stringResource(R.string.permission_permanently_denied),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 鏉冮檺浣跨敤璇存槑
            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                permissionInfo.usage()
            }
        }
    }
}

