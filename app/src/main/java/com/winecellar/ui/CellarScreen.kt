package com.winecellar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.winecellar.R
import com.winecellar.domain.FilterState
import com.winecellar.domain.SortOrder
import com.winecellar.domain.WineStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellarScreen(
    state: CellarUiState,
    currentYear: Int,
    onQuery: (String) -> Unit,
    onLocation: (String?) -> Unit,
    onStyle: (WineStyle?) -> Unit,
    onWinery: (String?) -> Unit,
    onSort: (SortOrder) -> Unit,
    onClearFilters: () -> Unit,
    onOpenWine: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    Column(Modifier.fillMaxSize().padding(contentPadding)) {

        SearchField(query = state.filter.query, onQuery = onQuery)

        FilterRow(
            state = state,
            onLocation = onLocation,
            onStyle = onStyle,
            onWinery = onWinery,
            onSort = onSort,
            onClearFilters = onClearFilters,
        )

        if (state.wines.isEmpty()) {
            EmptyState(hasQuery = state.filter.query.isNotBlank() || state.filter.hasActiveFilters)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.wines, key = { it.id }) { wine ->
                    WineListItem(wine = wine, currentYear = currentYear, onClick = { onOpenWine(wine.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear_search))
                }
            }
        },
        singleLine = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    state: CellarUiState,
    onLocation: (String?) -> Unit,
    onStyle: (WineStyle?) -> Unit,
    onWinery: (String?) -> Unit,
    onSort: (SortOrder) -> Unit,
    onClearFilters: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Localised labels for every style (associateWith is inline, so the
        // stringResource calls run in this composable's context).
        val styleLabels = WineStyle.entries.associateWith { stringResource(it.labelRes()) }
        DropdownFilterChip(
            label = state.filter.location ?: stringResource(R.string.filter_location),
            selected = state.filter.location != null,
            options = state.locations,
            optionLabel = { it },
            onSelect = onLocation,
        )
        DropdownFilterChip(
            label = state.filter.style?.let { styleLabels.getValue(it) } ?: stringResource(R.string.filter_style),
            selected = state.filter.style != null,
            options = state.styles,
            optionLabel = { styleLabels.getValue(it) },
            onSelect = onStyle,
        )
        DropdownFilterChip(
            label = state.filter.winery ?: stringResource(R.string.filter_winery),
            selected = state.filter.winery != null,
            options = state.wineries,
            optionLabel = { it },
            onSelect = onWinery,
        )
        Box(Modifier.weight(1f))
        SortMenu(current = state.filter.sort, onSort = onSort)
    }
    if (state.filter.hasActiveFilters) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.cellar_count, state.wines.size, state.totalBottles),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = onClearFilters) { Text(stringResource(R.string.action_clear_filters)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownFilterChip(
    label: String,
    selected: Boolean,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filter_all)) },
                onClick = { onSelect(null); expanded = false },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SortMenu(current: SortOrder, onSort: (SortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.action_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(stringResource(order.labelRes())) },
                    onClick = { onSort(order); expanded = false },
                    trailingIcon = {
                        if (order == current) Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyState(hasQuery: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (hasQuery) stringResource(R.string.empty_no_match) else stringResource(R.string.empty_cellar),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}
