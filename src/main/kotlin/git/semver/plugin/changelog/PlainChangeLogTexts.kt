package git.semver.plugin.changelog


internal object PlainHeaderTexts {
    const val TOP_HEADER = "## What's Changed"
    const val BREAKING_CHANGES = "### Breaking Changes"
    const val OTHER_CHANGES = "### Other Changes"
    const val BUG_FIXES = "### Bug Fixes"
    const val NEW_FEATURE = "### New Features"
    const val TESTS = "### Tests"
    const val DOCUMENTATION = "### Documentation"
    const val DEPENDENCY_UPDATES = "### Dependency Updates"
    const val BUILD = "### Build & CI"
    const val CI = BUILD
    const val CHORES = "### Chores"
    const val PERFORMANCE_ENHANCEMENTS = "### Performance Enhancements"
    const val REFACTORINGS = "### Refactorings"
}
object PlainChangeLogTexts : ChangeLogTexts(mutableMapOf(
    HEADER to PlainHeaderTexts.TOP_HEADER,
    BREAKING_CHANGE to PlainHeaderTexts.BREAKING_CHANGES,
    OTHER_CHANGE to PlainHeaderTexts.OTHER_CHANGES,
    "fix" to PlainHeaderTexts.BUG_FIXES,
    "feat" to PlainHeaderTexts.NEW_FEATURE,
    "test" to PlainHeaderTexts.TESTS,
    "docs" to PlainHeaderTexts.DOCUMENTATION,
    "deps" to PlainHeaderTexts.DEPENDENCY_UPDATES,
    "build" to PlainHeaderTexts.BUILD,
    "ci" to PlainHeaderTexts.CI,
    "chore" to PlainHeaderTexts.CHORES,
    "perf" to PlainHeaderTexts.PERFORMANCE_ENHANCEMENTS,
    "refactor" to PlainHeaderTexts.REFACTORINGS
))