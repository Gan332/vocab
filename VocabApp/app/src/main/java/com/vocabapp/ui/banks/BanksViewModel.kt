package com.vocabapp.ui.banks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vocabapp.VocabApp
import com.vocabapp.data.db.entity.BankEntity
import com.vocabapp.data.db.entity.FavoriteEntity
import com.vocabapp.data.db.entity.WordEntity
import com.vocabapp.data.db.entity.WrongBookEntity
import com.vocabapp.data.model.BuiltInBank
import com.vocabapp.data.model.WordPair
import com.vocabapp.data.repository.VocabRepository
import com.vocabapp.util.TxtParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BanksUiState(
    val banks: List<BankEntity> = emptyList(),
    val wrongBook: List<WrongBookEntity> = emptyList(),
    val favorites: List<FavoriteEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val showMarket: Boolean = false,
    val showWordList: Boolean = false,
    val wordListBank: String = "",
    val wordListWords: List<WordEntity> = emptyList(),
    val wordListSearch: String = "",
    val builtInBanks: List<BuiltInBank> = emptyList(),
    val marketTab: String = "builtin" // builtin, url
)

class BanksViewModel(
    private val repository: VocabRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BanksUiState())
    val uiState: StateFlow<BanksUiState> = _uiState.asStateFlow()

    private val _exportContent = MutableStateFlow<ExportContent?>(null)
    val exportContent: StateFlow<ExportContent?> = _exportContent.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        // Load banks
        repository.getAllBanks().onEach { banks ->
            _uiState.update { it.copy(banks = banks) }
        }.launchIn(viewModelScope)

        // Load wrong book
        repository.getAllWrongBook().onEach { wrongBook ->
            _uiState.update { it.copy(wrongBook = wrongBook) }
        }.launchIn(viewModelScope)

        // Load favorites
        repository.getAllFavorites().onEach { favorites ->
            _uiState.update { it.copy(favorites = favorites) }
        }.launchIn(viewModelScope)

        // Load built-in banks
        loadBuiltInBanks()
    }

    private fun loadBuiltInBanks() {
        val banks = listOf(
            BuiltInBank("大学英语四级", "📘", "CET-4 核心词汇，覆盖历年真题高频词", "150 词", emptyList()),
            BuiltInBank("大学英语六级", "📙", "CET-6 核心词汇，六级考试必备", "120 词", emptyList()),
            BuiltInBank("考研英语", "📕", "考研英语核心词汇，真题高频词", "120 词", emptyList()),
            BuiltInBank("IELTS 雅思", "🌍", "雅思考试核心词汇，听说读写全覆盖", "120 词", emptyList())
        )
        _uiState.update { it.copy(builtInBanks = banks) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleMarket() {
        _uiState.update { it.copy(showMarket = !it.showMarket) }
    }

    fun setMarketTab(tab: String) {
        _uiState.update { it.copy(marketTab = tab) }
    }

    fun showWordList(bankName: String) {
        viewModelScope.launch {
            val words = repository.getWordsByBankList(bankName)
            _uiState.update {
                it.copy(
                    showWordList = true,
                    wordListBank = bankName,
                    wordListWords = words,
                    wordListSearch = ""
                )
            }
        }
    }

    fun hideWordList() {
        _uiState.update { it.copy(showWordList = false) }
    }

    fun updateWordListSearch(query: String) {
        viewModelScope.launch {
            val bankName = _uiState.value.wordListBank
            val words = if (query.isBlank()) {
                repository.getWordsByBankList(bankName)
            } else {
                repository.searchWords(bankName, query)
            }
            _uiState.update { it.copy(wordListSearch = query, wordListWords = words) }
        }
    }

    fun importFileContent(fileName: String, content: String) {
        viewModelScope.launch {
            val cards = TxtParser.parse(content)
            if (cards.isNotEmpty()) {
                val name = fileName.removeSuffix(".txt")
                repository.importBank(name, cards)
            }
        }
    }

    fun importBuiltInBank(bank: BuiltInBank) {
        viewModelScope.launch {
            val cards = getBuiltInBankCards(bank.name)
            if (cards.isNotEmpty()) {
                repository.importBank(bank.name, cards)
            }
        }
    }

    fun importUrlBank(url: String, content: String) {
        viewModelScope.launch {
            val cards = TxtParser.parse(content)
            if (cards.isNotEmpty()) {
                val name = url.substringAfterLast("/").removeSuffix(".txt").ifBlank { "下载词库" }
                repository.importBank(name, cards)
            }
        }
    }

    fun deleteBank(name: String) {
        viewModelScope.launch {
            repository.deleteBank(name)
        }
    }

    fun clearWrongBook() {
        viewModelScope.launch {
            repository.clearWrongBook()
        }
    }

    fun clearFavorites() {
        viewModelScope.launch {
            repository.clearFavorites()
        }
    }

    fun exportBank(name: String) {
        viewModelScope.launch {
            val words = repository.getWordsByBankList(name)
            val content = words.joinToString("\n") { "${it.word} - ${it.definition}" }
            _exportContent.value = ExportContent(filename = "${name}.txt", content = content)
        }
    }

    fun exportWrongBook() {
        viewModelScope.launch {
            val entries = repository.getWrongBookList()
            val content = entries.joinToString("\n") { "${it.word} - ${it.definition}" }
            _exportContent.value = ExportContent(filename = "错题本.txt", content = content)
        }
    }

    fun exportFavorites() {
        viewModelScope.launch {
            val entries = repository.getFavoritesList()
            val content = entries.joinToString("\n") { "${it.word} - ${it.definition}" }
            _exportContent.value = ExportContent(filename = "收藏单词.txt", content = content)
        }
    }

    fun clearExportContent() {
        _exportContent.value = null
    }

    private fun getBuiltInBankCards(name: String): List<WordPair> {
        // Return word pairs from built-in data
        return when (name) {
            "大学英语四级" -> cet4Words
            "大学英语六级" -> cet6Words
            "考研英语" -> kaoyanWords
            "IELTS 雅思" -> ieltsWords
            else -> emptyList()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BanksViewModel(VocabApp.instance.repository) as T
            }
        }
    }
}

