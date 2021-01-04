package git.semver.plugin.semver

import git.semver.plugin.scm.IRefInfo
import org.slf4j.LoggerFactory

class SemVersion(
    var sha: String = "",
    var majorVersion: Int = 0,
    var minorVersion: Int = 0,
    var patchVersion: Int = 0,
    preRelease: String? = null,
    revision: Int? = null
) : Comparable<SemVersion> {

    companion object {
        private val logger = LoggerFactory.getLogger(SemVersion::class.java)
        private val tagPattern =
            ("""(?<Major>0|[1-9]\d*)\.(?<Minor>0|[1-9]\d*)(?:\.(?<Patch>0|[1-9]\d*)(?:\.(?<Revision>0|[1-9]\d*))?)?"""
                    + """(?:-(?<PreRelease>(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?"""
                    + """(?:\+(?<BuildMetadata>[0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?""").toRegex()

        fun tryParse(refInfo: IRefInfo): SemVersion? {
            val match = tagPattern.find(refInfo.text) ?: return null

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

    private var lastReleaseMajor:Int = majorVersion
    private var lastReleaseMinor:Int = minorVersion
    var commitCount = 0
    var preReleasePrefix: String? = null
    var preReleaseVersion: Int? = null

    private var bumpPatch = 0
    private var bumpMinor = 0
    private var bumpMajor = 0
    private var bumpPre = 0

    init {
        setPreRelease(preRelease, revision)
    }

    val isPreRelease
        get() = preReleasePrefix != null || preReleaseVersion ?: -1 > 0

    private val isPendingChanges
        get() = bumpMajor + bumpMinor + bumpPatch + bumpPre > 0

    fun setPreRelease(value: String?, defaultPreReleaseVersion: Int? = null) {
        bumpPre = 0

        if (value == null) {
            preReleaseVersion = null
            preReleasePrefix = null
            return
        }

        val prefix = value.trimEnd { it.isDigit() }
        preReleasePrefix = if (prefix.isNotEmpty()) prefix else null
        preReleaseVersion = if (prefix.length < value.length) {
            value.substring(prefix.length).toInt()
        } else {
            defaultPreReleaseVersion
        }
    }

    override fun compareTo(other: SemVersion): Int {
        var i = majorVersion.compareTo(other.majorVersion + other.bumpMajor)
        if (i == 0) {
            i = minorVersion.compareTo(if (other.bumpMajor == 0) other.minorVersion + other.bumpMinor else 0)
        }
        if (i == 0) {
            i = patchVersion.compareTo(if (other.bumpMajor + other.bumpMinor == 0) other.patchVersion + other.bumpPatch else 0)
        }
        if (i == 0) {
            i = -isPreRelease.compareTo(other.isPreRelease)
        }
        if (i == 0) {
            i = (preReleasePrefix ?: "").compareTo(other.preReleasePrefix ?: "")
        }
        if (i == 0) {
            i = (preReleaseVersion ?: 0).compareTo(other.preReleaseVersion ?: 0 + other.bumpPre)
        }
        if (i == 0) {
            i = commitCount.compareTo(other.commitCount)
        }
        return i
    }

    fun updateFromCommit(commit: IRefInfo, settings: SemverSettings, newPreRelease: SemVersion? = null) {
        sha = commit.sha
        commitCount += 1

        when {
            newPreRelease != null -> {
                if (newPreRelease > this) {
                    reset()
                    majorVersion = newPreRelease.majorVersion
                    minorVersion = newPreRelease.minorVersion
                    patchVersion = newPreRelease.patchVersion
                    preReleasePrefix = newPreRelease.preReleasePrefix
                    preReleaseVersion = newPreRelease.preReleaseVersion
                    commitCount = 0
                }
                else {
                    logger.warn("Ignored: {} < {} " , newPreRelease, this)
                }
            }
            isPreRelease -> {
                when {
                    majorVersion == lastReleaseMajor
                            && settings.majorRegex.containsMatchIn(commit.text) -> bumpMajor = 1

                    majorVersion == lastReleaseMajor
                            && minorVersion == lastReleaseMinor
                            && settings.minorRegex.containsMatchIn(commit.text) -> bumpMinor = 1

                    else -> bumpPre = 1
                }
            }
            settings.majorRegex.containsMatchIn(commit.text) -> bumpMajor = 1
            settings.minorRegex.containsMatchIn(commit.text) -> bumpMinor = 1
            settings.patchRegex.containsMatchIn(commit.text) -> bumpPatch = 1
        }
    }

    fun applyPendingChanges(autoBump: Boolean) {
        if (autoBump && !isPendingChanges) {
            if (isPreRelease) {
                bumpPre = 1
            }
            else {
                bumpPatch = 1
            }
        }
        val preVer = preReleaseVersion
        when {
            bumpMajor > 0 -> {
                majorVersion += bumpMajor
                minorVersion = 0
                patchVersion = 0
                preReleaseVersion = if (preVer != null) 0 else null
            }
            bumpMinor > 0 -> {
                minorVersion += bumpMinor
                patchVersion = 0
                preReleaseVersion = if (preVer != null) 0 else null
            }
            bumpPatch > 0 -> {
                patchVersion += bumpPatch
                preReleaseVersion = if (preVer != null) 0 else null
            }
            bumpPre > 0 -> {
                preReleaseVersion = if (preVer != null) preVer + bumpPre else null
            }
        }
        reset()
    }

    private fun reset() {
        bumpMajor = 0
        bumpMinor = 0
        bumpPatch = 0
        bumpPre = 0
    }

    fun toVersionString(): String {
        return "${majorVersion}.${minorVersion}.${patchVersion}"
    }

    fun toInfoVersionString(commitCountStringFormat: String = "%03d", shaLength: Int = 0, v2: Boolean = true): String {
        val builder = StringBuilder()
        builder.append(majorVersion).append('.').append(minorVersion).append('.').append(patchVersion)

        if (isPreRelease) {
            val preReleasePrefix = preReleasePrefix
            val preReleaseVersion = preReleaseVersion
            builder.append('-')
            if (preReleasePrefix != null) {
                builder.append(if (v2) preReleasePrefix else preReleasePrefix.replace("[^0-9A-Za-z-]".toRegex(), ""))
            }
            if (preReleaseVersion != null) {
                builder.append(preReleaseVersion)
            }
        }
        if (v2) {
            var metaSeparator = '+'
            val commitCount = commitCount
            if (commitCount > 0 && commitCountStringFormat.isNotEmpty()) {
                builder.append(metaSeparator).append(commitCountStringFormat.format(commitCount))
                metaSeparator = '.'
            }
            if (shaLength > 0) {
                builder.append(metaSeparator).append("sha.").append(sha.take(shaLength))
            }
        }
        return builder.toString()
    }

    override fun toString(): String {
        return toInfoVersionString(shaLength = 7) +
                if (isPendingChanges) "+($bumpMajor;$bumpMinor;$bumpPatch;$bumpPre)" else ""
    }
}