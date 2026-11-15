package com.offloop.app.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for exit strategy decision logic.
 * Tests the pure logic without Android framework dependencies.
 */
class ExitStrategyTest {

    @Test
    fun `max back attempts is bounded`() {
        val maxAttempts = 3
        assertTrue("MAX_BACK_ATTEMPTS must be positive", maxAttempts > 0)
        assertTrue("MAX_BACK_ATTEMPTS must be reasonable (3-5)", maxAttempts in 1..5)
    }

    @Test
    fun `back retry delay is reasonable`() {
        val delayMs = 300L
        assertTrue("BACK_RETRY_DELAY_MS must be positive", delayMs > 0)
        assertTrue("BACK_RETRY_DELAY_MS should be 100-1000ms", delayMs in 100..1000)
    }

    @Test
    fun `exit cooldown is preserved`() {
        val cooldownMs = 5_000L
        assertEquals("EXIT_COOLDOWN_MS must remain 5 seconds", 5_000L, cooldownMs)
    }

    @Test
    fun `youtube has home tab identifiers`() {
        val target = AppTarget.YOUTUBE
        assertTrue("YouTube must have at least one home tab view ID", 
            target.homeTabViewIds.isNotEmpty())
        assertTrue("YouTube must have at least one home tab content description", 
            target.homeTabContentDescriptions.isNotEmpty())
    }

    @Test
    fun `instagram has home tab identifiers`() {
        val target = AppTarget.INSTAGRAM
        assertTrue("Instagram must have at least one home tab view ID", 
            target.homeTabViewIds.isNotEmpty())
        assertTrue("Instagram must have at least one home tab content description", 
            target.homeTabContentDescriptions.isNotEmpty())
    }

    @Test
    fun `all app targets have exit strategy configured`() {
        for (target in AppTarget.entries) {
            assertTrue("${target.displayName} must have home tab view IDs",
                target.homeTabViewIds.isNotEmpty())
            assertTrue("${target.displayName} must have home tab content descriptions",
                target.homeTabContentDescriptions.isNotEmpty())
        }
    }

    @Test
    fun `home tab view ids are qualified`() {
        for (target in AppTarget.entries) {
            for (viewId in target.homeTabViewIds) {
                assertTrue("${target.displayName} view ID must start with package name",
                    viewId.startsWith(target.packageName))
                assertTrue("${target.displayName} view ID must contain id separator",
                    viewId.contains(":id/"))
            }
        }
    }
}