data class ExportContent(
    val filename: String,
    val content: String
)
 
// Sample built-in word data
private val cet4Words = listOf(
    WordPair("abandon", "放弃"), WordPair("ability", "能力，才能"),
    WordPair("abroad", "到国外"), WordPair("absence", "缺席，不在"),
    WordPair("absorb", "吸收，吸引"), WordPair("abstract", "抽象的"),
    WordPair("abundant", "丰富的，充裕的"), WordPair("academic", "学术的"),
    WordPair("accelerate", "加速，促进"), WordPair("access", "通道，接近"),
    WordPair("accident", "事故，意外"), WordPair("accompany", "陪伴，伴随"),
    WordPair("accomplish", "完成，实现"), WordPair("accurate", "精确的"),
    WordPair("accuse", "控告，指责"), WordPair("achieve", "达到，取得"),
    WordPair("acknowledge", "承认，致谢"), WordPair("acquire", "获得，学到"),
    WordPair("adapt", "适应，改编"), WordPair("adequate", "足够的，适当的"),
    WordPair("adjust", "调整，调节"), WordPair("admire", "钦佩，赞赏"),
    WordPair("admit", "承认，准许进入"), WordPair("adopt", "采用，收养"),
    WordPair("advance", "前进，推进"), WordPair("advantage", "优势，有利条件"),
    WordPair("advertise", "做广告"), WordPair("affair", "事务，事件"),
    WordPair("affect", "影响，感动"), WordPair("afford", "负担得起"),
)

private val cet6Words = listOf(
    WordPair("abnormal", "反常的，异常的"), WordPair("abolish", "废除，废止"),
    WordPair("abrupt", "突然的，唐突的"), WordPair("absurd", "荒谬的，可笑的"),
    WordPair("abundance", "丰富，充裕"), WordPair("abuse", "滥用，虐待"),
    WordPair("academy", "学院，研究院"), WordPair("accessory", "附件，配件"),
    WordPair("accommodate", "容纳，适应"), WordPair("accumulate", "积累，积聚"),
    WordPair("acquaint", "使熟悉，告知"), WordPair("activate", "激活，启动"),
    WordPair("acute", "敏锐的，急性的"), WordPair("adhere", "粘附，坚持"),
    WordPair("adjacent", "邻近的，毗连的"), WordPair("administer", "管理，执行"),
)

private val kaoyanWords = listOf(
    WordPair("abide", "遵守，容忍"), WordPair("abolish", "废除，废止"),
    WordPair("abound", "充满，丰富"), WordPair("absorb", "吸收，吸引"),
    WordPair("abstract", "抽象的，摘要"), WordPair("abuse", "滥用，虐待"),
    WordPair("accelerate", "加速，促进"), WordPair("accent", "口音，重音"),
    WordPair("access", "通道，接近"), WordPair("acclaim", "欢呼，称赞"),
)

private val ieltsWords = listOf(
    WordPair("abandon", "放弃，遗弃"), WordPair("abroad", "在国外"),
    WordPair("absent", "缺席的"), WordPair("absolute", "绝对的"),
    WordPair("absorb", "吸收"), WordPair("abstract", "抽象的"),
    WordPair("abundant", "丰富的"), WordPair("academic", "学术的"),
    WordPair("accelerate", "加速"), WordPair("access", "通道，接近"),
)
