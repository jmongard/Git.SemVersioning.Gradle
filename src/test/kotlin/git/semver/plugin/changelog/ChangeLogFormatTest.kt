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
            "0110000" to "An uncategorized change",
            "0200000" to """
                fix(gradle-plugin): Fix-up the caching mechanism

                The previous caching mechanism assumed that all tree nodes with a
                particular component identifier have identical subtrees. In some cases
                this does not seem to hold.

                In a some real world Android project, a dependency `A`, did not have
                chilren within the `releaseRuntimeClasspath` scope, while its `pom.xml`
                declared multiple dependencies limited to the `runtime` scope via [1].
                Running `./gradlew dependencies` revealed that the subtree of `A`
                actually has children in any `*RuntimeClassPath` scope, but has none in
                the `*CompileClasspath` scope. However, `GradleInspector` cached the
                subtree without children (as it first processed a Gradle configuration
                where the children are not resolved), and re-used that cached subtree
                for all scopes in which `A` appears. As result, the children of `A`
                were missing accidentally.

                Adjust the caching mechanism to only de-duplicate identical subtrees by
                using the same instance of `OrtComponentReference` for any identical
                subtree. While this new approach does not speed-up execution time, it
                does (compared to no caching) reduce main memory consumption, which was
                the primary goal of the introduction of the previous caching mechanism
                introduced by [2] and one of the goals of [3]. Compared to before, the
                memory consumption increases a bit, because the now removed incorrect
                "de-duplication" did reduce memory consumption. Together with the
                preceeding commit, which reduced main memory consumption, it should be
                ok overall.

                Note: Previously, `globalDependencySubtrees` and `ortComponentCache`
                      were used, which was redundant, because `ortComponentCache` also
                      caches subtrees, as the entries of type `OrtComponentReference`
                      are recursive data structures containing a subtree. This is why
                      the new approach uses only one `Map` for the caching.
                      Furthermore, `OrtComponentReferenceImpl` becomes a `data` class
                      now, because the `OrtComponentReference` interface is being used
                      as the key for a map.

                [1]: `<scope>runtime</scope>`
                [2]: https://github.com/oss-review-toolkit/ort/commit/9ccccf6b86b0f3aaf16add5067d8011b4511e33a
                [3]: https://github.com/oss-review-toolkit/ort/commit/7c63104aec5c4bfdfcb618558c4bddb7790df4ac
            """.trimIndent()
        )
        return changeLog.map { Commit(it.value, it.key, 0, emptySequence()) }
    }
}