package com.vocabapp.ui.banks

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vocabapp.data.db.entity.BankEntity
import com.vocabapp.data.db.entity.FavoriteEntity
import com.vocabapp.data.db.entity.WordEntity
import com.vocabapp.data.db.entity.WrongBookEntity
import com.vocabapp.ui.components.MarketSheet
import com.vocabapp.ui.components.WordListSheet
import com.vocabapp.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanksScreen(
    onStartLearn: (String) -> Unit = {},
    viewModel: BanksViewModel = viewModel(factory = BanksViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // File picker for TXT import
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
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("🔍 搜索词库或单词...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        // Import area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { filePickerLauncher.launch("text/plain") },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📂", fontSize = 28.sp)
                Text(
                    "点击导入 TXT 词库文件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "每行格式: 单词 - 释义",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Bank list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Wrong book card
            val wrongBook = uiState.wrongBook
            if (wrongBook.isNotEmpty()) {
                item(key = "__wrongbook__") {
                    BankCard(
                        name = "📕 错题本",
                        meta = "${wrongBook.size} 个单词 · 累计错误 ${wrongBook.sumOf { it.wrongCount }} 次",
                        borderColor = amber,
                        onStudy = { onStartLearn("__wrongbook__") },
                        onExport = {
                            val content = viewModel.exportWrongBook()
                            if (content != null) shareText(context, content, "错题本.txt")
                        },
                        onDelete = { viewModel.clearWrongBook() }
                    )
                }
            }

            // Favorites card
            val favorites = uiState.favorites
            if (favorites.isNotEmpty()) {
                item(key = "__favorites__") {
                    BankCard(
                        name = "⭐ 收藏单词",
                        meta = "${favorites.size} 个单词",
                        borderColor = MaterialTheme.colorScheme.primary,
                        onStudy = { onStartLearn("__favorites__") },
                        onExport = {
                            val content = viewModel.exportFavorites()
                            if (content != null) shareText(context, content, "收藏单词.txt")
                        },
                        onDelete = { viewModel.clearFavorites() }
                    )
                }
            }

            // Filter banks by search query
            val filteredBanks = if (uiState.searchQuery.isBlank()) {
                uiState.banks
            } else {
                uiState.banks.filter { it.name.contains(uiState.searchQuery, ignoreCase = true) }
            }

            if (filteredBanks.isEmpty() && uiState.banks.isEmpty() && wrongBook.isEmpty() && favorites.isEmpty()) {
                item {
                    EmptyState()
                }
            }

            if (filteredBanks.isEmpty() && uiState.searchQuery.isNotBlank()) {
                item {
                    Text(
                        "没有找到匹配的词库",
                        modifier = Modifier.padding(vertical = 24.dp),
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(filteredBanks, key = { it.name }) { bank ->
                BankCard(
                    name = bank.name,
                    meta = "${bank.count} 个单词",
                    borderColor = MaterialTheme.colorScheme.primary,
                    onStudy = { onStartLearn(bank.name) },
                    onViewWords = { viewModel.showWordList(bank.name) },
                    onExport = {
                        val content = viewModel.exportBank(bank.name)
                        if (content != null) shareText(context, content, "${bank.name}.txt")
                    },
                    onDelete = { viewModel.deleteBank(bank.name) }
                )
            }
        }

        // Import button
        Button(
            onClick = { viewModel.toggleMarket() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("📥 获取词库", color = MaterialTheme.colorScheme.onPrimary)
        }
    }

    // Word list bottom sheet
    if (uiState.showWordList) {
        WordListSheet(
            bankName = uiState.wordListBank,
            words = uiState.wordListWords,
            searchQuery = uiState.wordListSearch,
            onSearchChange = { viewModel.updateWordListSearch(it) },
            onDismiss = { viewModel.hideWordList() }
        )
    }

    // Market bottom sheet
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
private fun BankCard(
    name: String,
    meta: String,
    borderColor: androidx.compose.ui.graphics.Color,
    onStudy: () -> Unit,
    onViewWords: (() -> Unit)? = null,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewWords?.invoke() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallButton("学习", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, onStudy)
                SmallButton("导出", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, onExport)
                SmallButton("删除", vermillionLight, vermillion, onDelete)
            }
        }
    }
}

@Composable
private fun SmallButton(
    text: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text, fontSize = 12.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📖", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "还没有词库，导入一个 TXT 文件开始吧",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
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
    } catch (e: Exception) {
        // Fallback: share as text
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "导出"))
    }
}
