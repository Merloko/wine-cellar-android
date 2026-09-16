package com.winecellar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.winecellar.R
import com.winecellar.domain.CellarStats
import com.winecellar.ui.theme.StatusPast
import com.winecellar.ui.theme.StatusReady
import com.winecellar.ui.theme.StatusYoung

@Composable
fun StatsScreen(
    stats: CellarStats,
    contentPadding: PaddingValues,
) {
    if (stats.isEmpty) {
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.stats_empty),
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.stats_bottles), stats.totalBottles.toString(), Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_distinct), stats.distinctWines.toString(), Modifier.weight(1f))
            }
        }

        item {
            SectionCard(stringResource(R.string.stats_ready_section)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MiniStat(stringResource(R.string.stats_now), stats.readyNow, StatusReady, Modifier.weight(1f))
                    MiniStat(stringResource(R.string.stats_cellaring), stats.cellaring, StatusYoung, Modifier.weight(1f))
                    MiniStat(stringResource(R.string.stats_past), stats.pastPeak, StatusPast, Modifier.weight(1f))
                }
            }
        }

        if (stats.oldestVintage != null && stats.newestVintage != null) {
            item {
                SectionCard(stringResource(R.string.stats_vintage_range)) {
                    Text(
                        "${stats.oldestVintage} – ${stats.newestVintage}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (stats.byStyle.isNotEmpty()) {
            item {
                val max = stats.byStyle.maxOf { it.second }
                SectionCard(stringResource(R.string.stats_by_style)) {
                    stats.byStyle.forEach { (style, count) ->
                        BarRow(stringResource(style.labelRes()), count, max, style.accentColor())
                    }
                }
            }
        }

        if (stats.byCountry.isNotEmpty()) {
            item {
                val max = stats.byCountry.maxOf { it.bottles }
                SectionCard(stringResource(R.string.stats_by_country)) {
                    stats.byCountry.forEach { bucket -> BarRow(bucket.label, bucket.bottles, max) }
                }
            }
        }

        if (stats.topWineries.isNotEmpty()) {
            item {
                val max = stats.topWineries.maxOf { it.bottles }
                SectionCard(stringResource(R.string.stats_top_wineries)) {
                    stats.topWineries.forEach { bucket -> BarRow(bucket.label, bucket.bottles, max) }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.SemiBold)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun BarRow(label: String, count: Int, max: Int, color: Color = MaterialTheme.colorScheme.primary) {
    val fraction = if (max > 0) count.toFloat() / max else 0f
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}
