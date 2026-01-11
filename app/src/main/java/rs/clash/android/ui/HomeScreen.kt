package rs.clash.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import rs.clash.android.Proxy
import rs.clash.android.viewmodel.HomeViewModel

@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val proxies = viewModel.proxies
    val isRefreshing = viewModel.isRefreshing
    val errorMessage = viewModel.errorMessage
    val delays = viewModel.delays

    LaunchedEffect(Unit) {
        viewModel.fetchProxies()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Proxy Selection",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = { viewModel.fetchProxies() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (proxies.isEmpty() && !isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No proxy groups found. Is VPN running?")
                }
            } else {
                val proxyTypes = remember(proxies) { proxies.mapValues { it.value.type } }
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val groups = proxies.filter { it.value.all != null }
                    items(groups.toList()) { (name, proxy) ->
                        ProxyGroupWidget(
                            name = name,
                            proxy = proxy,
                            delays = delays,
                            proxyTypes = proxyTypes,
                            onSelect = { selectedName ->
                                viewModel.selectProxy(name, selectedName)
                            },
                            onTestDelay = {
                                proxy.all?.let { viewModel.testGroupDelay(it) }
                            }
                        )
                    }
                }
            }
        }

        if (isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun ProxyGroupWidget(
    name: String,
    proxy: Proxy,
    delays: Map<String, String>,
    proxyTypes: Map<String, String>,
    onSelect: (String) -> Unit,
    onTestDelay: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded }
                ) {
                    Text(
                        text = name, 
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = proxy.now ?: "None",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(
                    onClick = onTestDelay,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Test Latency",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded && proxy.all != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                
                proxy.all.chunked(2).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { option ->
                            ProxyItem(
                                modifier = Modifier.weight(1f),
                                option = option,
                                isSelected = option == proxy.now,
                                delay = delays[option],
                                type = proxyTypes[option],
                                onSelect = { onSelect(option) }
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProxyItem(
    modifier: Modifier = Modifier,
    option: String,
    isSelected: Boolean,
    delay: String?,
    type: String?,
    onSelect: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onSelect() }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = option,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (type != null) {
                Text(
                    text = type,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
            if (delay != null) {
                Text(
                    text = delay,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = when {
                        delay.contains("ms") -> {
                            val ms = delay.replace("ms", "").toIntOrNull() ?: 0
                            if (ms < 300) Color(0xFF4CAF50) else if (ms < 600) Color(0xFFFFC107) else Color(0xFFF44336)
                        }
                        delay == "testing..." -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}
