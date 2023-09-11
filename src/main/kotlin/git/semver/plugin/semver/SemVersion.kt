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
    private val lastReleaseMinor: Int = minor
) : Comparable<SemVersion> {

    constructor(v: SemVersion) : this(
        v.sha, v.major, v.minor, v.patch, v.preRelease, 0,
        v.bumpPatch, v.bumpMinor, v.bumpMajor, v.bumpPre,
        v.lastReleaseMajor, v.lastReleaseMinor
    )

    companion object {
        private val logger = LoggerFactory.getLogger(SemVersion::class.java)
        private const val numeric = "0|[1-9]\\d*"
        private const val alphaNumeric = "[0-9a-zA-Z-]"
        private const val preVersion = "(?:$numeric|\\d*[a-zA-Z-]$alphaNumeric*)"
        private val semVersionPattern = (
                """(?<Major>$numeric)\.(?<Minor>$numeric)(?:\.(?<Patch>$numeric)(?:\.(?<Revision>$numeric))?)?"""
                        + """(?:-(?<PreRelease>$preVersion(?:\.$preVersion)*))?"""
                        + """(?:\+(?<BuildMetadata>$alphaNumeric+(?:\.$alphaNumeric+)*))?"""
                ).toRegex()

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

    val isPreRelease
        get() = preRelease != nullPreRelease

    internal fun setPreRelease(value: String?) {
        preRelease = parsePrerelease(value, null)
    }

    override fun compareTo(other: SemVersion): Int {
        return compareValuesBy(this, other,
            { it.major },
            { it.minor },
            { it.patch },
            { !it.isPreReleaseOrUpdated() },
            { it.preRelease.first },
            { it.preRelease.second },
            { it.commitCount }
        )
    }

    private fun isPreReleaseOrUpdated() =
        isPreRelease || bumpMajor + bumpMinor + bumpPatch > 0

    internal fun updateFromCommit(commit: IRefInfo, settings: SemverSettings, preReleaseVersion: SemVersion?) {
        sha = commit.sha

        if (preReleaseVersion != null) {
            if (preReleaseVersion >= this) {
                major = preReleaseVersion.major
                minor = preReleaseVersion.minor
                patch = preReleaseVersion.patch
                preRelease = preReleaseVersion.preRelease
                commitCount = 0
                resetPendingChanges()
                return
            } else {
                logger.warn("Ignored given version lower than the current version: {} < {} ", preReleaseVersion, this)
            }
        }

        commitCount += 1
        checkCommitText(settings, commit.text)
    }

    private fun checkCommitText(
        settings: SemverSettings,
        text: String
    ) {
        when {
            settings.majorRegex.containsMatchIn(text) ->
                if (!isPreRelease || major == lastReleaseMajor) {
                    bumpMajor += 1
                    bumpMinor = 0
                    bumpPatch = 0
                    bumpPre = 0
                }

            settings.minorRegex.containsMatchIn(text) ->
                if (!isPreRelease || major == lastReleaseMajor && minor == lastReleaseMinor) {
                    bumpMinor += 1
                    bumpPatch = 0
                    bumpPre = 0
                }

            settings.patchRegex.containsMatchIn(text) ->
                if (!isPreRelease) {
                    bumpPatch += 1
                    bumpPre = 0
                } else if (preRelease.second != null) {
                    bumpPre += 1
                }
        }
    }

    internal fun applyPendingChanges(forceBumpIfNoChanges: Boolean, groupChanges: Boolean): Boolean {
        if (groupChanges) {
            applyChangesGrouped()
        } else {
            applyChangesNotGrouped()
        }

        if (bumpMajor + bumpMinor + bumpPatch + bumpPre > 0) {
            resetPendingChanges()
            return true
        }

        if (!forceBumpIfNoChanges) {
            return false
        }

        if (preRelease.second != null) {
            updatePreReleaseNumber { it + 1 }
        } else {
            patch += 1
        }
        return true
    }

    private fun applyChangesNotGrouped() {
        if (bumpMajor > 0) {
            updateMajor(bumpMajor)
        }
        if (bumpMinor > 0) {
            updateMinor(bumpMinor)
        }
        if (bumpPatch > 0) {
            updatePatch(bumpPatch)
        }
        if (bumpPre > 0) {
            updatePreReleaseNumber { it + bumpPre }
        }
    }

    private fun applyChangesGrouped() {
        when {
            bumpMajor > 0 -> {
                updateMajor(1)
            }
            bumpMinor > 0 -> {
                updateMinor(1)
            }
            bumpPatch > 0 -> {
                updatePatch(1)
            }
            bumpPre > 0 -> {
                updatePreReleaseNumber { it + 1 }
            }
        }
    }

    private fun updateMajor(i: Int) {
        major += i
        minor = 0
        patch = 0
        updatePreReleaseNumber { 1 }
    }

    private fun updateMinor(i: Int) {
        minor += i
        patch = 0
        updatePreReleaseNumber { 1 }
    }

    private fun updatePatch(i: Int) {
        patch += i
        updatePreReleaseNumber { 1 }
    }

    private fun updatePreReleaseNumber(updateFunction: (Int) -> Int) {
        val preReleaseNumber = preRelease.second
        if (preReleaseNumber != null) {
            preRelease = Pair(preRelease.first, updateFunction(preReleaseNumber))
        }
    }

    internal fun mergeChanges(versions: List<SemVersion>) {
        this.commitCount = versions.map { it.commitCount }.sum()
        this.bumpPatch = versions.map { it.bumpPatch }.sum()
        this.bumpMinor = versions.map { it.bumpMinor }.sum()
        this.bumpMajor = versions.map { it.bumpMajor }.sum()
        this.bumpPre = versions.map { it.bumpPre }.sum()
    }

    private fun resetPendingChanges() {
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