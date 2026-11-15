package com.passion.knownow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.passion.knownow.core.rendering.TableKind
import com.passion.knownow.core.rendering.TableModel

@Composable
fun AdaptiveTable(model: TableModel, modifier: Modifier = Modifier) {
    when (model.kind) {
        TableKind.PROPERTIES -> PropertyCards(model)
        TableKind.PARAMETERS -> ParameterCards(model)
        TableKind.NAV_CARDS -> NavCards(model)
        TableKind.GENERIC -> GenericTable(model)
    }
}

@Composable
private fun PropertyCards(model: TableModel) {
    Column(Modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        model.rows.forEach { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(row.getOrElse(0) { "" }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(row.getOrElse(1) { "" }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ParameterCards(model: TableModel) {
    Column(Modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        model.rows.forEach { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(row.getOrElse(0) { "" }, style = MaterialTheme.typography.titleSmall)
                        Text(row.getOrElse(1) { "" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(row.getOrElse(2) { "" }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun NavCards(model: TableModel) {
    Column(Modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        model.rows.forEach { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    row.forEach { cell ->
                        if (cell.isNotBlank()) Text(cell, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun GenericTable(model: TableModel) {
    Column(Modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (model.header.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                model.header.forEach { h ->
                    Text(h, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                }
            }
        }
        model.rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cell ->
                    Text(cell, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}