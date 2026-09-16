package com.winecellar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.winecellar.R
import com.winecellar.data.Wine
import com.winecellar.ui.theme.StatusPast
import com.winecellar.ui.theme.StatusReady
import com.winecellar.ui.theme.StatusSoon

@Composable
fun DrinkNowScreen(
    state: DrinkNowState,
    currentYear: Int,
    onOpenWine: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val empty = state.pastPeak.isEmpty() && state.ready.isEmpty() && state.soon.isEmpty()
    if (empty) {
        Box(
            Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.drinknow_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        section(
            titleRes = R.string.drinknow_past_title,
            subtitleRes = R.string.drinknow_past_sub,
            color = StatusPast,
            wines = state.pastPeak,
            currentYear = currentYear,
            onOpenWine = onOpenWine,
        )
        section(
            titleRes = R.string.drinknow_ready_title,
            subtitleRes = R.string.drinknow_ready_sub,
            color = StatusReady,
            wines = state.ready,
            currentYear = currentYear,
            onOpenWine = onOpenWine,
        )
        section(
            titleRes = R.string.drinknow_soon_title,
            subtitleRes = R.string.drinknow_soon_sub,
            color = StatusSoon,
            wines = state.soon,
            currentYear = currentYear,
            onOpenWine = onOpenWine,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    @StringRes titleRes: Int,
    @StringRes subtitleRes: Int,
    color: androidx.compose.ui.graphics.Color,
    wines: List<Wine>,
    currentYear: Int,
    onOpenWine: (Long) -> Unit,
) {
    if (wines.isEmpty()) return
    item(key = "header-$titleRes") {
        Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.stats_section_count, stringResource(titleRes), wines.size),
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(wines, key = { "wine-${it.id}" }) { wine ->
        WineListItem(wine = wine, currentYear = currentYear, onClick = { onOpenWine(wine.id) })
    }
}
