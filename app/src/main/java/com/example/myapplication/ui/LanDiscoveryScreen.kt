package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.network.DiscoveredDevice
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanDiscoveryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPeer: (ip: String, port: Int, deviceId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = viewModel()
) {
    val discovered by viewModel.discoveredDevices.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 自动转大写
    val normalizedSearch = remember(searchText) { searchText.trim().uppercase() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("局域网发现") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("输入他人识别码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (searchText.isBlank()) return@IconButton
                            scope.launch {
                                isSearching = true
                                val found = viewModel.scanLanForDevice(normalizedSearch)
                                isSearching = false
                                if (found != null) {
                                    onNavigateToPeer(
                                        found.address.hostAddress ?: "",
                                        found.port,
                                        normalizedSearch
                                    )
                                } else {
                                    snackbarHostState.showSnackbar("未找到该设备，请确认对方已开启应用并在同一局域网内")
                                }
                            }
                        },
                        enabled = !isSearching
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                }
            )

            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "在线设备",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (discovered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未发现在线设备",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discovered.toList(), key = { it.first }) { (deviceId, device) ->
                        DeviceCard(
                            deviceId = deviceId,
                            ip = device.address.hostAddress ?: "",
                            onClick = {
                                onNavigateToPeer(device.address.hostAddress ?: "", device.port, deviceId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    deviceId: String,
    ip: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = deviceId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
