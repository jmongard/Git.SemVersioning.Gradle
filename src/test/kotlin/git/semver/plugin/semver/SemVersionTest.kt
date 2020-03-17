package git.semver.plugin.semver

import git.semver.plugin.gradle.semver.SemverSettings
import git.semver.plugin.gradle.scm.Commit
import git.semver.plugin.gradle.scm.Tag
import git.semver.plugin.gradle.semver.SemVersion
import kotlin.test.*


class SemVersionTest {
    companion object {
        const val SHA = "8727a3e1234567"
    }

    private val settings = SemverSettings()

    @Test
    fun `test invalid tags`() {
        assertNull(SemVersion.tryParse(Tag("foo", SHA)))
        assertNull(SemVersion.tryParse(Tag("foo", SHA)))
        assertNull(SemVersion.tryParse(Tag("v1", SHA)))
        assertNull(SemVersion.tryParse(Tag("v1.", SHA)))
        assertNull(SemVersion.tryParse(Tag("va1,2", SHA)))
        assertNull(SemVersion.tryParse(Tag("av1,2", SHA)))
        assertNull(SemVersion.tryParse(Tag("v1.-2", SHA)))
        assertNull(SemVersion.tryParse(Tag("v-1,2", SHA)))
    }

    @Test
    fun `valid version tags`() {
        checkValid("v1.2", 1, 2, 0, null, null)
        checkValid("v1.2-alpha", 1, 2, 0, "alpha", null)
        checkValid("v1.2-alpha5", 1, 2, 0, "alpha", 5)
        checkValid("v1.2-betaV.5", 1, 2, 0, "betaV.", 5)
        checkValid("v1.2.0-beta.5", 1, 2, 0, "beta.", 5)
        checkValid("v1.2.3-beta.5", 1, 2, 3, "beta.", 5)
        checkValid("v1.2.3-5", 1, 2, 3, null, 5)
        checkValid("v1.2.3-alpha.beta", 1, 2, 3, "alpha.beta", null)
        checkValid("1.2.3.4", 1, 2, 3, null, null)
        checkValid("v9.5.0.41-rc", 9, 5, 0, "rc", 41)
    }

    private fun checkValid(tagName: String, majorVersion: Int, minorVersion: Int, patchVersion: Int, suffix: String?, preRelease: Int?) {
        val version = assertNotNull(SemVersion.tryParse(Tag(tagName, SHA)))
        assertEquals(majorVersion, version.majorVersion)
        assertEquals(minorVersion, version.minorVersion)
        assertEquals(patchVersion, version.patchVersion)
        assertEquals(suffix, version.preReleasePrefix)
        assertEquals(preRelease, version.preReleaseVersion)
    }

