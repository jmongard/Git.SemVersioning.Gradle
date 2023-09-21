package git.semver.plugin.changelog

data class ChangeLogSettings(
    var header: String? = null,
    var breakingChangeHeader: String? = null,
    var otherChangeHeader: String? = null,
    var missingTypeHeader: String? = null,
    var headerTexts: MutableMap<String, String> = mutableMapOf(),
    var changePrefix: String = "",
    var changePostfix: String = "",
    var changeLineSeparator: String? = null,
    var changeShaLength: Int = 40,
    var groupStart: String? = null,
    var groupEnd: String? = null,
    var footer: String? = null
) {
    var changeLogPattern = "\\A(?<Type>\\w+)(?:\\((?<Scope>[^()]+)\\))?!?:\\s*(?<Message>(?:.|\n)*)"

    companion object {
        val defaultChangeLog
            get() = ChangeLogSettings(
                "## What's Changed",
                "### Breaking Changes 🛠",
                "### Other Changes \uD83D\uDCA1",
                "### Other Changes \uD83D\uDCA1",
                headerTexts = mutableMapOf(
                    "fix" to "### Bug Fixes \uD83D\uDC1E",
                    "feat" to "### New Features \uD83C\uDF89",
                    "test" to "### Tests ✅",
                    "docs" to "### Docs \uD83D\uDCD6",
                    "deps" to "### Dependency updates \uD83D\uDE80",
                    "build" to "### Build \uD83D\uDC18 & CI ⚙\uFE0F",
                    "ci" to "### Build \uD83D\uDC18 & CI ⚙\uFE0F",
                    "chore" to "### Chores \uD83D\uDD27",
                    "perf" to "### Performance Enhancements ⚡",
                    "refactor" to "### Refactorings \uD83D\uDE9C",
                    "release" to ""
                ),
                "- ")
        val simpleChangeLog
            get() = ChangeLogSettings(
                breakingChangeHeader = "## Breaking Changes",
                headerTexts = mutableMapOf(
                    "fix" to "## Bug Fixes",
                    "feat" to "## New Features",
                    "release" to ""
                ),
                changePrefix = "- ",
                changeShaLength = 0)
    }
}