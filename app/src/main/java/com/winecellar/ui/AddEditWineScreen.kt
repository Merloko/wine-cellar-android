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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.winecellar.data.Wine

private val COMMON_LOCATIONS = listOf("Cellar", "Fridge", "Front Fridge", "Downstairs Fridge")
private val COMMON_SHELVES = listOf("Top", "Second", "Third", "Bottom", "On Top")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWineScreen(
    initial: Wine?,
    knownLocations: List<String>,
    knownWineries: List<String>,
    initialBarcode: String? = null,
    onCancel: () -> Unit,
    onSave: (Wine) -> Unit,
) {
    val isNew = initial == null

    var winery by rememberSaveable { mutableStateOf(initial?.winery ?: "") }
    var vintage by rememberSaveable { mutableStateOf(initial?.vintage?.toString() ?: "") }
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var grape by rememberSaveable { mutableStateOf(initial?.grapeType ?: "") }
    var location by rememberSaveable { mutableStateOf(initial?.location ?: "") }
    var shelf by rememberSaveable { mutableStateOf(initial?.shelf ?: "") }
    var rackColumn by rememberSaveable { mutableStateOf(initial?.rackColumn?.toString() ?: "") }
    var rackRow by rememberSaveable { mutableStateOf(initial?.rackRow?.toString() ?: "") }
    var country by rememberSaveable { mutableStateOf(initial?.country ?: "") }
    var region by rememberSaveable { mutableStateOf(initial?.region ?: "") }
    var quantity by rememberSaveable { mutableStateOf(initial?.quantity?.toString() ?: "1") }
    var drinkFrom by rememberSaveable { mutableStateOf(initial?.drinkFrom?.toString() ?: "") }
    var drinkTo by rememberSaveable { mutableStateOf(initial?.drinkTo?.toString() ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }
    var barcode by rememberSaveable { mutableStateOf(initial?.barcode ?: initialBarcode ?: "") }
    var favorite by rememberSaveable { mutableStateOf(initial?.favorite ?: false) }

    val canSave = winery.isNotBlank()

    fun buildWine(): Wine = (initial ?: Wine(winery = "")).copy(
        winery = winery.trim(),
        vintage = vintage.trim().toIntOrNull(),
        name = name.trim().ifBlank { null },
        grapeType = grape.trim().ifBlank { null },
        location = location.trim().ifBlank { null },
        shelf = shelf.trim().ifBlank { null },
        rackColumn = rackColumn.trim().toIntOrNull(),
        rackRow = rackRow.trim().toIntOrNull(),
        country = country.trim().ifBlank { null },
        region = region.trim().ifBlank { null },
        quantity = quantity.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1,
        drinkFrom = drinkFrom.trim().toIntOrNull(),
        drinkTo = drinkTo.trim().toIntOrNull(),
        notes = notes.trim().ifBlank { null },
        barcode = barcode.trim().ifBlank { null },
        favorite = favorite,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add wine" else "Edit wine") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(buildWine()) }, enabled = canSave) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("Wine")
            Field(winery, { winery = it }, "Winery / vineyard *", isError = winery.isBlank())
            Field(name, { name = it }, "Cuvée / name (optional)")
            Field(grape, { grape = it }, "Grape / type", supporting = "Separate a blend with ;")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(vintage, { vintage = it }, "Vintage year", Modifier.weight(1f), number = true, supporting = "Blank = NV")
                Field(quantity, { quantity = it }, "Bottles", Modifier.weight(1f), number = true)
            }

            SectionLabel("Where it lives")
            SuggestField(location, { location = it }, "Location", COMMON_LOCATIONS + knownLocations)
            SuggestField(shelf, { shelf = it }, "Shelf", COMMON_SHELVES)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(rackColumn, { rackColumn = it }, "Rack column", Modifier.weight(1f), number = true)
                Field(rackRow, { rackRow = it }, "Rack row", Modifier.weight(1f), number = true)
            }

            SectionLabel("Origin")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(country, { country = it }, "Country", Modifier.weight(1f))
                Field(region, { region = it }, "Region", Modifier.weight(1f))
            }

            SectionLabel("Drinking window (optional)")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(drinkFrom, { drinkFrom = it }, "Drink from", Modifier.weight(1f), number = true)
                Field(drinkTo, { drinkTo = it }, "Drink to", Modifier.weight(1f), number = true)
            }
            Text(
                "Leave blank to let the app estimate from the grape and vintage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionLabel("Extras")
            Field(barcode, { barcode = it }, "Barcode", supporting = "Scanned automatically when you add via the scanner")
            Field(notes, { notes = it }, "Notes", singleLine = false)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Favourite", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = favorite, onCheckedChange = { favorite = it })
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    number: Boolean = false,
    isError: Boolean = false,
    singleLine: Boolean = true,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        isError = isError,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        supportingText = supporting?.let { { Text(it) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = suggestions.distinct()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it); expanded = true },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        val filtered = options.filter { it.contains(value, ignoreCase = true) || value.isBlank() }
        if (filtered.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                filtered.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onChange(option); expanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}
