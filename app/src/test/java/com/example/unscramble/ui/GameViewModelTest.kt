package com.example.unscramble.ui

import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.allWords
import com.example.unscramble.data.getUnscrambledWord
import org.junit.Assert.*
import org.junit.Test

class GameViewModelTest {
    private val viewModel = GameViewModel()

    private fun answerCurrentWord() {
        val answer = getUnscrambledWord(viewModel.uiState.value.currentScrambledWord)
        assertTrue("Every round must use a known word", answer.isNotEmpty())
        viewModel.updateUserGuess("  ${answer.uppercase()}  ")
        viewModel.checkUserGuess()
    }

    @Test fun initialState_hasScrambledWordAndZeroScore() {
        val state = viewModel.uiState.value
        val original = getUnscrambledWord(state.currentScrambledWord)
        assertTrue(original in allWords)
        assertNotEquals(original, state.currentScrambledWord)
        assertEquals(original.toList().sorted(), state.currentScrambledWord.toList().sorted())
        assertEquals(1, state.currentWordCount)
        assertEquals(0, state.score)
        assertFalse(state.isGameOver)
    }

    @Test fun incorrectAndEmptyAnswers_doNotAdvanceOrScore() {
        val initial = viewModel.uiState.value
        for (guess in listOf("", "not-a-valid-answer")) {
            viewModel.updateUserGuess(guess)
            viewModel.checkUserGuess()
            assertEquals(initial.copy(isGuessedWordWrong = true), viewModel.uiState.value)
            assertEquals("", viewModel.userGuess)
        }
    }

    @Test fun correctAnswer_ignoresCaseAndSurroundingSpaces() {
        answerCurrentWord()
        assertEquals(SCORE_INCREASE, viewModel.uiState.value.score)
        assertEquals(2, viewModel.uiState.value.currentWordCount)
        assertFalse(viewModel.uiState.value.isGuessedWordWrong)
        assertEquals("", viewModel.userGuess)
    }

    @Test fun skip_clearsErrorAndInputWithoutScoring() {
        viewModel.checkUserGuess()
        viewModel.updateUserGuess("pending")
        viewModel.skipWord()
        assertEquals(2, viewModel.uiState.value.currentWordCount)
        assertEquals(0, viewModel.uiState.value.score)
        assertFalse(viewModel.uiState.value.isGuessedWordWrong)
        assertEquals("", viewModel.userGuess)
    }

    @Test fun tenCorrectAnswers_endGameAtMaximumScoreWithoutRepeats() {
        val seen = mutableSetOf<String>()
        repeat(MAX_NO_OF_WORDS) { round ->
            val state = viewModel.uiState.value
            assertEquals(round + 1, state.currentWordCount)
            assertFalse(state.isGameOver)
            assertTrue(seen.add(getUnscrambledWord(state.currentScrambledWord)))
            answerCurrentWord()
        }
        assertTrue(viewModel.uiState.value.isGameOver)
        assertEquals(MAX_NO_OF_WORDS, viewModel.uiState.value.currentWordCount)
        assertEquals(MAX_NO_OF_WORDS * SCORE_INCREASE, viewModel.uiState.value.score)
    }

    @Test fun tenSkippedWords_endGameWithZeroScore() {
        repeat(MAX_NO_OF_WORDS) { viewModel.skipWord() }
        assertTrue(viewModel.uiState.value.isGameOver)
        assertEquals(MAX_NO_OF_WORDS, viewModel.uiState.value.currentWordCount)
        assertEquals(0, viewModel.uiState.value.score)
    }

    @Test fun mixedRounds_onlyCorrectAnswersAddPoints() {
        repeat(MAX_NO_OF_WORDS) { round ->
            if (round % 2 == 0) answerCurrentWord() else viewModel.skipWord()
        }
        assertTrue(viewModel.uiState.value.isGameOver)
        assertEquals(5 * SCORE_INCREASE, viewModel.uiState.value.score)
    }

    @Test fun completedGame_rejectsFurtherChanges() {
        repeat(MAX_NO_OF_WORDS) { answerCurrentWord() }
        val finalState = viewModel.uiState.value
        viewModel.updateUserGuess(getUnscrambledWord(finalState.currentScrambledWord))
        viewModel.checkUserGuess()
        viewModel.skipWord()
        assertEquals(finalState, viewModel.uiState.value)
        assertEquals("", viewModel.userGuess)
    }

    @Test fun reset_clearsPendingInputErrorAndCompletedGame() {
        answerCurrentWord()
        viewModel.checkUserGuess()
        viewModel.updateUserGuess("pending")
        viewModel.resetGame()
        assertEquals(0, viewModel.uiState.value.score)
        assertEquals(1, viewModel.uiState.value.currentWordCount)
        assertFalse(viewModel.uiState.value.isGuessedWordWrong)
        assertEquals("", viewModel.userGuess)
        repeat(MAX_NO_OF_WORDS) { viewModel.skipWord() }
        viewModel.resetGame()
        assertFalse(viewModel.uiState.value.isGameOver)
        assertEquals(1, viewModel.uiState.value.currentWordCount)
    }
}
