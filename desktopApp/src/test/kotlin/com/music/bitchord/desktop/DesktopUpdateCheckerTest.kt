package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Version ordering for the fork's releases (`1.9.1-meng.N`) next to upstream's own schemes. */
class DesktopUpdateCheckerTest {

    @Test
    fun forkRevisionOrdersReleasesOfTheSameVersion() {
        assertTrue(DesktopUpdateChecker.isNewer("1.9.1-meng.3", "1.9.1-meng.2"))
        assertFalse(DesktopUpdateChecker.isNewer("1.9.1-meng.2", "1.9.1-meng.3"))
        assertFalse(DesktopUpdateChecker.isNewer("1.9.1-meng.3", "1.9.1-meng.3"))
        assertTrue(DesktopUpdateChecker.isNewer("1.9.1-meng.10", "1.9.1-meng.9"))
    }

    @Test
    fun numbersOutrankTheForkRevision() {
        assertTrue(DesktopUpdateChecker.isNewer("1.9.2-meng.1", "1.9.1-meng.7"))
        assertTrue(DesktopUpdateChecker.isNewer("1.9.1-meng.3", "1.8-meng.2"))
    }

    @Test
    fun betaRulesStillHold() {
        assertTrue(DesktopUpdateChecker.isNewer("1.8", "1.8-beta1"))
        assertFalse(DesktopUpdateChecker.isNewer("1.8-beta1", "1.8"))
        assertTrue(DesktopUpdateChecker.isNewer("1.9.1-meng.1", "1.9.1"))
    }
}
