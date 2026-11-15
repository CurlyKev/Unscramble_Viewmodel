/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.unscramble.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.allWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Owns game data and rules independently of the composables. */
class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    var userGuess by mutableStateOf("")
        private set

    private lateinit var currentWord: String
    private val usedWords = mutableSetOf<String>()

    init {
        resetGame()
    }

    fun updateUserGuess(guessedWord: String) {
        if (!_uiState.value.isGameOver) userGuess = guessedWord
    }

    fun checkUserGuess() {
        if (_uiState.value.isGameOver) return
        if (userGuess.trim().equals(currentWord, ignoreCase = true)) {
            updateGameState(_uiState.value.score + SCORE_INCREASE)
        } else {
            _uiState.update { it.copy(isGuessedWordWrong = true) }
        }
        userGuess = ""
    }

    fun skipWord() {
        if (_uiState.value.isGameOver) return
        updateGameState(_uiState.value.score)
        userGuess = ""
    }

    fun resetGame() {
        usedWords.clear()
        userGuess = ""
        _uiState.value = GameUiState(currentScrambledWord = pickRandomWordAndShuffle())
    }

    private fun updateGameState(updatedScore: Int) {
        // Finish on round 10 without drawing an eleventh word.
        if (_uiState.value.currentWordCount == MAX_NO_OF_WORDS) {
            _uiState.update {
                it.copy(score = updatedScore, isGuessedWordWrong = false, isGameOver = true)
            }
        } else {
            val nextWord = pickRandomWordAndShuffle()
            _uiState.update {
                it.copy(
                    currentScrambledWord = nextWord,
                    currentWordCount = it.currentWordCount + 1,
                    score = updatedScore,
                    isGuessedWordWrong = false
                )
            }
        }
    }

    private fun pickRandomWordAndShuffle(): String {
        currentWord = (allWords - usedWords).random()
        usedWords.add(currentWord)
        val letters = currentWord.toCharArray()
        do {
            letters.shuffle()
        } while (String(letters) == currentWord)
        return String(letters)
    }
}
