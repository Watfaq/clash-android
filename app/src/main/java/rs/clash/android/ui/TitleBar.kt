package rs.clash.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TitleBar(
	modifier: Modifier = Modifier,
	buttons: (@Composable () -> Unit)? = null,
	title: String,
) {
	Row(
		modifier =
			modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween,
	) {
		Text(
			text = title,
			modifier = Modifier.weight(1f),
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold,
		)
		Row(
			modifier = Modifier.heightIn(min = 48.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.End,
		) {
			buttons?.invoke()
		}
	}
}
