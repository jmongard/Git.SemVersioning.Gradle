package git.semver.plugin.semver

import git.semver.plugin.scm.IRefInfo
import org.slf4j.LoggerFactory

class SemVersion(
    var sha: String = "",
    var major: Int = 0,
    var minor: Int = 0,
    var patch: Int = 0,
    var preRelease: Pair<String, Int?> = nullPreRelease,
    var commitCount: Int = 0,
    private var bumpPatch: Int = 0,
    private var bumpMinor: Int = 0,
    private var bumpMajor: Int = 0,
    private var bumpPre: Int = 0,
    private val lastReleaseMajor: Int = major,
    private val lastReleaseMinor: Int = minor,
    private var versionChanged: Boolean = false
) : Comparable<SemVersion> {

    constructor(v: SemVersion) : this(
        v.sha, v.major, v.minor, v.patch, v.preRelease, 0,
        v.bumpPatch, v.bumpMinor, v.bumpMajor, v.bumpPre,
        v.lastReleaseMajor, v.lastReleaseMinor, v.versionChanged
    )

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
                parsePrerelease(
                    match.groups["PreRelease"]?.value,
                    match.groups["Revision"]?.value?.toInt()
                )
            )

            logger.debug("Found version: {} in: '{}'", version, refInfo.text)
            return version
        }

        fun isRelease(commit: IRefInfo, settings: SemverSettings): Boolean {
            return settings.releaseRegex.containsMatchIn(commit.text)
        }

        val nullPreRelease = "" to null

        fun parsePrerelease(value: String?, defaultPreReleaseVersion: Int?): Pair<String, Int?> {
            if (value == null) {
                return nullPreRelease
            }

            val prefix = value.trimEnd { it.isDigit() }
            return prefix to
                    if (prefix.length < value.length)
                        value.substring(prefix.length).toInt()
                    else
                        defaultPreReleaseVersion
        }
    }

    val isSnapshot
        get() = commitCount > 0

    val isPreRelease
        get() = preRelease != nullPreRelease

    fun setPreRelease(value: String?) {
        preRelease = parsePrerelease(value, null)
    }

    private fun updatePreReleaseNumber(versionFunc: (Int) -> Int) {
        val preReleaseNumber = preRelease.second
        if (preReleaseNumber != null) {
            preRelease = Pair(preRelease.first, versionFunc(preReleaseNumber))
        }
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
            i = preRelease.first.compareTo(other.preRelease.first)
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
        if (bumpMajor + bumpMinor + bumpPatch == 0) (preRelease.second ?: 1) + bumpPre else 1

    private fun isPreReleaseOrUpdated() =
        isPreRelease || versionChanged || bumpMajor + bumpMinor + bumpPatch > 0

    fun updateFromCommit(commit: IRefInfo, settings: SemverSettings, givenVersion: SemVersion?): Boolean {
        sha = commit.sha

        if (givenVersion != null) {
            if (givenVersion >= this) {
                reset()
                major = givenVersion.major
                minor = givenVersion.minor
                patch = givenVersion.patch
                preRelease = givenVersion.preRelease
                commitCount = 0
                return false
            } else {
                logger.warn("Ignored given version lower than the current version: {} < {} ", givenVersion, this)
            }
        }

        commitCount += 1
        return checkCommitText(settings, commit.text)
    }

    private fun checkCommitText(
        settings: SemverSettings,
        text: String
    ): Boolean {
        when {
            settings.majorRegex.containsMatchIn(text) ->
                if (!isPreRelease || major == lastReleaseMajor) {
                    bumpMajor = 1
                }

            settings.minorRegex.containsMatchIn(text) ->
                if (!isPreRelease || major == lastReleaseMajor && minor == lastReleaseMinor) {
                    bumpMinor = 1
                }

            settings.patchRegex.containsMatchIn(text) ->
                if (!isPreRelease) {
                    bumpPatch = 1
                } else if (preRelease.second != null) {
                    bumpPre = 1
                }

            else -> return false
        }
        return true
    }

    fun applyPendingChanges(forceBumpIfNoChanges: Boolean): Boolean {
        when {
            bumpMajor > 0 -> {
                major += bumpMajor
                minor = 0
                patch = 0
                updatePreReleaseNumber { 1 }
            }

            bumpMinor > 0 -> {
                minor += bumpMinor
                patch = 0
                updatePreReleaseNumber { 1 }
            }

            bumpPatch > 0 -> {
                patch += bumpPatch
                updatePreReleaseNumber { 1 }
            }

            bumpPre > 0 -> {
                updatePreReleaseNumber { it + bumpPre }
            }

            forceBumpIfNoChanges -> {
                if (preRelease.second != null) {
                    updatePreReleaseNumber { it + 1 }
                } else {
                    patch += 1
                }
            }

            else -> return versionChanged
        }
        reset()
        versionChanged = true
        return true
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
                        preRelease.first
                    else
                        preRelease.first.replace("[^0-9A-Za-z-]".toRegex(), "")
                )
                .append(preRelease.second ?: "")
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