package com.winecellar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winecellar.R
import com.winecellar.data.Wine
import com.winecellar.domain.DrinkWindowCalculator
import com.winecellar.domain.WineStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WineDetailScreen(
    wine: Wine?,
    currentYear: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (Wine) -> Unit,
    onToggleFavorite: (Wine) -> Unit,
    onDrink: (rating: Int?, note: String?) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showDrink by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_wine_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (wine != null) {
                        IconButton(onClick = { onToggleFavorite(wine) }) {
                            if (wine.favorite) {
                                Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.action_unfavourite))
                            } else {
                                Icon(Icons.Filled.StarBorder, contentDescription = stringResource(R.string.cd_favourite))
                            }
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        if (wine == null) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                Text(
                    stringResource(R.string.wine_not_found),
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        val style = WineStyle.classify(wine.grapeType, wine.name)
        val window = DrinkWindowCalculator.windowFor(wine)
        val status = DrinkWindowCalculator.statusFor(wine, currentYear).visual()

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Headline
            Column {
                Text(wine.displayTitle, style = MaterialTheme.typography.headlineSmall)
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusChip(text = stringResource(style.labelRes()), color = style.accentColor())
                    StatusChip(text = stringResource(status.labelRes), color = status.color)
                }
            }

            // Drink a bottle — logs it to history and decrements the count.
            if (wine.quantity > 0) {
                Button(
                    onClick = { showDrink = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.LocalBar, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_drink_bottle, wine.quantity))
                }
            } else {
                Text(
                    stringResource(R.string.no_bottles_left),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Where it is — the headline feature.
            InfoCard(highlight = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.detail_where), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = wine.locationSummary ?: stringResource(R.string.detail_no_location),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            // Details
            InfoCard {
                Text(stringResource(R.string.detail_details), style = MaterialTheme.typography.titleMedium)
                DetailRow(stringResource(R.string.detail_winery), wine.winery)
                DetailRow(stringResource(R.string.detail_vintage), wine.vintage?.toString() ?: stringResource(R.string.detail_non_vintage))
                wine.name?.let { DetailRow(stringResource(R.string.detail_name), it) }
                wine.grapeType?.let { DetailRow(stringResource(R.string.detail_grape), it.replace(";", ", ")) }
                DetailRow(stringResource(R.string.detail_style), stringResource(style.labelRes()))
                wine.originSummary?.let { DetailRow(stringResource(R.string.detail_origin), it) }
                DetailRow(stringResource(R.string.detail_bottles), wine.quantity.toString())
                wine.barcode?.let { DetailRow(stringResource(R.string.detail_barcode), it) }
            }

            // Drinking window
            InfoCard {
                Text(stringResource(R.string.detail_drink_window), style = MaterialTheme.typography.titleMedium)
                DetailRow(stringResource(R.string.detail_best), window.label() ?: stringResource(R.string.status_unknown))
                DetailRow(stringResource(R.string.detail_status), stringResource(status.labelRes))
                DetailRow(
                    stringResource(R.string.detail_source),
                    stringResource(if (window.estimated) R.string.source_estimated else R.string.source_recorded),
                )
                wine.drinkWindowNote?.let { DetailRow(stringResource(R.string.detail_note), it) }
            }

            wine.notes?.takeIf { it.isNotBlank() }?.let {
                InfoCard {
                    Text(stringResource(R.string.detail_notes), style = MaterialTheme.typography.titleMedium)
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }

    if (confirmDelete && wine != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_msg, wine.displayTitle)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(wine) }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showDrink && wine != null) {
        DrinkDialog(
            title = wine.displayTitle,
            onDismiss = { showDrink = false },
            onConfirm = { rating, note ->
                showDrink = false
                onDrink(rating, note)
            },
        )
    }
}

@Composable
private fun DrinkDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (rating: Int?, note: String?) -> Unit,
) {
    var rating by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_drink_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.dialog_rate), style = MaterialTheme.typography.labelLarge)
                Row {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = if (rating == star) 0 else star }) {
                            Icon(
                                if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = stringResource(R.string.cd_star, star),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.field_tasting_note)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(rating.takeIf { it > 0 }, note) }) { Text(stringResource(R.string.action_log_it)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun InfoCard(
    highlight: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
