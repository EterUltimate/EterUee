package me.rerere.rikkahub.ui.components.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Coins
import com.composables.icons.lucide.Lucide
import me.rerere.ai.provider.ProviderManager
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.utils.toDp
import org.koin.compose.koinInject

@Composable
fun ProviderBalanceText(
    providerSetting: ProviderSetting,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    if (!providerSetting.balanceOption.enabled || providerSetting !is ProviderSetting.OpenAI) {
        // Balance option is disabled or provider is not OpenAI type
        return
    }

    val providerManager = koinInject<ProviderManager>()

    val value = produceState(initialValue = "", key1 = providerSetting) {
        // Fetch balance from API
        runCatching {
            value = providerManager.getProviderByType(providerSetting).getBalance(providerSetting)
        }.onFailure {
            // Handle error
            value = "Error: ${it.message}"
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Lucide.Coins,
            contentDescription = null,
            modifier = Modifier.size(style.fontSize.toDp())
        )
        Text(
            text = value.value,
            style = style,
        )
    }
}
