package rs.clash.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import rs.clash.android.viewmodel.HomeViewModel
import java.util.*
import kotlin.math.log10
import kotlin.math.pow
import uniffi.clash_android_ffi.Proxy
import kotlin.text.format


@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val tabs = listOf("Proxies", "Overview")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Clash Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.fetchProxies() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = modifier.padding(padding).fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> ProxyTab(viewModel)
                1 -> OverviewTab(viewModel)
            }
        }
    }
}

@Composable
fun ProxyTab(viewModel: HomeViewModel) {
    val proxies = viewModel.proxies
    val isRefreshing = viewModel.isRefreshing
    val errorMessage = viewModel.errorMessage
    val delays = viewModel.delays

    LaunchedEffect(Unit) {
        viewModel.fetchProxies()
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            val proxyTypes = remember(proxies) { proxies.mapValues { it.value.proxyType } }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val groups = proxies.filter { it.value.all.isNotEmpty() }
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
                            viewModel.testGroupDelay(proxy.all)
                        }
                    )
                }
            }
        }

        if (isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun OverviewTab(viewModel: HomeViewModel) {
    val memory = viewModel.memoryUsage
    val connections = viewModel.connectionCount
    val download = viewModel.totalDownload
    val upload = viewModel.totalUpload

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatsCard(
            title = "Memory Usage",
            value = memory?.let { formatSize(it.inuse) } ?: "N/A",
            subtitle = memory?.let { "Limit: ${formatSize(it.oslimit)}" } ?: ""
        )
        
        StatsCard(
            title = "Active Connections",
            value = connections.toString(),
            subtitle = "Ongoing network sessions"
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatsCard(
                modifier = Modifier.weight(1f),
                title = "Total Download",
                value = formatSize(download)
            )
            StatsCard(
                modifier = Modifier.weight(1f),
                title = "Total Upload",
                value = formatSize(upload)
            )
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.US, "%.2f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
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

            if (expanded && proxy.all.isNotEmpty()) {
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
