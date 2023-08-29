package git.semver.plugin.gradle

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

/**
 * A simple functional test for the plugin.
 */
class GitSemverPluginFunctionalTest {

    companion object {
        @JvmStatic
        fun gradleVersions(): List<Arguments> {
            return listOf(
                Arguments.of("8.3"),
                Arguments.of("7.6.2"),
//                Arguments.of("6.9.4"),
//                Arguments.of("5.6.4")
            )
        }
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    fun `can run release task`(version: String) {
        val projectDir = setupTestProject()

        val releaseResult = run(
            projectDir, version, "release",
            "--tag", "--commit",
            "-PdefaultPreRelease=NEXT",
            "-PnoDirtyCheck=true",
            "--stacktrace"
        )

        assertThat(releaseResult.output)
            .doesNotContain("FAILED")
            .containsPattern("Sub1: \\d+\\.\\d+\\.\\d+-NEXT")
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    fun `can run release task with --no-tag and --no-commit`(version: String) {
        val projectDir = setupTestProject()

        val releaseResult2 = run(
            projectDir, version, "release",
            "--no-tag", "--no-commit",
            "--preRelease=NEXT.1",
            "--message=test"
        )

        assertThat(releaseResult2.output)
            .doesNotContain("FAILED")
            .doesNotContain("NEXT")
            .contains("SNAPSHOT")
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    fun `can run testTask task`(version: String) {
        val projectDir = setupTestProject()

        val result = run(projectDir, version, "testTask")

        assertThat(result.output).containsPattern("Version: \\d+\\.\\d+\\.\\d+")
        assertThat(result.output).containsPattern("ProjVer: \\d+\\.\\d+\\.\\d+")
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    fun `can run printVersion task`(version: String) {
        val projectDir = setupTestProject()

        val result = run(projectDir, version, "printVersion")

        assertThat(result.output).containsPattern("Version: \\d+\\.\\d+\\.\\d+")
    }

    private fun setupTestProject(): File {

        // Setup the test build
        val projectDir = File("build/functionalTest")
        projectDir.mkdirs()
        projectDir.resolve(".gitignore").writeText(".gradle")
        projectDir.resolve("settings.gradle").writeText("include ':sub1'")
        projectDir.resolve("build.gradle").writeText(
            """
                plugins {
                  id('com.github.jmongard.git-semver-plugin')
                }
                
                semver {
                  groupVersionIncrements = false
                  createReleaseTag = true
                  createReleaseCommit = true
                }
                
                def v = semver.version
                allprojects {
                  version = v
                }
                
                task testTask(dependsOn:printVersion) {
                  doLast {
                     println "ProjVer: " + project.version 
                  }
                }
            """.trimIndent()
        )

        setupSubProject(projectDir)
        setupGitRepo(projectDir)

        return projectDir
    }

    private fun setupSubProject(projectDir: File) {
        val subProjectDir = File(projectDir, "sub1")
        subProjectDir.mkdirs()
        subProjectDir.resolve("build.gradle").writeText(
            """
                println("Sub1: " + project.version)            
            """.trimIndent()
        )
    }

    private fun setupGitRepo(projectDir: File) {

        // Add the build files to a new git repository
        Git.init().setDirectory(projectDir).call().use {
            it.add().addFilepattern(".").call()
            it.commit().setMessage("test files").call()
        }
    }

    private fun run(projectDir: File, gradleVersion: String, vararg args: String): BuildResult {
        return GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(args.toList())
            .withProjectDir(projectDir)
            .withGradleVersion(gradleVersion)
            .build()
    }
}
