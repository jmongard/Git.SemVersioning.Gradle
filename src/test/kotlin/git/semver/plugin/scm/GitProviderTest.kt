package git.semver.plugin.scm

import git.semver.plugin.semver.SemVersion
import git.semver.plugin.semver.SemverSettings
import org.eclipse.jgit.api.Git
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitProviderTest {
    @Test
    fun testGetSemVersion() {
        val actual = GitProvider(SemverSettings()).getSemVersion(File("."))

        assertNotNull(actual)
        println(actual)
    }

    @Test
    fun testCommits() {
        val gitDir = File("build/integrationTest2")
        gitDir.mkdirs()

        println("| %-30s | %-20s | %-38s|".format("Commit Text", "Calculated version", " Using release task: gradle ..."))
        println("| ------------------------------ | -------------------- | ------------------------------------- |")

        val gitProvider = GitProvider(SemverSettings())
        Git.init().setDirectory(gitDir).call().use {
            it.commit().setMessage("Initial commit").call()
            val last = it.log().all().call().last()
            it.reset().setRef(last.name).call()
            it.gc().call()
            printC("Initial commit", gitProvider, it)
            c(it, "some changes", gitProvider)
            r(gitProvider, it)
            c(it, "some changes", gitProvider)
            r(gitProvider, it)
            c(it, "fix: a fix", gitProvider)
            c(it, "fix: another fix", gitProvider)
            r(gitProvider, it)
            c(it, "feat: a feature", gitProvider)
            c(it, "feat: another feature", gitProvider)
            c(it, "feat!: breaking feature", gitProvider)
            c(it, "some changes", gitProvider)
            c(it, "feat: changes", gitProvider)
            r(gitProvider, it )
            c(it, "some changes", gitProvider)
            r(gitProvider, it, "alpha.1")
            c(it, "some changes", gitProvider)
            r(gitProvider, it )
            c(it, "fix: a fix", gitProvider)
            c(it, "fix: another fix", gitProvider)
            c(it, "feat: a feature", gitProvider)
            r(gitProvider, it )
            c(it, "feat: another feature", gitProvider)
            c(it, "feat!: breaking feature", gitProvider)

            val actual = r(gitProvider, it, "")

            assertEquals("2.0.0", actual.toVersionString())
        }
    }

    private fun c(it: Git, msg: String, gitProvider: GitProvider) {
        it.commit().setMessage(msg).call()
        printC(msg, gitProvider, it)
    }

    private fun printC(msg: String, gitProvider: GitProvider, it: Git) {
        println("| %-30s | %-20s | %-38s|".format(msg, gitProvider.semVersion(it).toInfoVersionString(), ""))
    }

    private fun r(gitProvider: GitProvider, it: Git, preRelease:String?=null) : SemVersion {
        gitProvider.createRelease(it, false, commit = true, preRelease = preRelease, noDirtyCheck = false)
        val semVersion = gitProvider.semVersion(it)
        println("| %-30s | %-20s | %-38s|".format(it.log().setMaxCount(1).call().first().fullMessage,

        semVersion.toInfoVersionString(), "releaseVersion " + if (preRelease == null) "" else "--preRelease=\"$preRelease\""))
        return semVersion
    }

    @Test
    fun testCreateReleaseCommit() {
        val gitDir = File("build/integrationTest")
        gitDir.mkdirs()

        val gitProvider = GitProvider(SemverSettings())

        Git.init().setDirectory(gitDir).call().use {
            c(it, "some changes", gitProvider)
        }
        gitProvider.createRelease(gitDir, true, commit = false, preRelease = "alpha.1", noDirtyCheck = false)

        Git.open(gitDir).use {
            c(it, "feat: some feature", gitProvider)
            c(it, "docs: some documentation", gitProvider)
        }
        gitProvider.createRelease(gitDir, false, true, "beta.1", "some message", false)

        Git.open(gitDir).use {
            c(it, "fix: some fixes", gitProvider)
            c(it, "docs: some documentation", gitProvider)
        }
        gitProvider.createRelease(gitDir, true, commit = true, preRelease = null, noDirtyCheck = false)

        Git.open(gitDir).use {
            c(it, "some changes", gitProvider)
            c(it, "docs: some documentation", gitProvider)
        }
        gitProvider.createRelease(gitDir, false, commit = true, preRelease = "", noDirtyCheck = false)

        Git.open(gitDir).use {
            assertTrue(gitProvider.getHeadCommit(it.repository).text.startsWith("release: v0."))
        }
    }
}