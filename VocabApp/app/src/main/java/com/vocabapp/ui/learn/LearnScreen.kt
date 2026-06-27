package com.vocabapp.ui.learn

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vocabapp.ui.theme.*
import com.vocabapp.util.TimeUtils

@Composable
fun LearnScreen(
    initialBankName: String = "",
    viewModel: LearnViewModel = viewModel(factory = LearnViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialBankName) {
        if (initialBankName.isNotBlank()) {
            viewModel.updateSelectedBank(initialBankName)
        }
    }

    if (uiState.showSetup) {
        LearnSetupView(viewModel, uiState)
    } else if (uiState.showResult) {
        ResultView(viewModel, uiState)
    } else {
        LearnSessionView(viewModel, uiState)
    }

    if (uiState.showPause) {
        PauseOverlay(viewModel)
    }
}

// ===== Setup View =====
@Composable
private fun LearnSetupView(
    viewModel: LearnViewModel,
    uiState: LearnUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "学习",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bank selection card
            IosCard {
                Column(Modifier.padding(16.dp)) {
                    Text("词库", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = uiState.selectedBank,
                        onValueChange = { viewModel.updateSelectedBank(it) },
                        placeholder = { Text("输入词库名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )
                }
            }

            // Direction card
            IosCard {
                Column(Modifier.padding(16.dp)) {
                    Text("方向", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp))
                    IosSegmentedControl(
                        items = listOf("看单词" to "word-first", "看释义" to "def-first"),
                        selectedValue = uiState.selectedDirection,
                        onSelection = { viewModel.updateSelectedDirection(it) }
                    )
                }
            }

            // Mode card
            IosCard {
                Column(Modifier.padding(16.dp)) {
                    Text("模式", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModeButton("闪卡", "flashcard", uiState.selectedMode,
                                Modifier.weight(1f)) { viewModel.updateSelectedMode(it) }
                            ModeButton("答题", "quiz", uiState.selectedMode,
                                Modifier.weight(1f)) { viewModel.updateSelectedMode(it) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModeButton("打字", "typing", uiState.selectedMode,
                                Modifier.weight(1f)) { viewModel.updateSelectedMode(it) }
                            ModeButton("间隔重复", "srs", uiState.selectedMode,
                                Modifier.weight(1f)) { viewModel.updateSelectedMode(it) }
                        }
                    }
                }
            }

            // Start button
            Button(
                onClick = { viewModel.startSession() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = uiState.selectedBank.isNotBlank()
            ) {
                Text("开始学习", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    value: String,
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == value
    val bg = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface

    Button(
        onClick = { onSelect(value) },
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ===== Session View =====
@Composable
private fun LearnSessionView(
    viewModel: LearnViewModel,
    uiState: LearnUiState
) {
    val learnState = uiState.learnState ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        // iOS Nav bar style header
        Surface(
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(learnState.progress, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(uiState.elapsedText, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.pauseSession() }) {
                        Text("暂停", color = iosBlue, fontSize = 15.sp)
                    }
                    TextButton(onClick = { viewModel.quitSession() }) {
                        Text("退出", color = iosRed, fontSize = 15.sp)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {
            when (learnState.mode) {
                "flashcard" -> FlashcardMode(learnState, uiState, viewModel)
                "srs" -> SrsMode(learnState, uiState, viewModel)
                "quiz" -> QuizMode(learnState, uiState, viewModel)
                "typing" -> TypingMode(learnState, uiState, viewModel)
            }
        }
    }
}

// ===== Flashcard Mode =====
@Composable
private fun FlashcardMode(
    learnState: com.vocabapp.data.model.LearnState,
    uiState: LearnUiState,
    viewModel: LearnViewModel
) {
    val card = learnState.currentCard ?: return

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { viewModel.flipCard() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        card.front,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (learnState.answered) {
                        Text(
                            card.back,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            "点击显示答案",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (learnState.answered) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.answerCard(false) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = iosRedLight,
                        contentColor = iosRed
                    )
                ) { Text("不认识", fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { viewModel.answerCard(true) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = iosGreenLight,
                        contentColor = iosGreen
                    )
                ) { Text("认识了", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ===== SRS Mode =====
@Composable
private fun SrsMode(
    learnState: com.vocabapp.data.model.LearnState,
    uiState: LearnUiState,
    viewModel: LearnViewModel
) {
    val card = learnState.currentCard ?: return

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { viewModel.flipSrsCard() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(card.front,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                        textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp))
                    if (learnState.answered) {
                        Text(card.back, style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    } else {
                        Text("点击显示答案", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (learnState.answered) {
            Spacer(Modifier.height(12.dp))
            Text("掌握程度", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SrsButton("忘记", "0", onClick = { viewModel.answerSrsCard(0) }, Modifier.weight(1f))
                    SrsButton("错误", "1", onClick = { viewModel.answerSrsCard(1) }, Modifier.weight(1f))
                    SrsButton("模糊", "2", onClick = { viewModel.answerSrsCard(2) }, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SrsButton("困难", "3", onClick = { viewModel.answerSrsCard(3) }, Modifier.weight(1f))
                    SrsButton("顺利", "4", onClick = { viewModel.answerSrsCard(4) }, Modifier.weight(1f))
                    SrsButton("完美", "5", onClick = { viewModel.answerSrsCard(5) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SrsButton(
    label: String,
    rating: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(rating, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(label, fontSize = 10.sp)
        }
    }
}

// ===== Quiz Mode =====
@Composable
private fun QuizMode(
    learnState: com.vocabapp.data.model.LearnState,
    uiState: LearnUiState,
    viewModel: LearnViewModel
) {
    val card = learnState.currentCard ?: return

    LaunchedEffect(learnState.index) {
        viewModel.showQuizQuestion()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("选择正确的答案", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 16.dp))
                Text(card.front, style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.quizOptions.forEach { option ->
                val bgColor = when {
                    option.isDisabled && option.isCorrect -> iosGreenLight
                    option.isSelected && !option.isCorrect -> iosRedLight
                    else -> MaterialTheme.colorScheme.surface
                }
                val textColor = when {
                    option.isDisabled && option.isCorrect -> iosGreen
                    option.isSelected && !option.isCorrect -> iosRed
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !option.isDisabled) { viewModel.answerQuiz(option) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(option.text,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        fontWeight = if (option.isDisabled) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

// ===== Typing Mode =====
@Composable
private fun TypingMode(
    learnState: com.vocabapp.data.model.LearnState,
    uiState: LearnUiState,
    viewModel: LearnViewModel
) {
    val card = learnState.currentCard ?: return
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(learnState.index) {
        inputText = ""
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(card.back, style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp))
                Text("拼写单词，按回车提交", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline)
            }
        }

        val borderColor = when {
            uiState.typingIsCorrect == true -> iosGreen
            uiState.typingIsCorrect == false -> iosRed
            else -> MaterialTheme.colorScheme.outlineVariant
        }
        val bgColor = when {
            uiState.typingIsCorrect == true -> iosGreenLight
            uiState.typingIsCorrect == false -> iosRedLight
            else -> MaterialTheme.colorScheme.surface
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = { if (!learnState.answered) inputText = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = borderColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = bgColor,
                unfocusedContainerColor = bgColor
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (!learnState.answered) viewModel.submitTypingAnswer(inputText)
                    else { viewModel.nextTypingQuestion(); inputText = "" }
                }
            ),
            enabled = !learnState.answered || uiState.typingIsCorrect != null
        )

        if (uiState.typingFeedback.isNotEmpty()) {
            Text(uiState.typingFeedback, style = MaterialTheme.typography.titleMedium,
                color = if (uiState.typingIsCorrect == true) iosGreen else iosRed,
                modifier = Modifier.padding(vertical = 12.dp),
                fontWeight = FontWeight.SemiBold)
        }

        if (learnState.answered) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.nextTypingQuestion(); inputText = "" },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = iosGreen,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("下一题") }
        }
    }
}

// ===== Result View =====
@Composable
private fun ResultView(
    viewModel: LearnViewModel,
    uiState: LearnUiState
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            uiState.resultTitle,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            uiState.resultBank,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        IosCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatItem("总题数", uiState.resultTotal.toString(), Modifier.weight(1f))
                    StatItem("正确", uiState.resultRemembered.toString(), Modifier.weight(1f))
                    StatItem("正确率", uiState.resultAccuracy, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatItem("用时", uiState.resultDuration, Modifier.weight(1f))
                    StatItem("平均每题", uiState.resultAvgTime, Modifier.weight(1f))
                    StatItem("错误", uiState.resultForgotten.toString(), Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.retrySession() },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) { Text("再来一轮") }
            OutlinedButton(
                onClick = { viewModel.goBackToSetup() },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("返回选择") }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ===== Pause Overlay =====
@Composable
private fun PauseOverlay(viewModel: LearnViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⏸️", fontSize = 36.sp)
                Text("已暂停", style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(vertical = 4.dp))
                Text("进度已自动保存", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp))
                Button(
                    onClick = { viewModel.continueFromPause() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("继续学习") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.saveAndQuit() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = iosRed)
                ) { Text("保存并退出") }
            }
        }
    }
}

// ===== Shared iOS Components =====

@Composable
fun IosCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun IosSegmentedControl(
    items: List<Pair<String, String>>,
    selectedValue: String,
    onSelection: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach { (label, value) ->
            val isSelected = selectedValue == value
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        RoundedCornerShape(7.dp)
                    )
                    .clickable { onSelection(value) }
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
