package git.semver.plugin.changelog

import git.semver.plugin.scm.Commit
import git.semver.plugin.semver.SemverSettings
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class ChangeLogFormatTest {
    @Test
    fun format_log_empty() {
        val settings = SemverSettings()

        val actual = ChangeLogFormat.defaultChangeLog.formatLog(listOf(), settings, DefaultChangeLogTexts)

        assertThat(actual).startsWith(DefaultHeaderTexts.TOP_HEADER)
    }

    @Test
    fun format_log_with_breaking_changes_default() {
        val changeLog = createChangeLog()
        val settings = SemverSettings()

        val actual = ChangeLogFormat.defaultChangeLog.formatLog(changeLog, settings, DefaultChangeLogTexts)

        assertThat(actual)
            .startsWith(DefaultHeaderTexts.TOP_HEADER)
            .containsOnlyOnce("Bugfix 1")
            .contains(DefaultHeaderTexts.BREAKING_CHANGES)
            .contains("- 0050000 fix(changelog)!: A breaking change")
            .contains(DefaultHeaderTexts.BUG_FIXES)
            .contains("- 0060000 build: A build change")
            .contains("- 0090000 A CI change")
            .contains(DefaultHeaderTexts.TESTS)
            .contains("- 0080000 Added some tests")
            .contains(DefaultHeaderTexts.NEW_FEATURE)
            .contains("- 0040000 semver: A feature")
            .contains("- 0100000 xyz: Some other change")
            .contains("- 0110000 An uncategorized change")
            .contains("- 0030000 deps: Bugfix broken deps")
            .doesNotContain("1.2.3")
            .doesNotContain("more text")
        println(actual)
    }

    @Test
    fun format_log_with_breaking_changes_scope() {
        val changeLog = createChangeLog()
        val settings = SemverSettings()

        val actual = ChangeLogFormat.scopeChangeLog.formatLog(changeLog, settings, DefaultChangeLogTexts)

        assertThat(actual)
            .startsWith(DefaultHeaderTexts.TOP_HEADER)
            .containsOnlyOnce("Bugfix 1")
            .contains(DefaultHeaderTexts.BREAKING_CHANGES)
            .contains("- 0050000 fix: A breaking change")
            .contains(DefaultHeaderTexts.BUG_FIXES)
            .contains("- 0060000 A build change")
            .contains("- 0090000 A CI change")
            .contains(DefaultHeaderTexts.TESTS)
            .contains("- 0080000 Added some tests")
            .contains(DefaultHeaderTexts.NEW_FEATURE)
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

        val actual = ChangeLogFormat.simpleChangeLog.formatLog(changeLog, settings, DefaultChangeLogTexts)

        assertThat(actual)
            .startsWith(DefaultHeaderTexts.TOP_HEADER)
            .containsOnlyOnce("Bugfix 1")
            .contains(DefaultHeaderTexts.BREAKING_CHANGES)
            .contains("- fix(changelog)!: A breaking change")
            .contains(DefaultHeaderTexts.BUG_FIXES)
            .doesNotContain("- build(deps): A build change")
            .doesNotContain("- A CI change")
            .contains(DefaultHeaderTexts.NEW_FEATURE)
            .contains("- semver: A feature")
            .doesNotContain("- xyz: Some other change")
            .doesNotContain("- An uncategorized change")
            .doesNotContain("1.2.3")
            .doesNotContain("more text")
        println(actual)
    }

    @Test
    fun format_default_changelog_with_plain_text() {
        val changeLog = createChangeLog()
        val settings = SemverSettings()

        val actual = ChangeLogFormat.defaultChangeLog.formatLog(changeLog, settings, PlainChangeLogTexts)

        assertThat(actual)
            .startsWith(PlainHeaderTexts.TOP_HEADER)
            .containsOnlyOnce("Bugfix 1")
            .contains(PlainHeaderTexts.BREAKING_CHANGES)
            .contains("- 0050000 fix(changelog)!: A breaking change")
            .contains(PlainHeaderTexts.BUG_FIXES)
            .contains("- 0060000 build: A build change")
            .contains("- 0090000 A CI change")
            .contains(PlainHeaderTexts.TESTS)
            .contains("- 0080000 Added some tests")
            .contains(PlainHeaderTexts.NEW_FEATURE)
            .contains("- 0040000 semver: A feature")
            .contains("- 0100000 xyz: Some other change")
            .contains("- 0110000 An uncategorized change")
            .doesNotContain("1.2.3")
            .doesNotContain("more text")
        println(actual)
    }

    @Test
    fun format_log_with_empty_settings() {
        val settings = SemverSettings()
        val changeLog = createChangeLog()

        val actual = ChangeLogFormatter {}.formatLog (changeLog, settings, DefaultChangeLogTexts)

        assertThat(actual).isEmpty()
    }

    @Test
    fun format_no_grouping_nor_sorting() {
        val settings = SemverSettings()
        val changeLog = listOf(
            Commit("fix: B", "1", 0, emptySequence()),
            Commit("fix: A", "2", 1, emptySequence()),
            Commit("ignore: B", "5", 2, emptySequence()),
            Commit("fix: B", "3", 3, emptySequence()),
            Commit("fix: A", "4", 4, emptySequence()),
        )
        val c = ChangeLogTexts(mutableMapOf(
            "fix" to "FIX",
            "ignore" to "",
            "#" to "CHANGELOG"))
        c.groupByText  = false
        c.sortByText = false


        val actual = ChangeLogFormat.defaultChangeLog.formatLog(changeLog, settings, c)

        assertThat(actual).startsWith(
            """
            CHANGELOG

            FIX
            - 1 B
            - 2 A
            - 3 B
            - 4 A
            
            """.trimIndent()
        )
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
        return changeLog.map { Commit(it.value, it.key, 0, emptySequence()) }
    }
}