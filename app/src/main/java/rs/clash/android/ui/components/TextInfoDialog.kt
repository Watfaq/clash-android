package rs.clash.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.clash.android.R

/**
 * Simple dialog to display text information
 */
@Composable
fun TextInfoDialog(
	title: String,
	content: String,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = {
			Text(
				text = title,
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold,
			)
		},
		text = {
			Column(
				modifier =
					Modifier
						.fillMaxWidth()
						.verticalScroll(rememberScrollState()),
			) {
				Text(
					text = content,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(R.string.confirm))
			}
		},
		modifier = modifier,
	)
}

/**
 * Dialog to display multiple sections of text
 */
@Composable
fun MultiSectionTextDialog(
	title: String,
	sections: List<Pair<String, String>>, // List of (section title, section content)
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = {
			Text(
				text = title,
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold,
			)
		},
		text = {
			Column(
				modifier =
					Modifier
						.fillMaxWidth()
						.verticalScroll(rememberScrollState()),
			) {
				sections.forEachIndexed { index, (sectionTitle, sectionContent) ->
					if (index > 0) {
						Spacer(modifier = Modifier.height(16.dp))
					}
					
					Text(
						text = sectionTitle,
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.SemiBold,
						color = MaterialTheme.colorScheme.primary,
					)
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = sectionContent,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(R.string.confirm))
			}
		},
		modifier = modifier,
	)
}
