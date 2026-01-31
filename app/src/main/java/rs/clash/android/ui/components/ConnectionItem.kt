package rs.clash.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import rs.clash.android.R
import rs.clash.android.formatSize
import uniffi.clash_android_ffi.Connection

@Composable
fun ConnectionItem(
	connection: Connection,
	modifier: Modifier = Modifier,
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
	) {
		Column(
			modifier = Modifier.padding(12.dp),
			verticalArrangement = Arrangement.spacedBy(6.dp),
		) {
			// Host and Type
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = connection.metadata.host.ifEmpty { connection.metadata.destinationIp },
					style = MaterialTheme.typography.titleSmall,
					fontWeight = FontWeight.Bold,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f),
				)
				Text(
					text = connection.metadata.network.uppercase(),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.secondary,
				)
			}

			// Destination
			Text(
				text = "${connection.metadata.destinationIp}:${connection.metadata.destinationPort}",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.outline,
			)

			// Source
			Text(
				text = stringResource(R.string.connections_source, connection.metadata.sourceIp),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.outline,
			)

			// Chains (proxy chain)
			if (connection.chains.isNotEmpty()) {
				Text(
					text = stringResource(R.string.connections_chain, connection.chains.joinToString(" → ")),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.secondary,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
			}

			// Rule
			if (connection.rule.isNotEmpty()) {
				Text(
					text = stringResource(R.string.connections_rule, connection.rule),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.tertiary,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
			}

			// Traffic
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				Text(
					text = stringResource(R.string.connections_download, formatSize(connection.download)),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.primary,
				)
				Text(
					text = stringResource(R.string.connections_upload, formatSize(connection.upload)),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.primary,
				)
			}
		}
	}
}
