package git.semver.plugin.changelog

import git.semver.plugin.scm.Commit
import git.semver.plugin.semver.SemverSettings
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class ChangeLogFormatterTest {
    @Test
    fun format_log_empty() {
        val settings = SemverSettings()

        val actual = ChangeLogFormatter(settings, ChangeLogSettings.defaultChangeLog).formatLog(listOf())

        assertThat(actual).startsWith("## What's Changed")
    }

    @Test
    fun format_log_with_breaking_changes_default() {
        val changeLog = createChangeLog()
        val settings = SemverSettings()

        val actual = ChangeLogFormatter(
            settings,
            ChangeLogSettings.defaultChangeLog.copy(changeShaLength = 7)
        ).formatLog(changeLog)

        assertThat(actual)
            .startsWith("## What's Changed")
            .containsOnlyOnce("Bugfix 1")
            .contains("### Breaking Changes")
            .contains("- 0050000 fix(#5)!: A breaking change")
            .contains("### Bug Fixes")
            .contains("- 0060000 build(deps): A build change")
            .contains("- 0090000 A CI change")
            .contains("### Tests")
            .contains("- 0080000 Added some tests")
            .contains("### New Features")
            .contains("- 0040000 #2: A feature")
            .contains("- 0100000 xyz: Some other change")
            .contains("- 0110000 An uncategorized change")
            .doesNotContain("1.2.3")
            .doesNotContain("more text")
        println(actual)
    }

    @Test
    fun format_log_with_breaking_changes_simple() {
        val changeLog = createChangeLog()
        val settings = SemverSettings()

        val actual = ChangeLogFormatter(settings, ChangeLogSettings.simpleChangeLog).formatLog(changeLog)

        assertThat(actual)
            .containsOnlyOnce("Bugfix 1")
            .contains("## Breaking Changes")
            .contains("- fix(#5)!: A breaking change")
            .contains("## Bug Fixes")
            .doesNotContain("- build(deps): A build change")
            .doesNotContain("- A CI change")
            .contains("## New Features")
            .contains("- #2: A feature")
            .doesNotContain("- xyz: Some other change")
            .doesNotContain("- An uncategorized change")
            .doesNotContain("1.2.3")
            .doesNotContain("more text")
        println(actual)
    }

    @Test
    fun format_log_with_multiline_and_no_sha() {
        val changeLog = createChangeLog()
        val settings = SemverSettings()
        val changeLogSettings = ChangeLogSettings.defaultChangeLog.apply {
            changeShaLength = 0
            changeLineSeparator = "\n  "
        }

        val actual = ChangeLogFormatter(settings, changeLogSettings).formatLog(changeLog)

        assertThat(actual)
            .startsWith("## What's Changed")
            .containsOnlyOnce("Bugfix 1")
            .contains("### Breaking Changes")
            .contains("- fix(#5)!: A breaking change")
            .doesNotContain("1.2.3")
            .contains("  more text")
        println(actual)
    }

    @Test
    fun format_log_with_empty_settings() {
        val settings = SemverSettings()
        val changeLog = createChangeLog()

        val actual = ChangeLogFormatter(settings, ChangeLogSettings()).formatLog(changeLog)

        assertThat(actual).isEmpty()
    }

    private fun createChangeLog(): List<Commit> {
        val changeLog = mapOf(
            "0010000000" to "fix(#1): Bugfix 1",
            "0020000000" to "fix(#1): Bugfix 1",
            "0030000000" to "fix(deps): Bugfix broken deps",
            "0040000000" to "feat(#2): A feature",
            "0050000000" to "fix(#5)!: A breaking change\nmore text",
            "0060000000" to "build(deps): A build change",
            "0070000000" to "release: 1.2.3-alpha",
            "0080000000" to "test: Added some tests",
            "0090000000" to "ci: A CI change",
            "0100000000" to "xyz: Some other change",
            "0110000000" to "An uncategorized change"
        )
        return changeLog.map { Commit(it.value, it.key, emptySequence()) }
    }
}