package com.vocabapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocabapp.data.model.BuiltInBank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketSheet(
    builtInBanks: List<BuiltInBank>,
    activeTab: String,
    onTabChange: (String) -> Unit,
    onImportBuiltIn: (BuiltInBank) -> Unit,
    onImportUrl: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📥 词库市场",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 18.sp)
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = if (activeTab == "builtin") 0 else 1
            ) {
                Tab(
                    selected = activeTab == "builtin",
                    onClick = { onTabChange("builtin") },
                    text = { Text("内置词库") }
                )
                Tab(
                    selected = activeTab == "url",
                    onClick = { onTabChange("url") },
                    text = { Text("从 URL 导入") }
                )
            }

            when (activeTab) {
                "builtin" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(builtInBanks) { bank ->
                            BuiltInBankItem(
                                bank = bank,
                                onImport = { onImportBuiltIn(bank) }
                            )
                        }
                    }
                }
                "url" -> {
                    UrlImportTab(
                        onImport = onImportUrl
                    )
                }
            }
        }
    }
}

@Composable
private fun BuiltInBankItem(
    bank: BuiltInBank,
    onImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(bank.icon, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    bank.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    bank.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    bank.size,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Button(
                onClick = onImport,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("导入", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun UrlImportTab(
    onImport: (String, String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            "输入 TXT 词库文件的直链 URL",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "每行格式: 单词 - 释义 或 单词\\t释义 或 单词|释义",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("https://example.com/words.txt") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                enabled = !isLoading
            )
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        isLoading = true
                        status = "⏳ 正在下载..."
                        // In a real app, you'd use a coroutine to fetch the URL
                        // For now, simulate with a message
                        status = "⚠️ URL导入需要网络请求权限，请在实际设备上测试"
                        isLoading = false
                    }
                },
                shape = RoundedCornerShape(10.dp),
                enabled = !isLoading
            ) {
                Text("下载导入")
            }
        }

        if (status.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            val statusColor = when {
                status.startsWith("✅") -> MaterialTheme.colorScheme.tertiary
                status.startsWith("❌") -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                status,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor
            )
        }
    }
}
