package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.calculator.CalculatorEngine
import com.example.data.security.PinMatchResult
import com.example.data.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Calculator", appName)
    }

    @Test
    fun `test calculator basic math`() {
        val res1 = CalculatorEngine.evaluate("2 + 3 × 4")
        assertEquals("14", res1.resultString)

        val res2 = CalculatorEngine.evaluate("100 ÷ 4")
        assertEquals("25", res2.resultString)
    }

    @Test
    fun `test security manager pin verification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sec = SecurityManager(context)
        sec.setupInitialPins(
            primaryPin = "1234",
            decoyPin = "4321",
            backupPin = "9999",
            securityQuestion = "What is your secret?",
            securityAnswer = "Sesame"
        )

        assertEquals(PinMatchResult.PRIMARY, sec.checkPin("1234"))
        assertEquals(PinMatchResult.DECOY, sec.checkPin("4321"))
        assertEquals(PinMatchResult.BACKUP, sec.checkPin("9999"))
        assertEquals(PinMatchResult.NONE, sec.checkPin("0000"))

        assertTrue(sec.verifyBackupPin("9999"))
        assertTrue(sec.verifySecurityAnswer("sesame"))
    }
}
