package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {

    private val sample: BackupData = mapOf(
        "profiles" to mapOf("list" to "1\t🐯\tAarav\n2\t🐼\tरिया", "current" to 2),
        "progress_1" to mapOf("stars" to 42, "ok|ENGLISH|A" to 3, "last_day" to 20729L, "path_plan|20729" to "ENGLISH:LEARN,ENGLISH:FIND"),
        "progress_2" to mapOf("cert|SWAR" to 20700L),
        "settings" to mapOf("music" to false, "daily_limit_minutes" to 30, "slow_voice" to true),
    )

    @Test
    fun aBackupReadsBackExactly() {
        val text = Backup.encode(sample)
        assertTrue(text.startsWith(Backup.HEADER))
        assertEquals(sample, Backup.decode(text))
        // Windows line endings (a file edited or sent around) read the same.
        assertEquals(sample, Backup.decode(text.replace("\n", "\r\n")))
    }

    @Test
    fun tabsLineBreaksAndBackslashesSurvive() {
        val tricky = mapOf("profiles" to mapOf("list" to "a\tb\nc\\d\\t\\"))
        assertEquals(tricky, Backup.decode(Backup.encode(tricky)))
    }

    @Test
    fun otherFilesAreRefused() {
        assertThrows(IllegalArgumentException::class.java) { Backup.decode("hello") }
        assertThrows(IllegalArgumentException::class.java) { Backup.decode("${Backup.HEADER}\nprofiles\tlist\ts") }
        assertThrows(IllegalArgumentException::class.java) { Backup.decode("${Backup.HEADER}\nprofiles\tlist\tx\t1") }
        // Only the app's own settings files can be written, never anything else.
        assertThrows(IllegalArgumentException::class.java) { Backup.decode("${Backup.HEADER}\nplay_time\tseconds\tl\t9") }
        // A backup must have children in it.
        assertThrows(IllegalArgumentException::class.java) { Backup.decode("${Backup.HEADER}\nsettings\tmusic\tb\ttrue") }
    }
}
