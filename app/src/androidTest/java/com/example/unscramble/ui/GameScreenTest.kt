package com.example.unscramble.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.unscramble.MainActivity
import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.allWords
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun model(): GameViewModel = compose.runOnIdle {
        ViewModelProvider(compose.activity)[GameViewModel::class.java]
    }

    private fun answer(): String {
        val scrambled = model().uiState.value.currentScrambledWord
        return allWords.first { it.toList().sorted() == scrambled.toList().sorted() }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val directory = instrumentation.targetContext.getExternalFilesDir("evidence")!!
        directory.mkdirs()
        File(directory, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun correctAnswerAndPendingInput_surviveRotationAndActivityRecreation() {
        compose.onNode(hasSetTextAction()).performTextInput(answer())
        compose.onNodeWithText("Submit").performClick()
        compose.onNodeWithText("Score: 20").assertIsDisplayed()
        compose.onNodeWithText("2/10").assertIsDisplayed()
        screenshot("01-juego.png")
        compose.onNode(hasSetTextAction()).performTextInput("pending")
        val originalModel = model()
        val originalState = originalModel.uiState.value
        compose.runOnUiThread {
            compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        compose.waitUntil(10_000) {
            compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        compose.waitForIdle()
        assertSame(originalModel, model())
        assertEquals(originalState, model().uiState.value)
        compose.onNode(hasSetTextAction()).assertTextContains("pending")
        compose.runOnUiThread {
            compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        compose.waitUntil(10_000) {
            compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        }
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        assertSame(originalModel, model())
        assertEquals(originalState, model().uiState.value)
        compose.onNode(hasSetTextAction()).assertTextContains("pending")
        compose.onNodeWithText("Score: 20").assertIsDisplayed()
    }

    @Test fun wrongAnswer_showsErrorAndSkipClearsIt() {
        compose.onNode(hasSetTextAction()).performTextInput("invalid-answer")
        compose.onNodeWithText("Submit").performClick()
        compose.onNodeWithText("Wrong Guess!").assertIsDisplayed()
        compose.onNodeWithText("1/10").assertIsDisplayed()
        screenshot("02-error.png")
        compose.onNodeWithText("Skip").performClick()
        compose.onNodeWithText("2/10").assertIsDisplayed()
        compose.onNodeWithText("Score: 0").assertIsDisplayed()
        compose.onNodeWithText("Enter your word").assertIsDisplayed()
    }

    @Test fun keyboardDone_submitsAnswer() {
        compose.onNode(hasSetTextAction()).performTextInput(answer())
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText("Score: 20").assertIsDisplayed()
        compose.onNodeWithText("2/10").assertIsDisplayed()
    }

    @Test fun finalDialog_showsScoreAndPlayAgainResetsGame() {
        repeat(MAX_NO_OF_WORDS) {
            compose.onNode(hasSetTextAction()).performTextReplacement(answer())
            compose.onNodeWithText("Submit").performClick()
        }
        compose.onNodeWithText("Congratulations!").assertIsDisplayed()
        compose.onNodeWithText("You scored: 200").assertIsDisplayed()
        screenshot("03-final.png")
        compose.onNodeWithText("Play Again").performClick()
        compose.onNodeWithText("1/10").assertIsDisplayed()
        compose.onNodeWithText("Score: 0").assertIsDisplayed()
    }
}
