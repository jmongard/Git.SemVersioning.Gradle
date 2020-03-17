package git.semver.plugin.gradle.semver

import git.semver.plugin.gradle.scm.IRefInfo
import org.slf4j.LoggerFactory

class SemVersion : Comparable<SemVersion> {
    companion object {
        private val logger = LoggerFactory.getLogger(SemVersion::class.java)
        private val tagPattern = ("""(?<Major>0|[1-9]\d*)\.(?<Minor>0|[1-9]\d*)(?:\.(?<Patch>0|[1-9]\d*)(?:\.(?<Revision>0|[1-9]\d*))?)?"""
                + """(?:-(?<PreRelease>(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?"""
                + """(?:\+(?<BuildMetadata>[0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?""").toRegex()

        fun tryParse(refInfo: IRefInfo): SemVersion? {
            val match = tagPattern.find(refInfo.text) ?: return null

            val preReleaseGroup = match.groups["PreRelease"]
            val patchGroup = match.groups["Patch"]
            val revisionGroup = match.groups["Revision"]

            val version = SemVersion().apply {
                sha = refInfo.sha
                majorVersion = match.groups["Major"]!!.value.toInt()
                minorVersion = match.groups["Minor"]!!.value.toInt()
                patchVersion = patchGroup?.value?.toInt() ?: 0
                setPreRelease(preReleaseGroup?.value)
            }
            if (version.isPreRelease && version.preReleaseVersion == null && revisionGroup != null) {
                version.preReleaseVersion = revisionGroup.value.toInt()
            }
            logger.debug("Found version: {} in: '{}'", version, refInfo.text)
            return version
        }

        fun isRelease(commit: IRefInfo, settings: SemverSettings): Boolean {
            return settings.releaseRegex.containsMatchIn(commit.text)
        }
    }

    var majorVersion = 0
    var minorVersion = 0
    var patchVersion = 0
    var commitCount = 0
    var preReleasePrefix: String? = null
    var preReleaseVersion: Int? = null
    var sha = ""
    val isPreRelease get() = preReleasePrefix != null || preReleaseVersion ?: -1 > 0

    private fun setPreRelease(value: String?) {
        if (value == null) {
            preReleaseVersion = null
            preReleasePrefix = null
        } else {
            val prefix = value.trimEnd { it.isDigit() }
            if (prefix.length < value.length) {
                preReleaseVersion = value.substring(prefix.length).toInt()
                preReleasePrefix = if (prefix.isNotEmpty()) prefix else null
            } else {
                preReleaseVersion = null
                preReleasePrefix = value
            }
        }
    }

    override fun compareTo(other: SemVersion): Int {
        var i = majorVersion.compareTo(other.majorVersion + other.bumpMajor)
        if (i == 0) {
            i = minorVersion.compareTo(other.minorVersion + other.bumpMinor)
        }
        if (i == 0) {
            i = patchVersion.compareTo(other.patchVersion + other.bumpPatch)
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

    private var bumpPatch = 0
    private var bumpMinor = 0
    private var bumpMajor = 0
    private var bumpPre = 0

    private var currentPreRelease: SemVersion? = null

    fun updateFromCommit(commit: IRefInfo, settings: SemverSettings, preReleaseFromTag: SemVersion? = null) {
        sha = commit.sha
        commitCount += 1

        val preRelease = currentPreRelease
        when {
            preReleaseFromTag != null -> {
                val v = preRelease ?: this
                if (preReleaseFromTag > v) {
                    currentPreRelease = preReleaseFromTag
                }
            }
            isRelease(commit, settings) -> {
                val v = preRelease ?: this
                applyPendingChanges(v)
                v.commitCount = 0
            }
            preRelease != null -> {
                preRelease.commitCount += 1
                when {
                    preRelease.majorVersion == majorVersion
                            && settings.majorRegex.containsMatchIn(commit.text) -> bumpMajor = 1

                    preRelease.majorVersion == majorVersion
                            && preRelease.minorVersion == minorVersion
                            && settings.minorRegex.containsMatchIn(commit.text) -> bumpMinor = 1

                    else -> bumpPre = 1
                }
            }
            settings.majorRegex.containsMatchIn(commit.text) -> bumpMajor += 1
            settings.minorRegex.containsMatchIn(commit.text) -> bumpMinor += 1
            settings.patchRegex.containsMatchIn(commit.text) -> bumpPatch += 1
        }
    }

    fun calculateNewVersion(isDirty: Boolean, defaultPreRelease: String) {
        if (currentPreRelease == null && (commitCount > 0 || isDirty)) {
            setPreRelease(defaultPreRelease)
            if (bumpMajor + bumpMinor + bumpPatch == 0) {
                bumpPatch = 1
            }
        }
        applyPendingChanges(currentPreRelease ?: this)
    }

    private fun applyPendingChanges(v: SemVersion) {
        val preRelease = v.preReleaseVersion
        when {
            bumpMajor > 0 -> {
                v.majorVersion += bumpMajor
                v.minorVersion = 0
                v.patchVersion = 0
                v.preReleaseVersion = if (preRelease != null) 0 else null
            }
            bumpMinor > 0 -> {
                v.minorVersion += bumpMinor
                v.patchVersion = 0
                v.preReleaseVersion = if (preRelease != null) 0 else null
            }
            bumpPatch > 0 -> {
                v.patchVersion += bumpPatch
                v.preReleaseVersion = if (preRelease != null) 0 else null
            }
            bumpPre > 0 -> {
                v.preReleaseVersion = if (preRelease != null) preRelease + bumpPre else null
            }
        }
        bumpMajor = 0
        bumpMinor = 0
        bumpPatch = 0
        bumpPre = 0
    }

    fun toVersionString(): String {
        val v = currentPreRelease ?: this
        return "${v.majorVersion}.${v.minorVersion}.${v.patchVersion}"
    }

    fun toInfoVersionString(commitCountStringFormat: String = "%03d", shaLength: Int = 0, v2: Boolean = true): String {
        val v = currentPreRelease ?: this
        val builder = StringBuilder()
        builder.append(v.majorVersion).append('.').append(v.minorVersion).append('.').append(v.patchVersion)

        if (v.isPreRelease) {
            val preReleasePrefix = v.preReleasePrefix
            val preReleaseVersion = v.preReleaseVersion
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
            val commitCount = v.commitCount
            if (commitCount > 0 && commitCountStringFormat.isNotEmpty()) {
                builder.append(metaSeparator).append(commitCountStringFormat.format(commitCount))
                metaSeparator = '.'
            }
            if (shaLength > 0) {
                builder.append(metaSeparator).append("sha.").append(v.sha.take(shaLength))
            }
        }
        return builder.toString()
    }

    override fun toString(): String {
        return toInfoVersionString(shaLength = 7)
    }
}