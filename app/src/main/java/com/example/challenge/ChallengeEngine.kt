package com.example.challenge

import androidx.compose.ui.graphics.Color
import com.example.model.ChallengeCategory
import com.example.model.ChallengeDifficulty
import kotlin.random.Random

data class ColorOption(
    val name: String,
    val color: Color
)

sealed class ChallengeItem {
    data class MathQuestion(
        val questionText: String,
        val correctAnswer: Int,
        val choices: List<Int>
    ) : ChallengeItem()

    data class SentenceQuestion(
        val sentenceText: String
    ) : ChallengeItem()

    data class ColorRecognitionQuestion(
        val wordText: String,
        val inkColorName: String,
        val inkColor: Color,
        val choices: List<ColorOption>
    ) : ChallengeItem()

    data class MemorySequenceQuestion(
        val sequenceItems: List<String>,
        val allChoices: List<String>
    ) : ChallengeItem()

    data class SequenceQuestion(
        val sequenceDisplay: String,
        val correctAnswer: Int,
        val choices: List<Int>
    ) : ChallengeItem()
}

object ChallengeEngine {

    val AVAILABLE_COLORS = listOf(
        ColorOption("RED", Color(0xFFFF453A)),
        ColorOption("BLUE", Color(0xFF0A84FF)),
        ColorOption("GREEN", Color(0xFF30D158)),
        ColorOption("YELLOW", Color(0xFFFFD60A)),
        ColorOption("PURPLE", Color(0xFFBF5AF2)),
        ColorOption("ORANGE", Color(0xFFFF9F0A))
    )

    private val EASY_SENTENCES = listOf(
        "Breathe in peace and focus now.",
        "Today is full of new possibilities.",
        "Small steps lead to great progress.",
        "I am fully present and awake.",
        "Calm mind, strong spirit, clear path."
    )

    private val MEDIUM_SENTENCES = listOf(
        "Discipline today creates freedom and calm tomorrow.",
        "Stay present and give your best to this precious day.",
        "Great achievements are made of small deeds done consistently.",
        "Wake up with determination and go to bed with satisfaction.",
        "Your focus determines your reality; choose clarity."
    )

    private val HARD_SENTENCES = listOf(
        "The secret of getting ahead is getting started; break complex tasks into small steps.",
        "Focus is not about saying yes to one thing, but saying no to a hundred distractions.",
        "Continuous effort, not strength or intelligence, is the key to unlocking our potential.",
        "Success is the sum of small mindful habits repeated day in and day out without fail."
    )

    fun generateChallenge(
        category: ChallengeCategory,
        difficulty: ChallengeDifficulty
    ): ChallengeItem {
        return when (category) {
            ChallengeCategory.MATH -> generateMathQuestion(difficulty)
            ChallengeCategory.SENTENCE_TYPING -> generateSentenceQuestion(difficulty)
            ChallengeCategory.COLOR_RECOGNITION -> generateColorRecognitionQuestion()
            ChallengeCategory.MEMORY -> generateMemoryQuestion(difficulty)
            ChallengeCategory.SEQUENCE -> generateSequenceQuestion(difficulty)
        }
    }

    private fun generateMathQuestion(difficulty: ChallengeDifficulty): ChallengeItem.MathQuestion {
        val op = when (difficulty) {
            ChallengeDifficulty.EASY -> listOf("+", "-").random()
            ChallengeDifficulty.MEDIUM -> listOf("+", "-", "*").random()
            ChallengeDifficulty.HARD -> listOf("+", "-", "*", "/").random()
        }

        var num1: Int
        var num2: Int
        var answer: Int

        when (op) {
            "+" -> {
                when (difficulty) {
                    ChallengeDifficulty.EASY -> {
                        num1 = Random.nextInt(10, 40)
                        num2 = Random.nextInt(5, 30)
                    }
                    ChallengeDifficulty.MEDIUM -> {
                        num1 = Random.nextInt(25, 80)
                        num2 = Random.nextInt(20, 60)
                    }
                    ChallengeDifficulty.HARD -> {
                        num1 = Random.nextInt(120, 450)
                        num2 = Random.nextInt(85, 350)
                    }
                }
                answer = num1 + num2
            }
            "-" -> {
                when (difficulty) {
                    ChallengeDifficulty.EASY -> {
                        num1 = Random.nextInt(20, 60)
                        num2 = Random.nextInt(5, num1 - 5)
                    }
                    ChallengeDifficulty.MEDIUM -> {
                        num1 = Random.nextInt(50, 130)
                        num2 = Random.nextInt(15, num1 - 10)
                    }
                    ChallengeDifficulty.HARD -> {
                        num1 = Random.nextInt(200, 600)
                        num2 = Random.nextInt(50, num1 - 20)
                    }
                }
                answer = num1 - num2
            }
            "*" -> {
                when (difficulty) {
                    ChallengeDifficulty.EASY -> {
                        num1 = Random.nextInt(3, 9)
                        num2 = Random.nextInt(3, 9)
                    }
                    ChallengeDifficulty.MEDIUM -> {
                        num1 = Random.nextInt(6, 14)
                        num2 = Random.nextInt(4, 12)
                    }
                    ChallengeDifficulty.HARD -> {
                        num1 = Random.nextInt(12, 22)
                        num2 = Random.nextInt(8, 18)
                    }
                }
                answer = num1 * num2
            }
            else -> { // "/"
                val divisor = Random.nextInt(3, 12)
                val quotient = when (difficulty) {
                    ChallengeDifficulty.EASY -> Random.nextInt(3, 9)
                    ChallengeDifficulty.MEDIUM -> Random.nextInt(8, 20)
                    ChallengeDifficulty.HARD -> Random.nextInt(15, 45)
                }
                num1 = divisor * quotient
                num2 = divisor
                answer = quotient
            }
        }

        val question = "$num1 $op $num2"
        val wrongChoices = mutableSetOf<Int>()
        while (wrongChoices.size < 3) {
            val delta = listOf(-10, -5, -2, -1, 1, 2, 5, 10, 15).random()
            val candidate = answer + delta
            if (candidate != answer && candidate >= 0) {
                wrongChoices.add(candidate)
            }
        }
        val allChoices = (wrongChoices + answer).shuffled()

        return ChallengeItem.MathQuestion(question, answer, allChoices)
    }

