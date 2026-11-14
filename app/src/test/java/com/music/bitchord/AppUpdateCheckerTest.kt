package com.music.bitchord

import com.music.bitchord.data.AppUpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Version ordering for the fork's releases (`1.9.1-meng.N`) next to upstream's own schemes. */
class AppUpdateCheckerTest {

    @Test
    fun forkRevisionOrdersReleasesOfTheSameVersion() {
        assertTrue(AppUpdateChecker.isNewer("1.9.1-meng.3", "1.9.1-meng.2"))
        assertFalse(AppUpdateChecker.isNewer("1.9.1-meng.2", "1.9.1-meng.3"))
        assertFalse(AppUpdateChecker.isNewer("1.9.1-meng.3", "1.9.1-meng.3"))
        assertTrue(AppUpdateChecker.isNewer("1.9.1-meng.10", "1.9.1-meng.9"))
    }

    @Test
    fun numbersOutrankTheForkRevision() {
        assertTrue(AppUpdateChecker.isNewer("1.9.2-meng.1", "1.9.1-meng.7"))
        assertFalse(AppUpdateChecker.isNewer("1.9.1-meng.7", "1.9.2-meng.1"))
    }

    @Test
    fun aForkBuildIsNotAPreRelease() {
        assertTrue(AppUpdateChecker.isNewer("1.9.1-meng.1", "1.9.1"))
        assertFalse(AppUpdateChecker.isNewer("1.9.1", "1.9.1-meng.1"))
    }

    @Test
    fun betaRulesStillHold() {
        assertTrue(AppUpdateChecker.isNewer("1.9.1", "1.9.1-beta2"))
        assertTrue(AppUpdateChecker.isNewer("1.9.1-meng.3", "1.9.1-meng.3-beta1"))
        assertTrue(AppUpdateChecker.isNewer("1.10", "1.9.1"))
    }
}
