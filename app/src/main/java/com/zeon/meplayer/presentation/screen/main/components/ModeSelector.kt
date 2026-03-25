package com.zeon.meplayer.presentation.screen.main.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zeon.meplayer.R
import com.zeon.meplayer.presentation.screen.main.model.MainContentMode

@Composable
fun ModeSelector(
    selectedMode: MainContentMode,
    onModeSelected: (MainContentMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.wrapContentWidth()
        ) {
            val colors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primary,
                activeContentColor = MaterialTheme.colorScheme.onPrimary
            )

            SegmentedButton(
                selected = selectedMode == MainContentMode.SONGS,
                onClick = { onModeSelected(MainContentMode.SONGS) },
                label = { Text(stringResource(R.string.all_songs)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = colors,
                icon = {}
            )
            SegmentedButton(
                selected = selectedMode == MainContentMode.PLAYLISTS,
                onClick = { onModeSelected(MainContentMode.PLAYLISTS) },
                label = { Text(stringResource(R.string.playlists)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = colors,
                icon = {}
            )
        }
    }
}