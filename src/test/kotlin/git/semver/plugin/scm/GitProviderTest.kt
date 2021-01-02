package git.semver.plugin.scm

import git.semver.plugin.semver.SemverSettings
import org.eclipse.jgit.api.Git
import java.io.File
import kotlin.test.Test
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
    fun testCreateReleaseCommit() {
        val gitDir = File("build/integrationTest")
        gitDir.mkdirs()


        val gitProvider = GitProvider(SemverSettings())

        Git.init().setDirectory(gitDir).call().use {
            it.commit().setMessage("some changes").call()
        }
        gitProvider.createRelease(gitDir, true, commit = false, preRelease = "alpha.1")

        Git.open(gitDir).use {
            it.commit().setMessage("feat: some feature").call()
            it.commit().setMessage("docs: some documentation").call()
        }
        gitProvider.createRelease(gitDir, false, true, "beta.1", "some message")

        Git.open(gitDir).use {
            it.commit().setMessage("fix: some fixes").call()
            it.commit().setMessage("docs: some documentation").call()
        }
        gitProvider.createRelease(gitDir, true, commit = true, preRelease = null)

        Git.open(gitDir).use {
            it.commit().setMessage("some changes").call()
            it.commit().setMessage("docs: some documentation").call()
        }
        gitProvider.createRelease(gitDir, false, commit = true, preRelease = "")

        Git.open(gitDir).use {
            assertTrue(gitProvider.getHeadCommit(it.repository).text.startsWith("release: v0."))
        }
    }
}