    @Test
    fun `test SemVer ordering`() {
        assertTrue(SemVersion.tryParse(Tag("v2.0", SHA))!! > SemVersion.tryParse(Tag("v1.0", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.2", SHA))!! > SemVersion.tryParse(Tag("v1.1", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.1", SHA))!! > SemVersion.tryParse(Tag("v1.1.0", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-RC", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0-RC.0", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-RC", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0-Beta", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-Alpha", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0-Beta.1", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-Alpha.2", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0-RC2", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-RC1", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0-RC.2", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-RC.1", SHA))!!)
    }

    @Test
    fun `test SemVer ordering differing commit count`() {
        val v1 = SemVersion()
        val v2 = SemVersion()
        v2.updateFromCommit(Commit("", SHA, emptySequence()), settings)
        assertTrue(v1.compareTo(SemVersion()) == 0)
        assertTrue(v1 < v2)
    }

    @Test
    fun `test preRelease version`() {
        val pre = assertNotNull(SemVersion.tryParse(Tag("1.0.0-RC.2", SHA)))
        val version = SemVersion()
        version.updateFromCommit(Commit("tagged preRelease", SHA, emptySequence()), settings, pre)
        version.updateFromCommit(Commit("text", SHA, emptySequence()), settings)
        version.updateFromCommit(Commit("text", SHA, emptySequence()), settings)
        version.updateFromCommit(Commit("text", SHA, emptySequence()), settings)
        version.calculateNewVersion(false, settings.defaultPreRelease)
        assertEquals("1.0.0-RC.3+003", version.toInfoVersionString())
    }

    @Test
    fun `test update from commit`() {
        assertEquals("1.1.1-SNAPSHOT+003", getVersionFromTagAndCommits("v1.1.0", arrayOf("Commit 1", "Commit 2", "Commit 3")))
        assertEquals("1.2.1-SNAPSHOT+003", getVersionFromTagAndCommits("v1.2", arrayOf("Commit 1", "Commit 2", "Commit 3")))
        assertEquals("2.2.3-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("Commit 1")))
        assertEquals("3.0.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("refactor!: drop some support")))
        assertEquals("3.0.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("feat:  new api\r\n\r\nReplacing the old API\r\n\r\nBREAKING CHANGE: drop support")))
        assertEquals("2.3.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("feat: new")))
        assertEquals("2.2.3-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("fix: bug")))
        assertEquals("1.0.2-SNAPSHOT+002", getVersionFromTagAndCommits("v1.0.0", arrayOf("fix: bug", "fix: bug")))

        assertEquals("3.0.0", getVersionFromTagAndCommits("v2.2.2", arrayOf("refactor!: drop some support", "release: it")))
        assertEquals("2.2.3", getVersionFromTagAndCommits("v2.2.2", arrayOf("fix:   bug", "release: ok")))
        assertEquals("1.0.2", getVersionFromTagAndCommits("v1.0.0", arrayOf("fix:   bug", "fix: bug", "release: ok")))
        assertEquals("1.0.4-SNAPSHOT+002", getVersionFromTagAndCommits("v1.0.0", arrayOf("fix:   bug", "fix: bug", "release: ok", "fix: bug", "fix: bug")))
        assertEquals("2.3.0", getVersionFromTagAndCommits("v2.2.2", arrayOf("feat:  wow", "release: ok")))
        assertEquals("3.0.0", getVersionFromTagAndCommits("v2.2.2", arrayOf("feat!: WOW", "release: ok")))
        assertEquals("2.2.4-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("fix:   bug", "release: ok", "fix: bug")))
        assertEquals("2.3.1-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("feat:  new", "release: ok", "fix: bug")))
        assertEquals("3.0.1-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("feat!: WOW", "release: ok", "fix: bug")))
        assertEquals("2.3.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("fix:   bug", "release: ok", "feat: new")))
        assertEquals("2.4.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("feat:  new", "release: ok", "feat: new")))
        assertEquals("3.1.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("feat!: WOW", "release: ok", "feat: new")))
        assertEquals("3.0.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("fix:   bug", "release: ok", "feat!: WOW")))
        assertEquals("3.0.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("feat:  new", "release: ok", "feat!: WOW")))
        assertEquals("4.0.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", arrayOf("feat!: WOW", "release: ok", "feat!: WOW")))
    }

    private fun getVersionFromTagAndCommits(tagName: String, commits: Array<String>): String {
        val version = assertNotNull(SemVersion.tryParse(Tag(tagName, SHA)))
        commits.forEach { version.updateFromCommit(Commit(it, SHA, emptySequence()), settings) }
        version.calculateNewVersion(false, settings.defaultPreRelease)
        return version.toInfoVersionString()
    }

    @Test
    fun `test update from commit pre-release dirty`() {
        assertEquals("1.1.1-Beta.3+001", getVersionFromTagAndPreAndCommitDirty("v1.1.0", "1.1.1-Beta.2", "Commit 1"))
        assertEquals("2.2.3-Alpha+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Alpha", "Commit 1"))
        assertEquals("2.2.3-Alpha.1+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Alpha.0", "fix: bug"))
        assertEquals("2.2.3-Beta.4+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Beta.3", "fix: bug"))
        assertEquals("3.0.0-Beta.1+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "3.0.0-Beta.0", "refactor!: drop some support"))
        assertEquals("3.0.0-Beta.0+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Beta.0", "refactor!: drop some support"))
        assertEquals("3.0.0-Beta.1+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "3.0.0-Beta.0", "feat: new api\r\n\r\nA message\r\n\r\nBREAKING CHANGE: drop support"))
        assertEquals("3.0.0-Beta.0+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Beta.0", "feat: new api\r\n\r\nA message\r\n\r\nBREAKING CHANGE: drop support"))
        assertEquals("2.3.0-NEXT.2+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.3.0-NEXT.1", "feat: new"))
        assertEquals("2.3.0-NEXT.0+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-NEXT.1", "feat: new"))
        assertEquals("2.3.0-SNAPSHOT+002", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "1.0.0-IGNORED", "feat: new"))
        assertEquals("2.3.0-NEXT+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-NEXT", "feat: new"))
    }

    private fun getVersionFromTagAndPreAndCommitDirty(tagName: String, preRelease: String, commit: String): String {
        // act
        val version = assertNotNull(SemVersion.tryParse(Tag(tagName, SHA)))
        version.updateFromCommit(Commit("Tagged commit", SHA, emptySequence()), settings, SemVersion.tryParse(Tag(preRelease, SHA)))
        version.updateFromCommit(Commit(commit, SHA, emptySequence()), settings)
        version.calculateNewVersion(true, settings.defaultPreRelease)
        return version.toInfoVersionString()
    }

    @Test
    fun testIncrementVersion_dirty() {
        assertEquals("1.1.1-SNAPSHOT", getVersionFromTagAndDirty("v1.1.0"))
        assertEquals("2.2.3-SNAPSHOT", getVersionFromTagAndDirty("v2.2.2"))
    }

    private fun getVersionFromTagAndDirty(tagName: String): String {
        val actualVersion = assertNotNull(SemVersion.tryParse(Tag(tagName, SHA)))
        actualVersion.calculateNewVersion(true, settings.defaultPreRelease)
        return actualVersion.toInfoVersionString()
    }

    @Test
    fun testInfoVersionSha() {
        val actualVersion = SemVersion.tryParse(Tag("1.0.0", SHA))
        assertEquals("1.0.0+sha.8727a3e", actualVersion.toString())
    }
}