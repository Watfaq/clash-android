package rs.clash.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatsCard(
	title: String,
	value: String,
	modifier: Modifier = Modifier,
	subtitle: String = "",
	containerColor: Color? = null,
	onClick: (() -> Unit)? = null,
) {
	Card(
		modifier =
			modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = "$title: $value${if (subtitle.isNotEmpty()) ", $subtitle" else ""}"
				},
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
		colors =
			if (containerColor != null) {
				CardDefaults.cardColors(containerColor = containerColor)
			} else {
				CardDefaults.cardColors()
			},
		onClick = onClick ?: {},
	) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(4.dp),
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.secondary,
			)
			Text(
				text = value,
				style = MaterialTheme.typography.headlineMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.primary,
			)
			if (subtitle.isNotEmpty()) {
				Text(
					text = subtitle,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.outline,
				)
			}
		}
	}
}
