package git.semver.plugin.changelog

import git.semver.plugin.changelog.DefaultHeaderTexts.BREAKING_CHANGES
import git.semver.plugin.changelog.DefaultHeaderTexts.BUG_FIXES
import git.semver.plugin.changelog.DefaultHeaderTexts.BUILD
import git.semver.plugin.changelog.DefaultHeaderTexts.CHORES
import git.semver.plugin.changelog.DefaultHeaderTexts.CI
import git.semver.plugin.changelog.DefaultHeaderTexts.DEPENDENCY_UPDATES
import git.semver.plugin.changelog.DefaultHeaderTexts.DOCUMENTATION
import git.semver.plugin.changelog.DefaultHeaderTexts.NEW_FEATURE
import git.semver.plugin.changelog.DefaultHeaderTexts.OTHER_CHANGES
import git.semver.plugin.changelog.DefaultHeaderTexts.PERFORMANCE_ENHANCEMENTS
import git.semver.plugin.changelog.DefaultHeaderTexts.TESTS
import git.semver.plugin.changelog.DefaultHeaderTexts.TOP_HEADER
import git.semver.plugin.changelog.DefaultHeaderTexts.REFACTOR


internal object DefaultHeaderTexts {
    const val TOP_HEADER = "## What's Changed"
    const val BREAKING_CHANGES = "### Breaking Changes 🛠"
    const val OTHER_CHANGES = "### Other Changes \uD83D\uDCA1"
    const val BUG_FIXES = "### Bug Fixes \uD83D\uDC1E"
    const val NEW_FEATURE = "### New Features \uD83C\uDF89"
    const val TESTS = "### Tests ✅"
    const val DOCUMENTATION = "### Documentation \uD83D\uDCD6"
    const val DEPENDENCY_UPDATES = "### Dependency Updates \uD83D\uDE80"
    const val BUILD = "### Build \uD83D\uDC18 & CI ⚙\uFE0F"
    const val CI = BUILD
    const val CHORES = "### Chores \uD83D\uDD27"
    const val PERFORMANCE_ENHANCEMENTS = "### Performance Enhancements ⚡"
    const val REFACTOR = "### Refactorings \uD83D\uDE9C"
}


object DefaultChangeLogTexts : ChangeLogTexts(mutableMapOf(
    HEADER to TOP_HEADER,
    BREAKING_CHANGE to BREAKING_CHANGES,
    OTHER_CHANGE to OTHER_CHANGES,
    "fix" to BUG_FIXES,
    "feat" to NEW_FEATURE,
    "test" to TESTS,
    "docs" to DOCUMENTATION,
    "deps" to DEPENDENCY_UPDATES,
    "build" to BUILD,
    "ci" to CI,
    "chore" to CHORES,
    "perf" to PERFORMANCE_ENHANCEMENTS,
    "refactor" to REFACTOR
))