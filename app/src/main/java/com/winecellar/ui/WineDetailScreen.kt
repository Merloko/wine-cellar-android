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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                title = { Text("Wine details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (wine != null) {
                        IconButton(onClick = { onToggleFavorite(wine) }) {
                            if (wine.favorite) {
                                Icon(Icons.Filled.Star, contentDescription = "Unfavourite")
                            } else {
                                Icon(Icons.Filled.StarBorder, contentDescription = "Favourite")
                            }
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
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
                    "Wine not found.",
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
                    StatusChip(text = style.label, color = style.accentColor())
                    StatusChip(text = status.label, color = status.color)
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
                    Text("Drink a bottle  (${wine.quantity} left)")
                }
            } else {
                Text(
                    "No bottles left in the cellar.",
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
                    Text("Where it is", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = wine.locationSummary ?: "No location recorded yet",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            // Details
            InfoCard {
                Text("Details", style = MaterialTheme.typography.titleMedium)
                DetailRow("Winery", wine.winery)
                DetailRow("Vintage", wine.vintage?.toString() ?: "Non-vintage")
                wine.name?.let { DetailRow("Cuvée / name", it) }
                wine.grapeType?.let { DetailRow("Grape / type", it.replace(";", ", ")) }
                DetailRow("Style", style.label)
                wine.originSummary?.let { DetailRow("Origin", it) }
                DetailRow("Bottles", wine.quantity.toString())
                wine.barcode?.let { DetailRow("Barcode", it) }
            }

            // Drinking window
            InfoCard {
                Text("Drinking window", style = MaterialTheme.typography.titleMedium)
                DetailRow("Best", window.label() ?: "—")
                DetailRow("Status", status.label)
                DetailRow(
                    "Source",
                    if (window.estimated) "Estimated from style & vintage" else "Recorded for this bottle",
                )
                wine.drinkWindowNote?.let { DetailRow("Note", it) }
            }

            wine.notes?.takeIf { it.isNotBlank() }?.let {
                InfoCard {
                    Text("Notes", style = MaterialTheme.typography.titleMedium)
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }

    if (confirmDelete && wine != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete wine?") },
            text = { Text("Remove \"${wine.displayTitle}\" from your cellar? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(wine) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
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
        title = { Text("Drink a bottle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text("Rate it (optional)", style = MaterialTheme.typography.labelLarge)
                Row {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = if (rating == star) 0 else star }) {
                            Icon(
                                if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "$star star",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Tasting note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(rating.takeIf { it > 0 }, note) }) { Text("Log it") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
