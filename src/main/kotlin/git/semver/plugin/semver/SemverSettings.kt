package git.semver.plugin.semver

import git.semver.plugin.gradle.ChangeLogFormatter

open class SemverSettings {

    var defaultPreRelease: String = "SNAPSHOT"
    var releasePattern: String = "\\Arelease(?:\\(\\w+\\))?:"
    var patchPattern: String = "\\Afix(?:\\([^()]+\\))?:"
    var minorPattern: String = "\\Afeat(?:\\([^()]+\\))?:"
    var majorPattern: String = "\\A\\w+(?:\\([^()]+\\))?!:|^BREAKING[ -]CHANGE:"
    var changeLogPattern: String = "\\A(?<Type>\\w+)(?:\\((?<Scope>[^()]+)\\))?:(?<Message>(?:.|\n)*)"
    var changeLogHeadings: Map<String, String> = mutableMapOf(
        "fix" to "## Bug Fixes \uD83D\uDC1E",
        "feat" to "## New Features \uD83C\uDF89",
        "test" to "## Tests ✅",
        "docs" to "## Docs \uD83D\uDCD6",
        "build" to "## Build \uD83D\uDC18 & CI ⚙\uFE0F",
        "ci" to "## Build \uD83D\uDC18 & CI ⚙\uFE0F",
        "chore" to "## Chores",
        "perf" to "## Performance Enhancements",
        "refactor" to "## Refactorings",
        "release" to "",
        ChangeLogFormatter.OTHER to "## Other Changes \uD83D\uDCA1",
        ChangeLogFormatter.BREAKING to "## Breaking Changes \uD83D\uDEE0",
        ChangeLogFormatter.HEADING to "# What's Changed"
    )

    var releaseCommitTextFormat = "release: v%s\n\n%s"
    var releaseTagNameFormat = "%s"
    var groupVersionIncrements = true
    var noDirtyCheck = false
    var noAutoBump = false;

    internal val releaseRegex: Regex by lazy { releasePattern.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)) }
    internal val patchRegex: Regex by lazy { patchPattern.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)) }
    internal val minorRegex: Regex by lazy { minorPattern.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)) }
    internal val majorRegex: Regex by lazy { majorPattern.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)) }
    internal val changeLogRegex: Regex by lazy { changeLogPattern.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)) }
}
