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
    viewModel: LearnViewModel = viewModel(factory = LearnViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showSetup) {
        LearnSetupView(viewModel, uiState)
    } else if (uiState.showResult) {
        ResultView(viewModel, uiState)
    } else {
        LearnSessionView(viewModel, uiState)
    }

    // Pause overlay
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "选择词库开始学习",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Bank select
                BankSelector(
                    selectedBank = uiState.selectedBank,
                    onBankSelected = { viewModel.updateSelectedBank(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Direction
                DirectionSelector(
                    selected = uiState.selectedDirection,
                    onSelected = { viewModel.updateSelectedDirection(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mode
                ModeSelector(
                    selected = uiState.selectedMode,
                    onSelected = { viewModel.updateSelectedMode(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.startSession() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
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
}

@Composable
private fun BankSelector(
    selectedBank: String,
    onBankSelected: (String) -> Unit
) {
    // This would list available banks from the repository
    // For simplicity, using a text field that the user can type the bank name
    OutlinedTextField(
        value = selectedBank,
        onValueChange = onBankSelected,
        placeholder = { Text("输入词库名称") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(10.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = listOf("word-first" to "看单词回忆释义", "def-first" to "看释义回忆单词")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp)
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(label, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
private fun ModeSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip("flashcard", "闪卡模式", selected, onSelected, Modifier.weight(1f))
            ModeChip("quiz", "答题测验", selected, onSelected, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip("typing", "打字模式", selected, onSelected, Modifier.weight(1f))
            ModeChip("srs", "🧠 间隔重复", selected, onSelected, Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeChip(
    value: String,
    label: String,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelected(value) },
        label = { Text(label, fontSize = 13.sp) },
        modifier = modifier,
        shape = RoundedCornerShape(10.dp)
    )
}

// ===== Session View =====
@Composable
private fun LearnSessionView(
    viewModel: LearnViewModel,
    uiState: LearnUiState
) {
    val learnState = uiState.learnState ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    learnState.progress,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    uiState.elapsedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallOutlinedButton("⏸ 暂停") { viewModel.pauseSession() }
                    SmallOutlinedButton("退出") { viewModel.quitSession() }
                }
            }
        }

        // Content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
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
        // Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { viewModel.flipCard() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Tag
                Text(
                    learnState.progress,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

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
                            "点击卡片显示答案",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Action buttons
        if (learnState.answered) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = { viewModel.answerCard(false) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = vermillionLight,
                        contentColor = vermillion
                    )
                ) {
                    Text("不认识", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { viewModel.answerCard(true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = jadeLight,
                        contentColor = jade
                    )
                ) {
                    Text("认识了", fontWeight = FontWeight.SemiBold)
                }
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
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    learnState.progress,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

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
                            "点击卡片显示答案",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (learnState.answered) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "掌握程度：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            // SRS rating buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SrsRatingButton("0\n忘记", onClick = { viewModel.answerSrsCard(0) }, Modifier.weight(1f))
                    SrsRatingButton("1\n错误", onClick = { viewModel.answerSrsCard(1) }, Modifier.weight(1f))
                    SrsRatingButton("2\n模糊", onClick = { viewModel.answerSrsCard(2) }, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SrsRatingButton("3\n困难", onClick = { viewModel.answerSrsCard(3) }, Modifier.weight(1f))
                    SrsRatingButton("4\n顺利", onClick = { viewModel.answerSrsCard(4) }, Modifier.weight(1f))
                    SrsRatingButton("5\n完美", onClick = { viewModel.answerSrsCard(5) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SrsRatingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 14.sp)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    learnState.progress,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "请选择正确的答案",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        card.front,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Options
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.quizOptions.forEach { option ->
                val bgColor = when {
                    option.isDisabled && option.isCorrect -> jadeLight
                    option.isSelected && !option.isCorrect -> vermillionLight
                    else -> MaterialTheme.colorScheme.surface
                }
                val textColor = when {
                    option.isDisabled && option.isCorrect -> jade
                    option.isSelected && !option.isCorrect -> vermillion
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !option.isDisabled) { viewModel.answerQuiz(option) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        option.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        fontWeight = if (option.isDisabled) FontWeight.SemiBold else FontWeight.Normal
                    )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    learnState.progress,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        card.back,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        "请拼写单词，按回车提交",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Input
        val borderColor = when {
            uiState.typingIsCorrect == true -> jade
            uiState.typingIsCorrect == false -> vermillion
            else -> MaterialTheme.colorScheme.outlineVariant
        }
        val bgColor = when {
            uiState.typingIsCorrect == true -> jadeLight
            uiState.typingIsCorrect == false -> vermillionLight
            else -> MaterialTheme.colorScheme.surface
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = {
                if (!learnState.answered) {
                    inputText = it
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
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
                    if (!learnState.answered) {
                        viewModel.submitTypingAnswer(inputText)
                    } else {
                        viewModel.nextTypingQuestion()
                        inputText = ""
                    }
                }
            ),
            enabled = !learnState.answered || uiState.typingIsCorrect != null
        )

        // Feedback
        if (uiState.typingFeedback.isNotEmpty()) {
            Text(
                uiState.typingFeedback,
                style = MaterialTheme.typography.titleMedium,
                color = if (uiState.typingIsCorrect == true) jade else vermillion,
                modifier = Modifier.padding(vertical = 12.dp),
                fontWeight = FontWeight.SemiBold
            )
        }

        if (learnState.answered) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.nextTypingQuestion()
                    inputText = ""
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = jade,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("下一题")
            }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    uiState.resultTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    uiState.resultBank,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Stats grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatItem("总题数", uiState.resultTotal.toString(), Modifier.weight(1f))
                    StatItem("正确", uiState.resultRemembered.toString(), Modifier.weight(1f))
                    StatItem("正确率", uiState.resultAccuracy, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatItem("用时", uiState.resultDuration, Modifier.weight(1f))
                    StatItem("平均每题", uiState.resultAvgTime, Modifier.weight(1f))
                    StatItem("错误", uiState.resultForgotten.toString(), Modifier.weight(1f))
                }
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.retrySession() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("再来一轮")
            }
            OutlinedButton(
                onClick = { viewModel.goBackToSetup() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("返回选择")
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===== Pause Overlay =====
@Composable
private fun PauseOverlay(viewModel: LearnViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⏸️", fontSize = 36.sp)
                Text(
                    "已暂停",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    "进度已自动保存",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(
                    onClick = { viewModel.continueFromPause() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("继续学习")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.saveAndQuit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = vermillion
                    )
                ) {
                    Text("保存并退出")
                }
            }
        }
    }
}

@Composable
private fun SmallOutlinedButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(text, fontSize = 12.sp)
    }
}
