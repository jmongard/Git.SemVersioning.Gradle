package git.semver.plugin.changelog

class ChangeLogTexts {
    companion object {
        const val HEADER = "#"
        const val BREAKING_CHANGE = "!"
        const val OTHER_CHANGE = "?"
    }

    val headerTexts = mutableMapOf(
        HEADER to "## What's Changed",
        BREAKING_CHANGE to "### Breaking Changes 🛠",
        OTHER_CHANGE to "### Other Changes \uD83D\uDCA1",
        "fix" to "### Bug Fixes \uD83D\uDC1E",
        "feat" to "### New Features \uD83C\uDF89",
        "test" to "### Tests ✅",
        "docs" to "### Docs \uD83D\uDCD6",
        "deps" to "### Dependency updates \uD83D\uDE80",
        "build" to "### Build \uD83D\uDC18 & CI ⚙\uFE0F",
        "ci" to "### Build \uD83D\uDC18 & CI ⚙\uFE0F",
        "chore" to "### Chores \uD83D\uDD27",
        "perf" to "### Performance Enhancements ⚡",
        "refactor" to "### Refactorings \uD83D\uDE9C"
    )

    var header: String
        get() = headerTexts[HEADER].orEmpty()
        set(value) {
            headerTexts[HEADER] = value
        }

    var footer: String = ""

    var breakingChange
        get() = headerTexts[BREAKING_CHANGE].orEmpty()
        set(value) {
            headerTexts[BREAKING_CHANGE] = value
        }

    var otherChange
        get() = headerTexts[OTHER_CHANGE].orEmpty()
        set(value) {
            headerTexts[OTHER_CHANGE] = value
        }
}