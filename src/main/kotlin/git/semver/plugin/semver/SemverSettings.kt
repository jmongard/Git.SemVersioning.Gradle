package git.semver.plugin.semver

import java.io.Serializable

open class SemverSettings(
    var defaultPreRelease: String = "SNAPSHOT",
    var releasePattern: String = "\\Arelease(?:\\([^()]+\\))?:",
    var patchPattern: String = "\\Afix(?:\\([^()]+\\))?:",
    var minorPattern: String = "\\Afeat(?:\\([^()]+\\))?:",
    var majorPattern: String = "\\A\\w+(?:\\([^()]+\\))?!:|^BREAKING[ -]CHANGE:",
    var releaseCommitTextFormat: String = "release: v%s\n\n%s",
    var releaseTagNameFormat: String = "%s",
    var groupVersionIncrements: Boolean = true,
    var noDirtyCheck: Boolean = false,
    var noAutoBump: Boolean = false
) : Serializable {
    companion object {
        val REGEX_OPTIONS = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    }

    constructor(
        s: SemverSettings
    ) : this(
        s.defaultPreRelease, s.releasePattern, s.patchPattern, s.minorPattern, s.majorPattern,
        s.releaseCommitTextFormat, s.releaseTagNameFormat, s.groupVersionIncrements, s.noDirtyCheck, s.noAutoBump
    )

    internal val releaseRegex: Regex by lazy { releasePattern.toRegex(REGEX_OPTIONS) }
    internal val patchRegex: Regex by lazy { patchPattern.toRegex(REGEX_OPTIONS) }
    internal val minorRegex: Regex by lazy { minorPattern.toRegex(REGEX_OPTIONS) }
    internal val majorRegex: Regex by lazy { majorPattern.toRegex(REGEX_OPTIONS) }
}
