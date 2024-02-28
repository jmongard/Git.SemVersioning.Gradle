package git.semver.plugin.semver

class SemverSettings : BaseSettings {
    companion object {
        val REGEX_OPTIONS = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    }

    constructor(settings: BaseSettings) : super(settings)

    internal constructor() // Used by unit tests

    internal val releaseRegex: Regex by lazy { releasePattern.toRegex(REGEX_OPTIONS) }
    internal val patchRegex: Regex by lazy { patchPattern.toRegex(REGEX_OPTIONS) }
    internal val minorRegex: Regex by lazy { minorPattern.toRegex(REGEX_OPTIONS) }
    internal val majorRegex: Regex by lazy { majorPattern.toRegex(REGEX_OPTIONS) }
}
