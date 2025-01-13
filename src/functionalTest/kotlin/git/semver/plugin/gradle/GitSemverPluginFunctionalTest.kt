package git.semver.plugin.gradle

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import java.io.File

/**
 * A simple functional test for the plugin.
 */
class GitSemverPluginFunctionalTest {

    companion object {
        @JvmStatic
        fun gradleVersions(): List<Arguments> {
            return listOf(
//                Arguments.of("8.6"),
                Arguments.of("8.7"),
//                Arguments.of("7.6.4"),
//                Arguments.of("6.9.4"),
//                Arguments.of("5.6.4")
            )
        }
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    @NullSource
    fun `can run release task`(version: String?) {
        val projectDir = setupTestProject()

        val releaseResult = run(
            projectDir, version, "subProjectVersion", "release",
            "--tag", "--commit",
            "-PdefaultPreRelease=NEXT",
            "-PnoDirtyCheck=true",
            "--stacktrace"
        )

        assertThat(releaseResult.output)
            .doesNotContain("FAILED")
            .containsPattern("SubProjectVersion: \\d+\\.\\d+\\.\\d+-NEXT")
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    @NullSource
    fun `can run release task with --no-tag and --no-commit`(version: String?) {
        val projectDir = setupTestProject()

        val releaseResult2 = run(
            projectDir, version, "subProjectVersion", "release",
            "--no-tag", "--no-commit",
            "--preRelease=NEXT.1",
            "--message=test"
        )

        assertThat(releaseResult2.output)
            .doesNotContain("FAILED")
            .contains("SNAPSHOT")
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    @NullSource
    fun `can run testTask task`(version: String?) {
        val projectDir = setupTestProject()

        val result = run(projectDir, version, "testTask", "--configuration-cache", "--stacktrace")

        assertThat(result.output).containsPattern("\\d+\\.\\d+\\.\\d+")
        assertThat(result.output).containsPattern("ProjVer: \\d+\\.\\d+\\.\\d+")
    }

    @ParameterizedTest
    @CsvSource(value = [
        "printVersion, \\d+\\.\\d+\\.\\d+",
        "printInfoVersion, \\d+\\.\\d+\\.\\d+-SNAPSHOT\\+\\d{3}",
        "printSemVersion, \\d+\\.\\d+\\.\\d+-SNAPSHOT\\+\\d{3}\\.sha"])
    fun `can run printVersion task`(printTask: String, pattern: String) {
        val projectDir = setupTestProject()

        val result = run(projectDir, null, printTask, "-q")

        assertThat(result.output).containsPattern(pattern)
    }

    @Test
    fun `can run printChangeLog task`() {
        val projectDir = setupTestProject()

        val result = run(projectDir, null, "printChangeLog", "-q")

        assertThat(result.output).containsPattern("added test files")
    }

    @Test
    fun `can run printVersion to file task`() {
        val projectDir = setupTestProject()

        val file = "testPrintVersion.txt"
        val result = run(projectDir, null, "-i", "printVersion", "--file", file)

        assertThat(result.output).containsPattern(file)
        val f = File(file);
        if (f.exists()) {
            assertThat(f.delete()).isTrue();
        }
    }

    private fun setupTestProject(): File {
        val semverSettings = """
            semver {
              groupVersionIncrements = false
              createReleaseTag = true
              createReleaseCommit = true
            }
        """.trimIndent();

        // Setup the test build
        val projectDir = File("build/functionalTest")
        projectDir.mkdirs()
        projectDir.resolve(".gitignore").writeText(".gradle")
        projectDir.resolve("settings.gradle").writeText("include ':sub1'")
        projectDir.resolve("build.gradle.kts").writeText(
            """
                plugins {
                  id("com.github.jmongard.git-semver-plugin")
                }
                
                ${semverSettings}
                
                semver {
                  changeLogTexts {
                    header = "# Test changelog"
                    hashFormat =  { commitInfo ->
                        commitInfo.commits.joinToString(" <<<", "", ">>> ") { it.sha }
                    }
                  }
//                  changeLogFormat = git.semver.plugin.changelog.ChangeLogFormat.defaultChangeLog
//                  changeLogFormat = git.semver.plugin.changelog.ChangeLogFormat.simpleChangeLog
//                  changeLogFormat = git.semver.plugin.changelog.ChangeLogFormat.scopeChangeLog
                  changeLogFormat {
                    appendLine(constants.header).appendLine()
                    withType("test") {
                    appendLine("## Test")
                    formatChanges {
                        appendLine("- ${'$'}{scope()}${'$'}{header()} ${'$'}{hash()}")
                    }
                    appendLine()
                    }
                  }
                }
                
                version = semver.version
                
                println("InfoVer: ${'$'}{semver.semVersion.toInfoVersionString()}")
                
                tasks.register("testTask") {
                    dependsOn("printVersion")
                    val ver = project.version
                
                    doLast {
                        println("ProjVer: ${'$'}{ver}")
                    }
                }
            """.trimIndent()
        )

        setupSubProject(projectDir, semverSettings)
        setupGitRepo(projectDir)

        return projectDir
    }

    private fun setupSubProject(projectDir: File, semverSettings: String) {
        val subProjectDir = File(projectDir, "sub1")
        subProjectDir.mkdirs()
        subProjectDir.resolve("build.gradle").writeText(
            """
                plugins {
                  id("com.github.jmongard.git-semver-plugin")
                }
                $semverSettings
                
                version = semver.version
                
                task subProjectVersion {
                  doLast {
                     println "SubProjectVersion: " + project.version 
                  }
                }          
            """.trimIndent()
        )
    }

    private fun setupGitRepo(projectDir: File) {

        // Add the build files to a new git repository
        Git.init().setDirectory(projectDir).call().use {
            it.add().addFilepattern(".").call()
            it.commit().setMessage("test: added test files").call()
        }
    }

    private fun run(projectDir: File, gradleVersion: String?, vararg args: String): BuildResult {
        val builder = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(args.toList())
            .withProjectDir(projectDir)
        gradleVersion?.let(builder::withGradleVersion)
        return builder.build()
    }
}