    private fun generateSentenceQuestion(difficulty: ChallengeDifficulty): ChallengeItem.SentenceQuestion {
        val list = when (difficulty) {
            ChallengeDifficulty.EASY -> EASY_SENTENCES
            ChallengeDifficulty.MEDIUM -> MEDIUM_SENTENCES
            ChallengeDifficulty.HARD -> HARD_SENTENCES
        }
        return ChallengeItem.SentenceQuestion(list.random())
    }

    private fun generateColorRecognitionQuestion(): ChallengeItem.ColorRecognitionQuestion {
        val wordColor = AVAILABLE_COLORS.random()
        var inkColor = AVAILABLE_COLORS.random()
        // Ensure Stroop effect where word != ink
        while (inkColor.name == wordColor.name) {
            inkColor = AVAILABLE_COLORS.random()
        }

        val wrongColors = AVAILABLE_COLORS.filter { it.name != inkColor.name }.shuffled().take(3)
        val choices = (wrongColors + inkColor).shuffled()

        return ChallengeItem.ColorRecognitionQuestion(
            wordText = wordColor.name,
            inkColorName = inkColor.name,
            inkColor = inkColor.color,
            choices = choices
        )
    }

    private fun generateMemoryQuestion(difficulty: ChallengeDifficulty): ChallengeItem.MemorySequenceQuestion {
        val count = when (difficulty) {
            ChallengeDifficulty.EASY -> 3
            ChallengeDifficulty.MEDIUM -> 4
            ChallengeDifficulty.HARD -> 5
        }
        val symbols = listOf("★", "●", "▲", "◆", "✦", "■")
        val sequence = (1..count).map { symbols.random() }
        return ChallengeItem.MemorySequenceQuestion(
            sequenceItems = sequence,
            allChoices = symbols.shuffled()
        )
    }

    private fun generateSequenceQuestion(difficulty: ChallengeDifficulty): ChallengeItem.SequenceQuestion {
        val type = Random.nextInt(0, 3)
        var seq: List<Int>
        var nextVal: Int

        when (type) {
            0 -> { // Arithmetic (+step)
                val step = when (difficulty) {
                    ChallengeDifficulty.EASY -> Random.nextInt(2, 6)
                    ChallengeDifficulty.MEDIUM -> Random.nextInt(6, 15)
                    ChallengeDifficulty.HARD -> Random.nextInt(12, 28)
                }
                val start = Random.nextInt(1, 20)
                seq = (0..3).map { start + it * step }
                nextVal = start + 4 * step
            }
            1 -> { // Geometric (*2 or *3)
                val ratio = if (difficulty == ChallengeDifficulty.EASY) 2 else 3
                val start = Random.nextInt(1, 4)
                seq = (0..3).map { (start * Math.pow(ratio.toDouble(), it.toDouble())).toInt() }
                nextVal = (start * Math.pow(ratio.toDouble(), 4.0)).toInt()
            }
            else -> { // Squares or step-increments (+1, +2, +3, +4...)
                val start = Random.nextInt(1, 5)
                var curr = start
                val list = mutableListOf<Int>()
                for (i in 1..4) {
                    list.add(curr)
                    curr += i * (if (difficulty == ChallengeDifficulty.HARD) 3 else 2)
                }
                seq = list
                nextVal = curr
            }
        }

        val display = "${seq.joinToString(", ")},  [ ? ]"
        val wrongChoices = mutableSetOf<Int>()
        while (wrongChoices.size < 3) {
            val delta = listOf(-6, -4, -2, -1, 1, 2, 4, 6, 8).random()
            val candidate = nextVal + delta
            if (candidate != nextVal && candidate > 0) {
                wrongChoices.add(candidate)
            }
        }
        val choices = (wrongChoices + nextVal).shuffled()

        return ChallengeItem.SequenceQuestion(display, nextVal, choices)
    }
}
