package com.zeon.meplayer.presentation.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zeon.meplayer.R
import com.zeon.meplayer.domain.model.SortOption
import com.zeon.meplayer.presentation.theme.ThemeMode
import com.zeon.meplayer.presentation.viewmodel.ThemeViewModel
import com.zeon.meplayer.presentation.viewmodel.rememberSortViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel
) {
    val sortViewModel = rememberSortViewModel()
    val themeMode by themeViewModel.themeMode.collectAsState()
    val sortOption by sortViewModel.sortOption.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            item {
                SettingsCard {
                    SettingsSection(
                        title = stringResource(R.string.theme_mode),
                        content = {
                            ThemeModeChips(
                                selectedMode = themeMode,
                                onModeSelected = { themeViewModel.setThemeMode(it) }
                            )
                        }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsSection(
                        title = stringResource(R.string.sort_by),
                        content = {
                            SortOptionsChips(
                                selectedSort = sortOption,
                                onSortSelected = { sortViewModel.setSortOption(it) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        content()
    }
}

@Composable
fun ThemeModeChips(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ThemeModeChip(
            selected = selectedMode == ThemeMode.LIGHT,
            onClick = { onModeSelected(ThemeMode.LIGHT) },
            icon = Icons.Default.WbSunny,
            label = stringResource(R.string.light_theme),
            modifier = Modifier.weight(1f)
        )
        ThemeModeChip(
            selected = selectedMode == ThemeMode.DARK,
            onClick = { onModeSelected(ThemeMode.DARK) },
            icon = Icons.Default.DarkMode,
            label = stringResource(R.string.dark_theme),
            modifier = Modifier.weight(1f)
        )
        ThemeModeChip(
            selected = selectedMode == ThemeMode.AUTO,
            onClick = { onModeSelected(ThemeMode.AUTO) },
            icon = Icons.Default.PhoneAndroid,
            label = stringResource(R.string.auto_theme),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ThemeModeChip(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun SortOptionsChips(
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SortOptionChip(
            selected = selectedSort == SortOption.TITLE,
            onClick = { onSortSelected(SortOption.TITLE) },
            icon = Icons.Default.SortByAlpha,
            label = stringResource(R.string.sort_by_title)
        )
        SortOptionChip(
            selected = selectedSort == SortOption.ARTIST,
            onClick = { onSortSelected(SortOption.ARTIST) },
            icon = Icons.Default.Person,
            label = stringResource(R.string.sort_by_artist)
        )
        SortOptionChip(
            selected = selectedSort == SortOption.DURATION,
            onClick = { onSortSelected(SortOption.DURATION) },
            icon = Icons.Default.Timer,
            label = stringResource(R.string.sort_by_duration)
        )
        SortOptionChip(
            selected = selectedSort == SortOption.DATE_ADDED,
            onClick = { onSortSelected(SortOption.DATE_ADDED) },
            icon = Icons.Default.CalendarToday,
            label = stringResource(R.string.sort_by_date_added)
        )
    }
}

@Composable
fun SortOptionChip(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}