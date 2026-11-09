package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.challenge.ChallengeEngine
import com.example.challenge.ChallengeItem
import com.example.model.ChallengeCategory
import com.example.model.ChallengeConfig
import com.example.model.ChallengeDifficulty
import com.example.ui.theme.OpalBlack
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChallengeScreen(
    config: ChallengeConfig,
    isTestMode: Boolean,
    onChallengeCompleted: () -> Unit,
    onDismissTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var consecutiveCorrect by remember { mutableIntStateOf(0) }
    val requiredConsecutive = config.requiredConsecutive.coerceAtLeast(1)

    // Current challenge item
    var currentItem by remember {
        mutableStateOf(ChallengeEngine.generateChallenge(config.category, config.difficulty))
    }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessAnimating by remember { mutableStateOf(false) }

    // Sentence typing state
    var typedSentence by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Memory sequence state
    var memorySequenceHidden by remember { mutableStateOf(false) }
    val userTappedMemorySequence = remember { mutableStateListOf<String>() }

    fun refreshChallenge() {
        val nextCategory = if (config.isRandomizedCategory) {
            ChallengeCategory.values().random()
        } else config.category

        currentItem = ChallengeEngine.generateChallenge(nextCategory, config.difficulty)
        typedSentence = ""
        userTappedMemorySequence.clear()
        memorySequenceHidden = false
    }

    // Auto-hide memory sequence after 2.5 seconds
    LaunchedEffect(currentItem) {
        if (currentItem is ChallengeItem.MemorySequenceQuestion) {
            memorySequenceHidden = false
            delay(2500L)
            memorySequenceHidden = true
        }
    }

    fun handleCorrectAnswer() {
        consecutiveCorrect += 1
        feedbackMessage = "Correct! ($consecutiveCorrect/$requiredConsecutive)"
        if (consecutiveCorrect >= requiredConsecutive) {
            isSuccessAnimating = true
        } else {
            refreshChallenge()
        }
    }

    fun handleWrongAnswer(explanation: String = "Incorrect! Streak reset to 0.") {
        consecutiveCorrect = 0
        feedbackMessage = explanation
        refreshChallenge()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OpalBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Challenge",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isTestMode) "Test Challenge" else "Wake-Up Challenge",
                            color = OpalTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${config.category.displayName} · ${config.difficulty.displayName}",
                            color = OpalTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isTestMode) {
                    IconButton(
                        onClick = onDismissTest,
                        modifier = Modifier.testTag("test_challenge_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Test",
                            tint = OpalTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Consecutive Correct Progress Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OpalCardBg)
                    .border(1.dp, OpalCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Consecutive: $consecutiveCorrect of $requiredConsecutive",
                    color = OpalTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..requiredConsecutive) {
                        val isDone = i <= consecutiveCorrect
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isDone) OpalMint else Color(0xFF25292E))
                        )
                    }
                }
            }

            // Feedback alert if any
            feedbackMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = if (msg.startsWith("Correct")) OpalMint else Color(0xFFFF5252),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Challenge Card
            if (!isSuccessAnimating) {
                when (val item = currentItem) {
                    is ChallengeItem.MathQuestion -> {
                        MathChallengeView(
                            question = item,
                            onAnswerSelected = { answer ->
                                if (answer == item.correctAnswer) {
                                    handleCorrectAnswer()
                                } else {
                                    handleWrongAnswer("Wrong! $answer was incorrect.")
                                }
                            }
                        )
                    }
                    is ChallengeItem.SentenceQuestion -> {
                        SentenceChallengeView(
                            sentence = item.sentenceText,
                            typedText = typedSentence,
                            onTypedChange = { typedSentence = it },
                            onSubmit = {
                                keyboardController?.hide()
                                if (typedSentence.trim() == item.sentenceText.trim()) {
                                    handleCorrectAnswer()
                                } else {
                                    handleWrongAnswer("Sentence did not match exactly.")
                                }
                            }
                        )
                    }
                    is ChallengeItem.ColorRecognitionQuestion -> {
                        ColorRecognitionChallengeView(
                            question = item,
                            onOptionSelected = { chosenColor ->
                                if (chosenColor.name == item.inkColorName) {
                                    handleCorrectAnswer()
                                } else {
                                    handleWrongAnswer("Wrong color chosen! Match the ink color.")
                                }
                            }
                        )
                    }
                    is ChallengeItem.MemorySequenceQuestion -> {
                        MemoryChallengeView(
                            question = item,
                            isSequenceHidden = memorySequenceHidden,
                            userSelected = userTappedMemorySequence,
                            onTapItem = { symbol ->
                                userTappedMemorySequence.add(symbol)
                                if (userTappedMemorySequence.size == item.sequenceItems.size) {
                                    if (userTappedMemorySequence == item.sequenceItems) {
                                        handleCorrectAnswer()
                                    } else {
                                        handleWrongAnswer("Sequence mismatch! Try again.")
                                    }
                                }
                            },
                            onResetSelection = {
                                userTappedMemorySequence.clear()
                            }
                        )
                    }
                    is ChallengeItem.SequenceQuestion -> {
                        SequenceChallengeView(
                            question = item,
                            onAnswerSelected = { answer ->
                                if (answer == item.correctAnswer) {
                                    handleCorrectAnswer()
                                } else {
                                    handleWrongAnswer("Incorrect sequence completion.")
                                }
                            }
                        )
                    }
                }
            } else {
                // Success Badge Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F2E22), Color(0xFF081A13))
                            )
                        )
                        .border(1.dp, OpalMint.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = OpalMint,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isTestMode) "Test Passed!" else "Challenge Cleared!",
                            color = OpalTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isTestMode) {
                                "You successfully solved the test challenge. Difficulty verified."
                            } else {
                                "Alarm dismissed! Have a mindful, focused day."
                            },
                            color = OpalTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onChallengeCompleted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OpalMint,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("challenge_done_button")
                        ) {
                            Text(
                                text = if (isTestMode) "Return to Settings" else "Turn Off Alarm",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun MathChallengeView(
    question: ChallengeItem.MathQuestion,
    onAnswerSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(OpalCardBg)
                .border(1.dp, OpalCardBorder, RoundedCornerShape(24.dp))
                .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${question.questionText} = ?",
                color = OpalTextPrimary,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("math_question_text")
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Multiple choice 2x2 grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val chunked = question.choices.chunked(2)
            chunked.forEach { rowChoices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowChoices.forEach { choice ->
                        Button(
                            onClick = { onAnswerSelected(choice) },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E232A),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("math_choice_$choice")
                        ) {
                            Text(
                                text = "$choice",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SentenceChallengeView(
    sentence: String,
    typedText: String,
    onTypedChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(OpalCardBg)
                .border(1.dp, OpalCardBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "TYPE THIS SENTENCE EXACTLY:",
                    color = OpalTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "\"$sentence\"",
                    color = OpalTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = typedText,
            onValueChange = onTypedChange,
            placeholder = { Text("Type here...", color = OpalTextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = OpalMint,
                unfocusedBorderColor = OpalCardBorder,
                focusedContainerColor = OpalCardBg,
                unfocusedContainerColor = OpalCardBg
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sentence_input_field"),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSubmit,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_sentence_button")
        ) {
            Text("Verify & Submit", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ColorRecognitionChallengeView(
    question: ChallengeItem.ColorRecognitionQuestion,
    onOptionSelected: (com.example.challenge.ColorOption) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(OpalCardBg)
                .border(1.dp, OpalCardBorder, RoundedCornerShape(24.dp))
                .padding(vertical = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "WHAT IS THE TEXT COLOR?",
                    color = OpalTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = question.wordText,
                    color = question.inkColor,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.testTag("stroop_word")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4 color buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val chunked = question.choices.chunked(2)
            chunked.forEach { rowChoices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowChoices.forEach { choice ->
                        Button(
                            onClick = { onOptionSelected(choice) },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E232A),
                                contentColor = choice.color
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .testTag("color_choice_${choice.name}")
                        ) {
                            Text(
                                text = choice.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryChallengeView(
    question: ChallengeItem.MemorySequenceQuestion,
    isSequenceHidden: Boolean,
    userSelected: List<String>,
    onTapItem: (String) -> Unit,
    onResetSelection: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(OpalCardBg)
                .border(1.dp, OpalCardBorder, RoundedCornerShape(24.dp))
                .padding(vertical = 32.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (!isSequenceHidden) "MEMORIZE THE SEQUENCE:" else "REPRODUCE IN ORDER:",
                    color = OpalTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (!isSequenceHidden) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        question.sequenceItems.forEach { sym ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF262C34)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sym,
                                    color = Color(0xFF00E5FF),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Show current user entered slots
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (i in question.sequenceItems.indices) {
                            val userVal = userSelected.getOrNull(i)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (userVal != null) OpalMint.copy(alpha = 0.2f) else Color(0xFF1E2228))
                                    .border(
                                        1.dp,
                                        if (userVal != null) OpalMint else OpalCardBorder,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userVal ?: "?",
                                    color = if (userVal != null) OpalMint else OpalTextSecondary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isSequenceHidden) {
            // Choices keyboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                question.allChoices.forEach { symbol ->
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF222830))
                            .clickable { onTapItem(symbol) }
                            .testTag("memory_sym_$symbol"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = symbol,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            IconButton(onClick = onResetSelection) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Clear Sequence",
                    tint = OpalTextSecondary
                )
            }
        }
    }
}

@Composable
private fun SequenceChallengeView(
    question: ChallengeItem.SequenceQuestion,
    onAnswerSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(OpalCardBg)
                .border(1.dp, OpalCardBorder, RoundedCornerShape(24.dp))
                .padding(vertical = 32.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FIND THE NEXT NUMBER:",
                    color = OpalTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = question.sequenceDisplay,
                    color = OpalTextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("sequence_display")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Choices 2x2 grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val chunked = question.choices.chunked(2)
            chunked.forEach { rowChoices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowChoices.forEach { choice ->
                        Button(
                            onClick = { onAnswerSelected(choice) },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E232A),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .testTag("sequence_choice_$choice")
                        ) {
                            Text(
                                text = "$choice",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
