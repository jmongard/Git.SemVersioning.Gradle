/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package git.semver.plugin.gradle

import git.semver.plugin.semver.SemverSettings
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class ChangeLogFormatterTest {
    @Test fun format_log_empty() {

        val settings = SemverSettings()

        val actual = ChangeLogFormatter(settings).formatLog(listOf())

        assertThat(actual).startsWith("# What's Changed")
    }

    @Test fun format_log_with_breaking_changes() {

        val settings = SemverSettings()
        val changeLog = listOf(
            "fix(#1): Bugfix 1",
            "fix(#1): Bugfix 1",
            "fix(deps): Bugfix broken deps",
            "feat(#2): A feature",
            "fix(#5)!: A breaking change",
            "build(deps): A build change",
            "release: 1.2.3-alpha",
            "test: Added some tests",
            "ci: A CI change"
        )

        val actual = ChangeLogFormatter(settings).formatLog(changeLog)
        println(actual)
        assertThat(actual)
            .startsWith("# What's Changed")
            .containsOnlyOnce("Bugfix 1")
            .contains("## Breaking Changes")
            .contains("A breaking change")
            .contains("## Bug Fixes")
            .contains("- build(deps): A build change")
            .contains("- A CI change")
            .contains("## Tests")
            .contains("- Added some tests")
            .contains("## New Features")
            .contains("- #2: A feature")
            .doesNotContain("1.2.3")
    }
}
