package git.semver.plugin.scm

import git.semver.plugin.semver.SemverSettings
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test

class GitProviderPathFilterTest {
    companion object {
        @TempDir
        lateinit var tempDir: Path
    }

    private fun getGitDir(name: String): File {
        val path = tempDir.resolve(name)
        Files.createDirectories(path)
        return path.toFile()
    }

    private fun commitFile(git: Git, relPath: String, message: String) {
        val file = File(git.repository.workTree, relPath)
        file.parentFile?.mkdirs()
        file.writeText(message)
        git.add().addFilepattern(relPath).call()
        git.commit().setMessage(message).call()
    }

    @Test
    fun `pathFilter empty behaves identically to no filter`() {
        val gitDir = getGitDir("pathFilterEmpty")
        val provider = GitProvider(SemverSettings().apply { pathFilter = "" })

        Git.init().setDirectory(gitDir).call().use { git ->
            commitFile(git, "README.md", "Initial commit")
            commitFile(git, "lib/A.kt", "release: 1.0.0")
            commitFile(git, "lib/B.kt", "feat: add B")

            assertThat(provider.semVersion(git).toVersionString()).isEqualTo("1.1.0-SNAPSHOT")
        }
    }

    @Test
    fun `commits outside pathFilter dont bump version`() {
        val gitDir = getGitDir("pathFilterOutside")
        val provider = GitProvider(SemverSettings().apply { pathFilter = "lib" })

        Git.init().setDirectory(gitDir).call().use { git ->
            commitFile(git, "README.md", "Initial commit")
            commitFile(git, "lib/A.kt", "release: 1.0.0")
            commitFile(git, "services/S.kt", "feat: add service")

            // feat in services/ only — should not bump version
            assertThat(provider.semVersion(git).toVersionString()).isEqualTo("1.0.0")
        }
    }

    @Test
    fun `commits inside pathFilter do bump version`() {
        val gitDir = getGitDir("pathFilterInside")
        val provider = GitProvider(SemverSettings().apply { pathFilter = "lib" })

        Git.init().setDirectory(gitDir).call().use { git ->
            commitFile(git, "README.md", "Initial commit")
            commitFile(git, "lib/A.kt", "release: 1.0.0")
            commitFile(git, "lib/B.kt", "feat: add feature in lib")

            assertThat(provider.semVersion(git).toVersionString()).isEqualTo("1.1.0-SNAPSHOT")
        }
    }

    @Test
    fun `release commits outside pathFilter are still found as base version`() {
        val gitDir = getGitDir("pathFilterReleaseOutside")
        val provider = GitProvider(SemverSettings().apply { pathFilter = "lib" })

        Git.init().setDirectory(gitDir).call().use { git ->
            // release commit touches README only — outside lib/ — but still acts as version base
            commitFile(git, "README.md", "release: 1.0.0")
            commitFile(git, "lib/A.kt", "feat: add feature in lib")

            assertThat(provider.semVersion(git).toVersionString()).isEqualTo("1.1.0-SNAPSHOT")
        }
    }

    @Test
    fun `mixed commits touching both pathFilter and other dirs are included`() {
        val gitDir = getGitDir("pathFilterMixed")
        val provider = GitProvider(SemverSettings().apply { pathFilter = "lib" })

        Git.init().setDirectory(gitDir).call().use { git ->
            commitFile(git, "README.md", "Initial commit")
            commitFile(git, "lib/A.kt", "release: 1.0.0")

            // Single commit that touches both lib/ and services/
            File(git.repository.workTree, "lib/C.kt").writeText("mixed")
            File(git.repository.workTree, "services").mkdirs()
            File(git.repository.workTree, "services/T.kt").writeText("mixed")
            git.add().addFilepattern("lib/C.kt").addFilepattern("services/T.kt").call()
            git.commit().setMessage("feat: mixed commit touching lib and services").call()

            assertThat(provider.semVersion(git).toVersionString()).isEqualTo("1.1.0-SNAPSHOT")
        }
    }

    @Test
    fun `releaseTagNameFormat prefix filters which tags are tracked`() {
        val gitDir = getGitDir("tagFormatFilter1")
        // lib subproject: only track lib-v* tags
        val provider = GitProvider(SemverSettings().apply {
            releaseTagNameFormat = "lib-v%s"
        })

        Git.init().setDirectory(gitDir).call().use { git ->
            commitFile(git, "README.md", "Initial commit")

            // lib subproject release (older)
            commitFile(git, "lib/A.kt", "lib commit")
            git.tag().setName("lib-v1.0.0").call()

            // services subproject release (newer — sits between HEAD and lib-v1.0.0)
            // Without tag filtering this would be picked up as the base version (2.0.0)
            commitFile(git, "services/S.kt", "services commit")
            git.tag().setName("services-v2.0.0").call()

            // new lib feature after both tags
            commitFile(git, "lib/B.kt", "feat: add B")

            val version = provider.semVersion(git)
            // Must be based on lib-v1.0.0 (1.x.x), NOT services-v2.0.0 (2.x.x)
            assertThat(version.toVersionString()).isEqualTo("1.1.0-SNAPSHOT")
        }
    }

    @Test
    fun `releaseTagNameFormat prefix empty tracks all tags (backward compat)`() {
        val gitDir = getGitDir("tagFormatFilter2")
        val provider = GitProvider(SemverSettings().apply {
            releaseTagNameFormat = "%s"  // default — no prefix filter
        })

        Git.init().setDirectory(gitDir).call().use { git ->
            commitFile(git, "README.md", "Initial commit")
            git.tag().setName("1.0.0").call()
            commitFile(git, "lib/B.kt", "feat: add B")

            assertThat(provider.semVersion(git).toVersionString()).isEqualTo("1.1.0-SNAPSHOT")
        }
    }

    @Test
    fun `ignored commits are excluded from changelog`() {
        val gitDir = getGitDir("pathFilterChangelog")
        val provider = GitProvider(SemverSettings().apply { pathFilter = "lib" })

        Git.init().setDirectory(gitDir).call().use { git ->
            commitFile(git, "README.md", "Initial commit")
            commitFile(git, "lib/A.kt", "release: 1.0.0")
            commitFile(git, "services/S.kt", "feat: add service outside lib")
            commitFile(git, "lib/B.kt", "feat: add B in lib")

            val messages = provider.changeLog(git).map { it.text }

            assertThat(messages).contains("feat: add B in lib")
            assertThat(messages).doesNotContain("feat: add service outside lib")
        }
    }
}
