package git.semver.plugin.semver

import java.io.Serializable

abstract class BaseSettings(
    var defaultPreRelease: String = "SNAPSHOT",
    var releasePattern: String = "\\Arelease(?:\\([^()]+\\))?:",
    var patchPattern: String = "\\Afix!?(?:\\([^()]+\\))?:",
    var minorPattern: String = "\\Afeat!?(?:\\([^()]+\\))?:",
    var majorPattern: String = "\\A\\w+(?:\\([^()]+\\))?!:|^BREAKING[ -]CHANGE:",
    var releaseCommitTextFormat: String = "release: v%s\n\n%s",
    var releaseTagNameFormat: String = "%s",
    var groupVersionIncrements: Boolean = true,
    var noDirtyCheck: Boolean = false,
    var noAutoBump: Boolean = false,
    var gitSigning: Boolean? = null // null means use the jgit default
) : Serializable {
    constructor(settings: BaseSettings) : this(
        settings.defaultPreRelease, settings.releasePattern, settings.patchPattern, settings.minorPattern,
        settings.majorPattern, settings.releaseCommitTextFormat, settings.releaseTagNameFormat,
        settings.groupVersionIncrements, settings.noDirtyCheck, settings.noAutoBump
    )
}