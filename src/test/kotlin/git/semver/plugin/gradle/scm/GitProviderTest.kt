package git.semver.plugin.gradle.scm

import git.semver.plugin.gradle.semver.SemverSettings
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull

class GitProviderTest {
    @Test
    fun testGetSemVersion() {
        val v = GitProvider(SemverSettings()).GetSemVersion(File("."))
        assertNotNull(v)
        println(v)
    }
}