package com.ling.astrbot.feature.sessions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 会话页内的两个分区。 */
enum class SessionsTab(val label: String) {
    CHATS("会话"),
    PROJECTS("项目"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsTabSwitch(
    selected: SessionsTab,
    onSelect: (SessionsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        SessionsTab.entries.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = SessionsTab.entries.size,
                ),
                label = { Text(tab.label) },
            )
        }
    }
}
