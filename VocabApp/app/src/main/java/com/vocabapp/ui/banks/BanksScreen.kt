package com.vocabapp.ui.banks

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vocabapp.ui.components.MarketSheet
import com.vocabapp.ui.components.WordListSheet
import com.vocabapp.ui.theme.*
import java.io.File

@Composable
fun BanksScreen(
    onStartLearn: (String) -> Unit = {},
    viewModel: BanksViewModel = viewModel(factory = BanksViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val exportContent by viewModel.exportContent.collectAsStateWithLifecycle()
    LaunchedEffect(exportContent) {
        exportContent?.let { (filename, content) ->
            shareText(context, content, filename)
            viewModel.clearExportContent()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()
                val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "import.txt"
                viewModel.importFileContent(fileName, content)
            } catch (_: Exception) { }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "词库",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("搜索") },
                    leadingIcon = { Text("🔍", fontSize = 15.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Import card
            item {
                IosGroupRow {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filePickerLauncher.launch("text/plain") }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📂", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("导入 TXT 词库", style = MaterialTheme.typography.bodyLarge)
                            Text("每行: 单词 - 释义", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(">", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Wrong book
            val wrongBook = uiState.wrongBook
            if (wrongBook.isNotEmpty()) {
                item {
                    IosGroupRow {
                        IosCell(
                            title = "📕 错题本",
                            subtitle = "${wrongBook.size} 个单词 · 累计错误 ${wrongBook.sumOf { it.wrongCount }} 次",
                            onStudy = { onStartLearn("__wrongbook__") },
                            onExport = { viewModel.exportWrongBook() },
                            onDelete = { viewModel.clearWrongBook() }
                        )
                    }
                }
            }

            // Favorites
            val favorites = uiState.favorites
            if (favorites.isNotEmpty()) {
                item {
                    IosGroupRow {
                        IosCell(
                            title = "⭐ 收藏单词",
                            subtitle = "${favorites.size} 个单词",
                            onStudy = { onStartLearn("__favorites__") },
                            onExport = { viewModel.exportFavorites() },
                            onDelete = { viewModel.clearFavorites() }
                        )
                    }
                }
            }

            // Filtered banks
            val filteredBanks = if (uiState.searchQuery.isBlank()) {
                uiState.banks
            } else {
                uiState.banks.filter { it.name.contains(uiState.searchQuery, ignoreCase = true) }
            }

            if (filteredBanks.isEmpty() && uiState.banks.isEmpty() && wrongBook.isEmpty() && favorites.isEmpty()) {
                item {
                    IosGroupRow {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📖", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("还没有词库", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("导入 TXT 文件开始吧", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            if (filteredBanks.isEmpty() && uiState.searchQuery.isNotBlank()) {
                item {
                    IosGroupRow {
                        Text("没有找到匹配的词库",
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Bank items
            items(filteredBanks, key = { it.name }) { bank ->
                IosGroupRow {
                    IosCell(
                        title = bank.name,
                        subtitle = "${bank.count} 个单词",
                        onStudy = { onStartLearn(bank.name) },
                        onViewWords = { viewModel.showWordList(bank.name) },
                        onExport = { viewModel.exportBank(bank.name) },
                        onDelete = { viewModel.deleteBank(bank.name) }
                    )
                }
            }

            // Bottom button
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.toggleMarket() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("📥 获取词库", color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (uiState.showWordList) {
        WordListSheet(
            bankName = uiState.wordListBank,
            words = uiState.wordListWords,
            searchQuery = uiState.wordListSearch,
            onSearchChange = { viewModel.updateWordListSearch(it) },
            onDismiss = { viewModel.hideWordList() }
        )
    }

    if (uiState.showMarket) {
        MarketSheet(
            builtInBanks = uiState.builtInBanks,
            activeTab = uiState.marketTab,
            onTabChange = { viewModel.setMarketTab(it) },
            onImportBuiltIn = { viewModel.importBuiltInBank(it) },
            onImportUrl = { url, content -> viewModel.importUrlBank(url, content) },
            onDismiss = { viewModel.toggleMarket() }
        )
    }
}

@Composable
fun IosGroupRow(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun IosCell(
    title: String,
    subtitle: String,
    onStudy: () -> Unit,
    onViewWords: (() -> Unit)? = null,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewWords?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IosSmallButton("学习", iosBlue, onStudy)
            IosSmallButton("导出", iosSecondaryLabel, onExport)
            IosSmallButton("删除", iosRed, onDelete)
        }
    }
}

@Composable
fun IosSmallButton(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(30.dp)
    ) {
        Text(text, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

private fun shareText(context: android.content.Context, text: String, filename: String) {
    try {
        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, filename)
        file.writeText(text)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "导出"))
    } catch (_: Exception) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "导出"))
    }
}
