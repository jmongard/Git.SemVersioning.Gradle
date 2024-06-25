/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package git.semver.plugin.gradle

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Path
import kotlin.test.Test

/**
 * A simple unit test for the 'git.semver.plugin.gradle.greeting' plugin.
 */
class GitSemverPluginTest {
    @TempDir
    lateinit var tempDir: Path

    @ParameterizedTest
    @ValueSource(strings = ["printVersion", "printSemVersion", "printInfoVersion", "printChangeLog"])
    fun `plugin register print tasks`(name: String) {
        val outFile = tempDir.resolve("print/out.txt")
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.github.jmongard.git-semver-plugin")
        val c = project.extensions.findByName("semver") as GitSemverPluginExtension
        c.defaultPreRelease = "XYZ"

        val task = project.tasks.findByName(name) as PrintTask

        assertThat(task).isNotNull()
        assertThatCode { task.print() }.doesNotThrowAnyException()
        task.setFile(outFile.toString())
        task.print()

        if (name.endsWith("Version"))
            assertThat(outFile).exists().isNotEmptyFile().content().doesNotContain("SNAPSHOT").contains(c.version)
        else
            assertThat(outFile).exists().isNotEmptyFile().content().startsWith("#")
    }

    @Test
    fun `plugin print relative file`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.github.jmongard.git-semver-plugin")
        val c = project.extensions.findByName("semver") as GitSemverPluginExtension
        c.defaultPreRelease = "XYZ"

        val task = project.tasks.findByName("printVersion") as PrintTask
        task.setFile("version.txt")
        task.print()

        val file = File("version.txt")
        assertThat(file).exists()
        file.delete()
    }

    @Test
    fun `plugin subproject don't register releaseVersion task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.github.jmongard.git-semver-plugin")
        val subproject = ProjectBuilder.builder().withParent(project).build()
        subproject.plugins.apply("com.github.jmongard.git-semver-plugin")

        assertThat(project.tasks.findByName("releaseVersion")).isNotNull()
        assertThat(subproject.tasks.findByName("releaseVersion")).isNull()
    }

    @Test
    fun `plugin register release tasks`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.github.jmongard.git-semver-plugin")

        val task = project.tasks.findByName("releaseVersion") as ReleaseTask

        assertThat(task).isNotNull()

        task.setPreRelease("alpha1")
        assertThat(task.getReleaseParams().preRelease).isEqualTo("alpha1")

        task.setMessage("A Message")
        assertThat(task.getReleaseParams().message).isEqualTo("A Message")

        task.setNoDirty(false)
        assertThat(task.getReleaseParams().noDirtyCheck).isFalse()
        task.setNoDirty(true)
        assertThat(task.getReleaseParams().noDirtyCheck).isTrue()

        task.setNoCommit(true)
        assertThat(task.getReleaseParams().commit).isFalse()
        task.setNoCommit(false)
        assertThat(task.getReleaseParams().commit).isTrue()

        task.setNoTag(true)
        assertThat(task.getReleaseParams().tag).isFalse()
        task.setNoTag(false)
        assertThat(task.getReleaseParams().tag).isTrue()

        task.setTag(true)
        assertThat(task.getReleaseParams().tag).isTrue()
        task.setTag(false)
        assertThat(task.getReleaseParams().tag).isFalse()

        task.setCommit(true)
        assertThat(task.getReleaseParams().commit).isTrue()
        task.setCommit(false)
        assertThat(task.getReleaseParams().commit).isFalse()

        assertThatCode {
            task.createRelease()
        }.doesNotThrowAnyException()
    }

    @Test
    fun `plugin version for a git directory`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.github.jmongard.git-semver-plugin")
        val c = project.extensions.findByName("semver") as GitSemverPluginExtension

//        c.gitDirectory.set(File("c:/dev/src/test1"))
        c.gitDirectory.set(project.layout.projectDirectory)

        val task = project.tasks.findByName("printVersion") as PrintTask

        assertThat(task).isNotNull()
        assertThatCode { task.print() }.doesNotThrowAnyException()
    }
}
