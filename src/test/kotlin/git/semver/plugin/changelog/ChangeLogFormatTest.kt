package git.semver.plugin.changelog

import git.semver.plugin.scm.Commit
import git.semver.plugin.semver.SemverSettings
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class ChangeLogFormatTest {
    @Test
    fun format_log_empty() {
        val settings = SemverSettings()

        val actual = ChangeLogFormat.defaultChangeLog.formatLog(listOf(), settings, ChangeLogTexts())

        assertThat(actual).startsWith("## What's Changed")
    }

    @Test
    fun format_log_with_breaking_changes_default() {
        val changeLog = createChangeLog()
        val settings = SemverSettings()

        val actual = ChangeLogFormat.defaultChangeLog.formatLog(changeLog, settings, ChangeLogTexts())

        assertThat(actual)
            .startsWith("## What's Changed")
            .containsOnlyOnce("Bugfix 1")
            .contains("### Breaking Changes")
            .contains("- 0050000 fix(changelog)!: A breaking change")
            .contains("### Bug Fixes")
            .contains("- 0060000 build: A build change")
            .contains("- 0090000 A CI change")
            .contains("### Tests")
            .contains("- 0080000 Added some tests")
            .contains("### New Features")
            .contains("- 0040000 semver: A feature")
            .contains("- 0100000 xyz: Some other change")
            .contains("- 0110000 An uncategorized change")
            .doesNotContain("1.2.3")
            .doesNotContain("more text")
        println(actual)
    }

    @Test
    fun format_log_with_breaking_changes_scope() {
        val changeLog = createChangeLog()
        val settings = SemverSettings()

        val actual = ChangeLogFormat.scopeChangeLog.formatLog(changeLog, settings, ChangeLogTexts())

        assertThat(actual)
            .startsWith("## What's Changed")
            .containsOnlyOnce("Bugfix 1")
            .contains("### Breaking Changes")
            .contains("- 0050000 fix: A breaking change")
            .contains("### Bug Fixes")
            .contains("- 0060000 A build change")
            .contains("- 0090000 A CI change")
            .contains("### Tests")
            .contains("- 0080000 Added some tests")
            .contains("### New Features")
            .contains("- 0040000 A feature")
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

        val actual = ChangeLogFormat.simpleChangeLog.formatLog(changeLog, settings, ChangeLogTexts())

        assertThat(actual)
            .containsOnlyOnce("Bugfix 1")
            .contains("## Breaking Changes")
            .contains("- fix(changelog)!: A breaking change")
            .contains("## Bug Fixes")
            .doesNotContain("- build(deps): A build change")
            .doesNotContain("- A CI change")
            .contains("## New Features")
            .contains("- semver: A feature")
            .doesNotContain("- xyz: Some other change")
            .doesNotContain("- An uncategorized change")
            .doesNotContain("1.2.3")
            .doesNotContain("more text")
        println(actual)
    }

    @Test
    fun format_log_with_empty_settings() {
        val settings = SemverSettings()
        val changeLog = createChangeLog()

        val actual = ChangeLogFormatter {}.formatLog (changeLog, settings, ChangeLogTexts())

        assertThat(actual).isEmpty()
    }

    private fun createChangeLog(): List<Commit> {
        val changeLog = mapOf(
            "0010000" to "fix(changelog): Bugfix 1 #1",
            "0020000" to "fix(changelog): Bugfix 1 #1",
            "0030000" to "fix(deps): Bugfix broken deps",
            "0040000" to "feat(semver): A feature",
            "0050000" to "fix(changelog)!: A breaking change\n\nBody text",
            "0060000" to "build(deps): A build change",
            "0070000" to "release: 1.2.3-alpha",
            "0080000" to "test: Added some tests",
            "0090000" to "ci: A CI change",
            "0100000" to "xyz: Some other change",
            "0110000" to "An uncategorized change"
        )
        return changeLog.map { Commit(it.value, it.key, emptySequence()) }
    }
}