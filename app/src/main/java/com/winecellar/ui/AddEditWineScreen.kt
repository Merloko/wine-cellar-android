package com.winecellar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.winecellar.R
import com.winecellar.data.LookupOutcome
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
    onLookupBarcode: (barcode: String, onResult: (LookupOutcome) -> Unit) -> Unit = { _, _ -> },
    onCancel: () -> Unit,
    onSave: (Wine) -> Unit,
) {
    val isNew = initial == null
    val context = LocalContext.current

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
    // Transient (not rememberSaveable): a config change mid-lookup clears the
    // spinner rather than stranding it, since the callback is tied to this
    // composition.
    var lookingUp by remember { mutableStateOf(false) }

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
                title = { Text(stringResource(if (isNew) R.string.title_add_wine else R.string.title_edit_wine)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(buildWine()) }, enabled = canSave) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save))
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
            SectionLabel(stringResource(R.string.section_wine))
            Field(winery, { winery = it }, stringResource(R.string.field_winery), isError = winery.isBlank())
            Field(name, { name = it }, stringResource(R.string.field_name))
            Field(grape, { grape = it }, stringResource(R.string.field_grape), supporting = stringResource(R.string.field_grape_help))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(vintage, { vintage = it }, stringResource(R.string.field_vintage), Modifier.weight(1f), number = true, supporting = stringResource(R.string.field_vintage_help))
                Field(quantity, { quantity = it }, stringResource(R.string.field_bottles), Modifier.weight(1f), number = true)
            }

            SectionLabel(stringResource(R.string.section_where))
            SuggestField(location, { location = it }, stringResource(R.string.field_location), COMMON_LOCATIONS + knownLocations)
            SuggestField(shelf, { shelf = it }, stringResource(R.string.field_shelf), COMMON_SHELVES)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(rackColumn, { rackColumn = it }, stringResource(R.string.field_rack_column), Modifier.weight(1f), number = true)
                Field(rackRow, { rackRow = it }, stringResource(R.string.field_rack_row), Modifier.weight(1f), number = true)
            }

            SectionLabel(stringResource(R.string.section_origin))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(country, { country = it }, stringResource(R.string.field_country), Modifier.weight(1f))
                Field(region, { region = it }, stringResource(R.string.field_region), Modifier.weight(1f))
            }

            SectionLabel(stringResource(R.string.section_drink_window))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(drinkFrom, { drinkFrom = it }, stringResource(R.string.field_drink_from), Modifier.weight(1f), number = true)
                Field(drinkTo, { drinkTo = it }, stringResource(R.string.field_drink_to), Modifier.weight(1f), number = true)
            }
            Text(
                stringResource(R.string.drink_window_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionLabel(stringResource(R.string.section_extras))
            Field(barcode, { barcode = it }, stringResource(R.string.field_barcode), supporting = stringResource(R.string.field_barcode_help))
            if (barcode.isNotBlank()) {
                TextButton(
                    onClick = {
                        lookingUp = true
                        // App context: the callback returns on the retained VM scope.
                        val appContext = context.applicationContext
                        onLookupBarcode(barcode.trim()) { outcome ->
                            lookingUp = false
                            when (outcome) {
                                is LookupOutcome.Found -> {
                                    if (winery.isBlank()) outcome.result.winery?.let { winery = it }
                                    if (name.isBlank()) outcome.result.name?.let { name = it }
                                    Toast.makeText(appContext, appContext.getString(R.string.lookup_filled), Toast.LENGTH_SHORT).show()
                                }
                                LookupOutcome.NotFound ->
                                    Toast.makeText(appContext, appContext.getString(R.string.lookup_not_found), Toast.LENGTH_SHORT).show()
                                LookupOutcome.Error ->
                                    Toast.makeText(appContext, appContext.getString(R.string.lookup_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !lookingUp,
                ) {
                    if (lookingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.lookup_searching))
                    } else {
                        Text(stringResource(R.string.action_lookup_online))
                    }
                }
            }
            Field(notes, { notes = it }, stringResource(R.string.field_notes), singleLine = false)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.label_favourite), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
