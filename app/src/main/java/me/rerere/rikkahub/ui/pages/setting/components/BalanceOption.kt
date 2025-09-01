package me.rerere.rikkahub.ui.pages.setting.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.rerere.ai.provider.BalanceOption

private val JsonKeyRegex = Regex("""^[^.\s\[\]]+(?:\.[^.\s\[\]]+)*$""")
private val ApiPathRegex = Regex("""^/[^ \t\n\r]*$""")

@Composable
fun SettingProviderBalanceOption(
    balanceOption: BalanceOption,
    modifier: Modifier = Modifier,
    onEdit: (BalanceOption) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "从API获取账号余额信息",
                modifier = Modifier.weight(1f),
            )
            Checkbox(
                checked = balanceOption.enabled,
                onCheckedChange = { onEdit(balanceOption.copy(enabled = it)) }
            )
        }
        AnimatedVisibility(balanceOption.enabled) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = balanceOption.apiPath,
                    onValueChange = { onEdit(balanceOption.copy(apiPath = it)) },
                    label = { Text("余额API路径") },
                    isError = !balanceOption.apiPath.matches(ApiPathRegex),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balanceOption.resultPath,
                    onValueChange = { onEdit(balanceOption.copy(resultPath = it)) },
                    label = { Text("余额结果JSON Key") },
                    isError = !balanceOption.resultPath.matches(JsonKeyRegex),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview
@Composable
private fun BalanceOptionPreview() {
    var balanceOption by remember { mutableStateOf(BalanceOption()) }
    Surface {
        SettingProviderBalanceOption(
            balanceOption = balanceOption,
            onEdit = { balanceOption = it }
        )
    }
}
