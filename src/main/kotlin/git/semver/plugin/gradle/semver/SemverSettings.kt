package git.semver.plugin.gradle.semver

open class SemverSettings {

    var defaultPreRelease: String ="SNAPSHOT"
    var releasePattern: String ="\\Arelease(?:\\(\\w+\\))?:"
    var patchPattern: String ="\\Afix(?:\\(\\w+\\))?:"
    var minorPattern: String ="\\Afeat(?:\\(\\w+\\))?:"
    var majorPattern: String ="\\A\\w+(?:\\(\\w+\\))?!:|^BREAKING[ -]CHANGE:"

    val releaseRegex: Regex by lazy { releasePattern.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)) }
    val patchRegex: Regex by lazy { patchPattern.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)) }
    val minorRegex: Regex by lazy { minorPattern.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)) }
    val majorRegex: Regex by lazy { majorPattern.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)) }
}
