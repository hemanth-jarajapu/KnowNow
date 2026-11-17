package com.passion.knownow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.passion.knownow.core.rendering.TableKind
import com.passion.knownow.core.rendering.TableModel

/**
 * Reference tables, rendered for a phone rather than a desktop.
 *
 * The first column is treated as the label of a definition and gets a fixed 40% share, the
 * remaining cells flow as prose. That keeps "Field | Description" tables readable instead of
 * the even 50/50 split a generic markdown renderer produces.
 */
@Composable
fun AdaptiveTable(model: TableModel, modifier: Modifier = Modifier) {
    if (model.rows.isEmpty() && model.header.isEmpty()) return
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            if (model.header.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
                    Text(
                        model.header.first(),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(LABEL_WEIGHT),
                    )
                    Text(
                        model.header.drop(1).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(VALUE_WEIGHT),
                    )
                }
                HorizontalDivider(
                    color = scheme.outlineVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            model.rows.forEachIndexed { index, row ->
                TableRow(row)
                if (index != model.rows.lastIndex) {
                    HorizontalDivider(
                        color = scheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TableRow(row: List<String>) {
    val scheme = MaterialTheme.colorScheme
    val label = row.firstOrNull().orEmpty()
    val type = if (row.size >= 3) row[1] else ""
    val value = if (row.size >= 3) {
        row.drop(2).filter { it.isNotBlank() }.joinToString(" · ")
    } else {
        row.getOrElse(1) { "" }
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(LABEL_WEIGHT)) {
            if (label.isNotBlank()) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
            }
            if (type.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    type,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (value.isBlank()) "—" else value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isBlank()) scheme.onSurfaceVariant else scheme.onSurface,
            modifier = Modifier.weight(VALUE_WEIGHT),
        )
    }
}

private const val LABEL_WEIGHT = 0.40f
private const val VALUE_WEIGHT = 0.60f
