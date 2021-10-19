package git.semver.plugin.semver

import git.semver.plugin.scm.IRefInfo
import org.slf4j.LoggerFactory

class SemVersion(
    var sha: String = "",
    var major: Int = 0,
    var minor: Int = 0,
    var patch: Int = 0,
    preRelease: String? = null,
    revision: Int? = null
) : Comparable<SemVersion> {

    companion object {
        private val logger = LoggerFactory.getLogger(SemVersion::class.java)
        private val semVersionPattern =
            ("""(?<Major>0|[1-9]\d*)\.(?<Minor>0|[1-9]\d*)(?:\.(?<Patch>0|[1-9]\d*)(?:\.(?<Revision>0|[1-9]\d*))?)?"""
                    + """(?:-(?<PreRelease>(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?"""
                    + """(?:\+(?<BuildMetadata>[0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?""").toRegex()

        fun tryParse(refInfo: IRefInfo): SemVersion? {
            val match = semVersionPattern.find(refInfo.text) ?: return null

            val version = SemVersion(
                refInfo.sha,
                match.groups["Major"]!!.value.toInt(),
                match.groups["Minor"]!!.value.toInt(),
                match.groups["Patch"]?.value?.toInt() ?: 0,
                match.groups["PreRelease"]?.value,
                match.groups["Revision"]?.value?.toInt()
            )

            logger.debug("Found version: {} in: '{}'", version, refInfo.text)
            return version
        }

        fun isRelease(commit: IRefInfo, settings: SemverSettings): Boolean {
            return settings.releaseRegex.containsMatchIn(commit.text)
        }
    }

    private val lastReleaseMajor: Int = major
    private val lastReleaseMinor: Int = minor
    var commitCount = 0
    var updated = false
    internal var preReleasePrefix: String = ""
    internal var preReleaseVersion: Int? = null

    private var bumpPatch = 0
    private var bumpMinor = 0
    private var bumpMajor = 0
    private var bumpPre = 0

    init {
        setPreRelease(preRelease, revision)
    }

    val isSnapshot
        get() = commitCount > 0

    val isPreRelease
        get() = preReleasePrefix.isNotEmpty() || (preReleaseVersion ?: -1) > 0

    fun setPreRelease(value: String?, defaultPreReleaseVersion: Int? = null) {
        bumpPre = 0

        if (value == null) {
            preReleaseVersion = null
            preReleasePrefix = ""
            return
        }

        val prefix = value.trimEnd { it.isDigit() }
        preReleasePrefix = prefix
        preReleaseVersion =
            if (prefix.length < value.length) value.substring(prefix.length).toInt() else defaultPreReleaseVersion
    }

    override fun compareTo(other: SemVersion): Int {
        var i = comparableMajor().compareTo(other.comparableMajor())
        if (i == 0) {
            i = comparableMinor().compareTo(other.comparableMinor())
        }
        if (i == 0) {
            i = comparablePatch().compareTo(other.comparablePatch())
        }
        if (i == 0) {
            i = -isPreReleaseOrUpdated().compareTo(other.isPreReleaseOrUpdated())
        }
        if (i == 0) {
            i = preReleasePrefix.compareTo(other.preReleasePrefix)
        }
        if (i == 0) {
            i = comparablePreReleaseVersion().compareTo(other.comparablePreReleaseVersion())
        }
        if (i == 0) {
            i = -isSnapshot.compareTo(other.isSnapshot)
        }
        return i
    }

    private fun comparableMajor() = major + bumpMajor

    private fun comparableMinor() =
        if (bumpMajor == 0) minor + bumpMinor else 0

    private fun comparablePatch() =
        if (bumpMajor + bumpMinor == 0) patch + bumpPatch else 0

    private fun comparablePreReleaseVersion() =
        if (bumpMajor + bumpMinor + bumpPatch == 0) (preReleaseVersion ?: 1) + bumpPre else 1

    private fun isPreReleaseOrUpdated() =
        isPreRelease || updated || bumpMajor + bumpMinor + bumpPatch > 0

    fun updateFromCommit(commit: IRefInfo, settings: SemverSettings, newPreRelease: SemVersion? = null) {
        sha = commit.sha
        commitCount += 1

        if (!settings.groupVersionIncrements) {
            applyPendingChanges(false)
        }

        when {
            newPreRelease != null -> {
                if (newPreRelease >= this) {
                    reset()
                    major = newPreRelease.major
                    minor = newPreRelease.minor
                    patch = newPreRelease.patch
                    preReleasePrefix = newPreRelease.preReleasePrefix
                    preReleaseVersion = newPreRelease.preReleaseVersion
                    commitCount = 0
                } else {
                    logger.warn(
                        "Ignored pre-release with lower version than the current version: {} < {} ",
                        newPreRelease, this
                    )
                }
            }
            isPreRelease -> {
                when {
                    major == lastReleaseMajor
                            && settings.majorRegex.containsMatchIn(commit.text) ->
                        bumpMajor = 1

                    major == lastReleaseMajor
                            && minor == lastReleaseMinor
                            && settings.minorRegex.containsMatchIn(commit.text) ->
                        bumpMinor = 1

                    preReleaseVersion != null ->
                        bumpPre = 1
                }
            }
            settings.majorRegex.containsMatchIn(commit.text) -> bumpMajor = 1
            settings.minorRegex.containsMatchIn(commit.text) -> bumpMinor = 1
            settings.patchRegex.containsMatchIn(commit.text) -> bumpPatch = 1
        }
    }

    fun applyPendingChanges(forceBumpIfNoChanges: Boolean): Boolean {
        when {
            bumpMajor > 0 -> {
                major += bumpMajor
                minor = 0
                patch = 0
                if (preReleaseVersion != null) {
                    preReleaseVersion = 1
                }
            }
            bumpMinor > 0 -> {
                minor += bumpMinor
                patch = 0
                if (preReleaseVersion != null) {
                    preReleaseVersion = 1
                }
            }
            bumpPatch > 0 -> {
                patch += bumpPatch
                if (preReleaseVersion != null) {
                    preReleaseVersion = 1
                }
            }
            bumpPre > 0 -> {
                preReleaseVersion = preReleaseVersion?.plus(bumpPre)
            }
            forceBumpIfNoChanges -> {
                if ((preReleaseVersion ?: -1) >= 0) {
                    preReleaseVersion = preReleaseVersion?.inc()
                } else {
                    patch += 1
                }
            }
            else -> return updated
        }
        reset()
        updated = true
        return updated
    }

    private fun reset() {
        bumpMajor = 0
        bumpMinor = 0
        bumpPatch = 0
        bumpPre = 0
    }

    fun toVersionString(v2: Boolean = true): String {
        return toInfoVersionString("", 0, v2)
    }

    fun toInfoVersionString(commitCountStringFormat: String = "%03d", shaLength: Int = 0, v2: Boolean = true): String {
        val builder = StringBuilder().append(major).append('.').append(minor).append('.').append(patch)
        if (isPreRelease) {
            builder
                .append('-')
                .append(
                    if (v2)
                        preReleasePrefix
                    else
                        preReleasePrefix.replace("[^0-9A-Za-z-]".toRegex(), "")
                )
                .append(preReleaseVersion ?: "")
        }
        if (v2) {
            var metaSeparator = '+'
            val commitCount = commitCount
            if (commitCount > 0 && commitCountStringFormat.isNotEmpty()) {
                builder.append(metaSeparator).append(commitCountStringFormat.format(commitCount))
                metaSeparator = '.'
            }
            if (shaLength > 0 && sha.isNotEmpty()) {
                builder.append(metaSeparator).append("sha.").append(sha.take(shaLength))
            }
        }
        return builder.toString()
    }

    override fun toString(): String {
        return toInfoVersionString(shaLength = 7)
    }
}