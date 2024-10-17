package git.semver.plugin.scm

import git.semver.plugin.semver.SemInfoVersion
import git.semver.plugin.semver.SemverSettings
import org.assertj.core.api.Assertions.*
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import kotlin.test.*

class GitProviderTest {
    private val tableStringFormat = "| %-45s | %-25s | %-20s |"
    private val tableSeparator = "| ${"-".repeat(45)} | ${"-".repeat(25)} | ${"-".repeat(20)} |"
    private val builder = StringBuilder()

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        builder.appendLine().appendLine(testInfo.displayName)
    }

    private fun printHead() {
        builder.appendLine(tableSeparator).appendLine(
            tableStringFormat.format(
                "Command",
                "Commit Text",
                "Calculated version"
            )
        ).appendLine(tableSeparator)
    }

    private fun printFoot() {
        println(builder)
    }

    @Test
    fun testGetSemVersion() {
        val actual = gitProvider().getSemVersion(File("."))

        assertNotNull(actual)
    }

    @Test
    fun testCommits_group() {
        val gitDir = File("build/integrationTest21")
        gitDir.mkdirs()

        printHead()

        val gitProvider = GitProvider(SemverSettings().apply { groupVersionIncrements = true })
        Git.init().setDirectory(gitDir).call().use {
            lotsOfCommits(it, gitProvider)

            val actual = release(gitProvider, it, "-")

            assertEquals("2.0.0", actual.toVersionString())
        }

        printFoot()
    }

    @Test
    fun testCommits_no_grouping() {
        val gitDir = File("build/integrationTest22")
        gitDir.mkdirs()

        printHead()

        val gitProvider = GitProvider(SemverSettings().apply { groupVersionIncrements = false })
        Git.init().setDirectory(gitDir).call().use {
            lotsOfCommits(it, gitProvider)

            val actual = release(gitProvider, it, "-")

            assertEquals("2.0.0", actual.toVersionString())
        }
        printFoot()
    }

    @Test
    fun testGetChangeLog() {
        val actual = gitProvider().getChangeLog(File("."))

        assertNotNull(actual)
    }

    @Test
    fun changeLog_before_release() {
        val gitDir = File("build/integrationTest31")
        gitDir.mkdirs()

        val gitProvider = GitProvider(SemverSettings())
        Git.init().setDirectory(gitDir).call().use {
            lotsOfCommits(it, gitProvider)

            val actual = gitProvider().changeLog(it)

            assertThat(actual.map(Commit::toString))
                .contains("feat: another feature")
                .contains("feat!: breaking feature")
                .doesNotContain("docs: updated readme")
        }
    }

    @Test
    fun changeLog_after_release() {
        val gitDir = File("build/integrationTest32")
        gitDir.mkdirs()

        val gitProvider = GitProvider(SemverSettings())
        Git.init().setDirectory(gitDir).call().use {
            lotsOfCommits(it, gitProvider)
            release(gitProvider, it, "-")

            val actual = gitProvider().changeLog(it)

            assertThat(actual.map(Commit::toString))
                .contains("feat: another feature")
                .contains("feat!: breaking feature")
                .contains("release: v2.0.0")
                .doesNotContain("docs: updated readme")
        }
    }

    @Test
    fun changeLog_stops_at_prerelease() {
        val gitDir = File("build/integrationTest33")
        gitDir.mkdirs()

        val gitProvider = GitProvider(SemverSettings())
        Git.init().setDirectory(gitDir).call().use {
            initOrReset(it, gitProvider)
            commit(it, "fix: first change", gitProvider)
            release(gitProvider, it, "alpha.1")
            commit(it, "build: some changes", gitProvider)
            commit(it, "feat: another feature", gitProvider)
            commit(it, "fix: another change", gitProvider)
            release(gitProvider, it, "-")

            val actual = gitProvider().changeLog(it)

            assertThat(actual.map(Commit::toString))
                .contains("feat: another feature")
                .contains("fix: another change")
                .contains("release: v0.1.0")
                .doesNotContain("fix: first change")
        }
    }

    private fun lotsOfCommits(it: Git, gitProvider: GitProvider) {
        initOrReset(it, gitProvider)
        commit(it, "build: some changes", gitProvider)
        release(gitProvider, it)
        commit(it, "docs: updated readme", gitProvider)
        release(gitProvider, it)
        commit(it, "fix: a fix", gitProvider)
        commit(it, "fix: another fix", gitProvider)
        release(gitProvider, it)
        commit(it, "feat: a feature", gitProvider)
        commit(it, "feat: another feature", gitProvider)
        commit(it, "feat!: breaking feature", gitProvider)
        commit(it, "docs: updated readme", gitProvider)
        commit(it, "feat: changes", gitProvider)
        commit(it, "feat: changes", gitProvider)
        commit(it, "fix: a fix", gitProvider)
        release(gitProvider, it)
        commit(it, "build: some changes", gitProvider)
        release(gitProvider, it, "alpha.1")
        commit(it, "build: some changes", gitProvider)
        release(gitProvider, it)
        commit(it, "fix: a fix", gitProvider)
        commit(it, "fix: another fix", gitProvider)
        commit(it, "feat: a feature", gitProvider)
        release(gitProvider, it)
        commit(it, "feat: another feature", gitProvider)
        commit(it, "feat!: breaking feature", gitProvider)
    }

    @Test
    fun testNoAutoBumpAndNoGroupingCommits_modified() {
        val gitDir = File("build/integrationTest7")
        gitDir.mkdirs()

        printHead()

        val gitProvider = GitProvider(SemverSettings().apply { groupVersionIncrements = false; noAutoBump = true })
        Git.init().setDirectory(gitDir).call().use {
            initOrReset(it, gitProvider)
            commit(it, "release: 0.0.10", gitProvider)
            commit(it, "fix: test12", gitProvider)
            val actual = commit(it, "test13", gitProvider)


            assertEquals("0.0.11-SNAPSHOT", actual.toVersionString())
        }
        printFoot()
    }

    @Test
    fun testNoAutoBumpAndNoGroupingCommits_not_modified() {
        val gitDir = File("build/integrationTest8")
        gitDir.mkdirs()

        printHead()

        val gitProvider = GitProvider(SemverSettings().apply { groupVersionIncrements = false; noAutoBump = true })
        Git.init().setDirectory(gitDir).call().use {
            initOrReset(it, gitProvider)
            commit(it, "release: 0.0.10", gitProvider)
            val actual = commit(it, "test13", gitProvider)


            assertEquals("0.0.10", actual.toVersionString())
        }
        printFoot()
    }

    @Test
    fun test_semver_snapshot_comparison_no_group() {
        val gitDir = File("build/integrationTest9")
        gitDir.mkdirs()

        printHead()

        val gitProvider = GitProvider(SemverSettings().apply { groupVersionIncrements = false; noAutoBump = true })
        Git.init().setDirectory(gitDir).call().use {
            initOrReset(it, gitProvider)
            release(gitProvider, it)
            commit(it, "fix: update semver plugin", gitProvider)
            release(gitProvider, it, "SNAPSHOT")
            commit(it, "fix: update semver plugin", gitProvider)
            val actual = release(gitProvider, it, "-")

            assertEquals("0.0.2", actual.toVersionString())
        }
        printFoot()
    }

    @Test
    fun test_semver_snapshot_comparison_group() {
        val gitDir = File("build/integrationTest10")
        gitDir.mkdirs()

        printHead()

        val gitProvider = GitProvider(SemverSettings().apply { groupVersionIncrements = true; noAutoBump = true })
        Git.init().setDirectory(gitDir).call().use {
            initOrReset(it, gitProvider)
            release(gitProvider, it)
            commit(it, "fix: update semver plugin", gitProvider)
            release(gitProvider, it, "SNAPSHOT")
            commit(it, "fix: update semver plugin", gitProvider)
            val actual = release(gitProvider, it, "-")

            assertEquals("0.0.2", actual.toVersionString())
        }
        printFoot()
    }

    private fun initOrReset(it: Git, gitProvider: GitProvider) {
        val msg = "Initial commit"
        it.commit().setMessage(msg).call()
        val last = it.log().all().call().last()
        it.reset().setRef(last.name).call()
        it.gc().call()
        getSemVersionAndPrint(gitProvider, it, "git commit -m \"$msg\"", msg)
    }

    private fun commit(it: Git, msg: String, gitProvider: GitProvider): SemInfoVersion {
        it.commit().setMessage(msg).call()
        return getSemVersionAndPrint(gitProvider, it, "git commit -m \"$msg\"", msg)
    }

    private fun release(gitProvider: GitProvider, it: Git, preRelease: String? = null): SemInfoVersion {
        gitProvider.createRelease(it, GitProvider.ReleaseParams(false, commit = true, preRelease = preRelease, noDirtyCheck = false))
        return getSemVersionAndPrint(
            gitProvider, it,
            "gradle releaseVersion " + if (preRelease == null) "" else "--preRelease=\"$preRelease\"",
            it.log().setMaxCount(1).call().first().fullMessage
        )
    }

    private fun getSemVersionAndPrint(
        gitProvider: GitProvider,
        it: Git,
        cmd: String,
        msg: String
    ): SemInfoVersion {
        val semVersion = gitProvider.semVersion(it)
        builder.appendLine(tableStringFormat.format(cmd, msg, semVersion.toInfoVersionString()))
        return semVersion
    }

    @Test
    fun testCreateReleaseCommit() {
        printHead()
        val gitDir = File("build/integrationTest")
        gitDir.mkdirs()

        val gitProvider = gitProvider()

        Git.init().setDirectory(gitDir).call().use {
            commit(it, "some changes", gitProvider)
        }
        gitProvider.createRelease(gitDir, GitProvider.ReleaseParams(true, commit = false, preRelease = "alpha.1", noDirtyCheck = false))

        Git.open(gitDir).use {
            commit(it, "feat: some feature", gitProvider)
            commit(it, "docs: some documentation", gitProvider)
        }
        gitProvider.createRelease(gitDir, GitProvider.ReleaseParams(false, commit = true, "beta.1", "some message", false))

        Git.open(gitDir).use {
            commit(it, "fix: some fixes", gitProvider)
            commit(it, "docs: some documentation", gitProvider)
        }
        gitProvider.createRelease(gitDir, GitProvider.ReleaseParams(true, commit = true, preRelease = null, noDirtyCheck = false))

        Git.open(gitDir).use {
            commit(it, "some changes", gitProvider)
            commit(it, "docs: some documentation", gitProvider)
        }
        gitProvider.createRelease(gitDir, GitProvider.ReleaseParams(false, commit = true, preRelease = "", noDirtyCheck = false))

        Git.open(gitDir).use {
            assertTrue(gitProvider.getHeadCommit(it.repository).text.startsWith("release: v0."))
        }

        printFoot()
    }

    @Test
    fun testCreateReleaseCommit3() {
        printHead()
        val gitDir = File("build/integrationTest3")
        gitDir.mkdirs()

        val gitProvider = gitProvider()

        Git.init().setDirectory(gitDir).call().use {
            initOrReset(it, gitProvider)
            release(gitProvider, it, "SNAPSHOT")
            commit(it, "docs: some documentation", gitProvider)
            commit(it, "fix: a fix", gitProvider)
            commit(it, "docs: some documentation", gitProvider)
            release(gitProvider, it, "-")
        }


        Git.open(gitDir).use {
            assertTrue(gitProvider.getHeadCommit(it.repository).text.startsWith("release: v0."))
        }
        printFoot()
    }

    @Test
    fun testEmptyRepo() {
        val gitDir = File("build/integrationTest4")
        gitDir.mkdirs()

        val gitProvider = gitProvider()

        Git.init().setDirectory(gitDir).call().use {
            assertEquals("0.0.0", gitProvider.getSemVersion(gitDir).toString())
        }
    }

    @Test
    fun testNoDirtyCheck() {
        val gitDir = File("build/integrationTest5")
        gitDir.mkdirs()

        val gitProvider = GitProvider(SemverSettings().apply { noDirtyCheck = true })

        Git.init().setDirectory(gitDir).call().use {
            File(gitDir, "tmp").createNewFile()
            assertThat(gitProvider.isClean(it)).isTrue()
        }
    }


    @Test
    fun testCreateReleaseCommit3_no_tag_or_commit() {
        printHead()
        val gitDir = File("build/integrationTest6")
        gitDir.mkdirs()

        val gitProvider = GitProvider(SemverSettings().apply {
            releaseCommitTextFormat = ""
            releaseTagNameFormat = ""
        })

        Git.init().setDirectory(gitDir).call().use {
            initOrReset(it, gitProvider)
            release(gitProvider, it, "SNAPSHOT")
            commit(it, "docs: some documentation", gitProvider)
            commit(it, "fix: a fix", gitProvider)
            commit(it, "docs: some documentation", gitProvider)
            release(gitProvider, it, "-")
        }

        Git.open(gitDir).use {
            assertFalse(gitProvider.getHeadCommit(it.repository).text.startsWith("release: v0."))
        }
        printFoot()
    }

    @ParameterizedTest
    @CsvSource(value = [
        "false, true",
        "true, false"
    ])
    fun checkDirty(noDirtyCheckParam: Boolean, isNotDirty: Boolean) {
        val provider = GitProvider(SemverSettings())

        assertThatCode { provider.checkDirty(noDirtyCheckParam, isNotDirty) }
            .doesNotThrowAnyException()
    }

    @Test
    fun checkDirty_throws() {
        val provider = GitProvider(SemverSettings())

        assertThatThrownBy { provider.checkDirty(noDirtyCheck = false, isClean = false) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @ParameterizedTest
    @CsvSource(value = [
        "%s, true, true",
        ", true, false",
        "%s, false, false"])
    fun shouldTag(format: String?, flag: Boolean, expected: Boolean) {
        assertThat(GitProvider.isFormatEnabled(flag, format.orEmpty())).isEqualTo(expected)
    }

    private fun gitProvider() = GitProvider(SemverSettings())
}