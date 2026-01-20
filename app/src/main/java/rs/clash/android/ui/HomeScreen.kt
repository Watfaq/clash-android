package rs.clash.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import rs.clash.android.R
import rs.clash.android.viewmodel.HomeViewModel
import java.util.*
import kotlin.math.log10
import kotlin.math.pow
import uniffi.clash_android_ffi.Proxy

// Constants
private const val DELAY_EXCELLENT_MS = 300
private const val DELAY_GOOD_MS = 600
private const val PROXY_COLUMNS = 2


@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val tabs = listOf(
        stringResource(R.string.tab_proxies),
        stringResource(R.string.tab_overview)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.fetchProxies() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh)
                            )
                        }
                    }
                    PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
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
    val proxies by remember { derivedStateOf { viewModel.proxies } }
    val isRefreshing by remember { derivedStateOf { viewModel.isRefreshing } }
    val errorMessage by remember { derivedStateOf { viewModel.errorMessage } }
    val delays = viewModel.delays

    LaunchedEffect(Unit) {
        if (proxies.isEmpty()) {
            viewModel.fetchProxies()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.fetchProxies() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            proxies.isEmpty() && !isRefreshing -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_proxy_groups),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val proxyTypes = remember(proxies) {
                    proxies.mapValues { it.value.proxyType }
                }
                val groups = remember(proxies) {
                    proxies.filter { it.value.all.isNotEmpty() }.toList()
                }

                if (groups.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                        
                        items(
                            items = groups,
                            key = { it.first }
                        ) { (name, proxy) ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                        
                        item { Spacer(modifier = Modifier.height(100.dp)) } // More bottom space
                    }
                }
            }
        }

        // Loading indicator overlay
        if (isRefreshing) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.refreshing),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewTab(viewModel: HomeViewModel) {
    val memory by remember { derivedStateOf { viewModel.memoryUsage } }
    val connections by remember { derivedStateOf { viewModel.connectionCount } }
    val download by remember { derivedStateOf { viewModel.totalDownload } }
    val upload by remember { derivedStateOf { viewModel.totalUpload } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        item(key = "memory") {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                StatsCard(
                    title = stringResource(R.string.stat_memory),
                    value = memory?.let { formatSize(it.inuse) } ?: stringResource(R.string.not_available),
                    subtitle = memory?.let { stringResource(R.string.stat_memory_limit, formatSize(it.oslimit)) } ?: stringResource(R.string.refreshing)
                )
            }
        }

        item(key = "connections") {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                StatsCard(
                    title = stringResource(R.string.stat_connections),
                    value = connections.toString(),
                    subtitle = if (connections > 0) stringResource(R.string.stat_connections_ongoing) else stringResource(R.string.stat_connections_none)
                )
            }
        }

        item(key = "bandwidth") {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatsCard(
                        title = stringResource(R.string.stat_download),
                        value = formatSize(download),
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = stringResource(R.string.stat_upload),
                        value = formatSize(upload),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String = ""
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$title: $value${if (subtitle.isNotEmpty()) ", $subtitle" else ""}"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
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
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val noneText = stringResource(R.string.none)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = proxy.now ?: noneText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand),
                        modifier = Modifier
                            .rotate(rotation)
                            .padding(horizontal = 8.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                
                IconButton(
                    onClick = onTestDelay,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = stringResource(R.string.test_latency),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded && proxy.all.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))

                    proxy.all.chunked(PROXY_COLUMNS).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { option ->
                                ProxyItem(
                                    option = option,
                                    isSelected = option == proxy.now,
                                    delay = delays[option],
                                    type = proxyTypes[option],
                                    onSelect = { onSelect(option) },
                                    modifier = Modifier.weight(1f)
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
}

@Composable
fun ProxyItem(
    option: String,
    isSelected: Boolean,
    delay: String?,
    type: String?,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onSelect() }
            .padding(8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        },
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.selected),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (type != null) {
                    Text(
                        text = type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
                if (delay != null) {
                    val testingText = stringResource(R.string.testing)
                    val timeoutText = stringResource(R.string.timeout)
                    val displayedDelay = when (delay) {
                        "testing..." -> testingText
                        "timeout" -> timeoutText
                        else -> delay
                    }
                    val delayColor = when {
                        delay.contains("ms") -> {
                            val ms = delay.replace("ms", "").trim().toIntOrNull() ?: 0
                            when {
                                ms < DELAY_EXCELLENT_MS -> Color(0xFF4CAF50) // Green
                                ms < DELAY_GOOD_MS -> Color(0xFFFFC107) // Amber
                                else -> Color(0xFFF44336) // Red
                            }
                        }
                        delay == "testing..." || delay == testingText -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.error
                    }

                    Text(
                        text = displayedDelay,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = delayColor
                    )
                }
            }
        }
    }
}
