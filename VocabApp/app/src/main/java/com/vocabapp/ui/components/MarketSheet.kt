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
import com.vocabapp.ui.theme.*
import com.vocabapp.ui.learn.IosCard

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
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
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
                Text("词库市场", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 18.sp)
                }
            }

            // Segmented tabs
            IosSegmentedTabs(activeTab, onTabChange)

            Spacer(Modifier.height(8.dp))

            when (activeTab) {
                "builtin" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(builtInBanks) { bank ->
                            BuiltInBankItem(bank, onImport = { onImportBuiltIn(bank) })
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
                "url" -> {
                    UrlImportTab(onImport = onImportUrl)
                }
            }
        }
    }
}

@Composable
private fun IosSegmentedTabs(activeTab: String, onTabChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf("builtin" to "内置词库", "url" to "从 URL 导入").forEach { (value, label) ->
            val isSelected = activeTab == value
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface
                        else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(7.dp)
                    )
                    .clickable { onTabChange(value) }
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BuiltInBankItem(
    bank: BuiltInBank,
    onImport: () -> Unit
) {
    IosCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(bank.icon, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(bank.name, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold)
                Text(bank.desc, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(bank.size, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            TextButton(onClick = onImport) {
                Text("导入", color = iosBlue, fontWeight = FontWeight.SemiBold)
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
        modifier = Modifier.fillMaxWidth().padding(20.dp)
    ) {
        Text("输入 TXT 词库文件的直链 URL", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("每行: 单词 - 释义 / 单词\\t释义 / 单词|释义",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 4.dp))

        Spacer(Modifier.height(12.dp))

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
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        isLoading = true
                        status = "⏳ 正在下载..."
                        status = "⚠️ URL导入需要网络请求权限"
                        isLoading = false
                    }
                },
                shape = RoundedCornerShape(10.dp),
                enabled = !isLoading
            ) { Text("下载导入") }
        }

        if (status.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            val statusColor = when {
                status.startsWith("✅") -> MaterialTheme.colorScheme.tertiary
                status.startsWith("❌") -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(status, style = MaterialTheme.typography.bodyMedium, color = statusColor)
        }
    }
}
