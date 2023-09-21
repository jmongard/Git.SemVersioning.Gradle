package git.semver.plugin.semver

open class SemverSettings {
    companion object {
        val REGEX_OPTIONS = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    }

    var defaultPreRelease = "SNAPSHOT"
    var releasePattern = "\\Arelease(?:\\([^()]+\\))?:"
    var patchPattern = "\\Afix(?:\\([^()]+\\))?:"
    var minorPattern = "\\Afeat(?:\\([^()]+\\))?:"
    var majorPattern = "\\A\\w+(?:\\([^()]+\\))?!:|^BREAKING[ -]CHANGE:"

    var releaseCommitTextFormat = "release: v%s\n\n%s"
    var releaseTagNameFormat = "%s"
    var groupVersionIncrements = true
    var noDirtyCheck = false
    var noAutoBump = false

    internal val releaseRegex: Regex by lazy { releasePattern.toRegex(REGEX_OPTIONS) }
    internal val patchRegex: Regex by lazy { patchPattern.toRegex(REGEX_OPTIONS) }
    internal val minorRegex: Regex by lazy { minorPattern.toRegex(REGEX_OPTIONS) }
    internal val majorRegex: Regex by lazy { majorPattern.toRegex(REGEX_OPTIONS) }
}
