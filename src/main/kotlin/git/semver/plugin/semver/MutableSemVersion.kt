package git.semver.plugin.semver

import git.semver.plugin.scm.IRefInfo
import org.slf4j.LoggerFactory

internal class MutableSemVersion(
    var sha: String = "",
    var major: Int = 0,
    var minor: Int = 0,
    var patch: Int = 0,
    var preRelease: PreRelease = PreRelease.noPreRelease,
    var commitCount: Int = 0,
    private var bumpPatch: Int = 0,
    private var bumpMinor: Int = 0,
    private var bumpMajor: Int = 0,
    private var bumpPre: Int = 0,
    private val lastReleaseMajor: Int = major,
    private val lastReleaseMinor: Int = minor,
    private val lastReleasePatch: Int = patch
) : Comparable<MutableSemVersion> {

    companion object {
        private val logger = LoggerFactory.getLogger(MutableSemVersion::class.java)
        private const val NUMERIC = "0|[1-9]\\d*"
        private const val ALPHA_NUMERIC = "[0-9a-zA-Z-]"
        private const val PRE_VERSION = "(?:$NUMERIC|\\d*[a-zA-Z-]$ALPHA_NUMERIC*)"
        private val semVersionPattern = (
                """(?<Major>$NUMERIC)\.(?<Minor>$NUMERIC)(?:\.(?<Patch>$NUMERIC)(?:\.(?<Revision>$NUMERIC))?)?"""
                        + """(?:-(?<PreRelease>$PRE_VERSION(?:\.$PRE_VERSION)*))?"""
                        + """(?:\+(?<BuildMetadata>$ALPHA_NUMERIC+(?:\.$ALPHA_NUMERIC+)*))?"""
                ).toRegex()

        fun tryParse(refInfo: IRefInfo): MutableSemVersion? {
            val match = semVersionPattern.find(refInfo.text) ?: return null
            fun getInt(group: String) = match.groups[group]?.value?.toInt()

            val version = MutableSemVersion(
                refInfo.sha,
                getInt("Major")!!,
                getInt("Minor")!!,
                getInt("Patch") ?: 0,
                parsePreRelease(
                    match.groups["PreRelease"]?.value,
                    getInt("Revision")
                )
            )
            logger.debug("Found version: {} in: '{}'", version, refInfo.text)
            return version
        }

        internal fun parsePreRelease(value: String?, defaultPreReleaseVersion: Int? = null): PreRelease {
            if (value == null) {
                return PreRelease.noPreRelease
            }

            val prefix = value.trimEnd { it.isDigit() }
            return PreRelease(prefix,
                if (prefix.length < value.length)
                    value.substring(prefix.length).toInt()
                else
                    defaultPreReleaseVersion)
        }

        fun isRelease(commit: IRefInfo, settings: SemverSettings): Boolean {
            return settings.releaseRegex.containsMatchIn(commit.text)
        }
    }

    val isPreRelease
        get() = preRelease.isPreRelease

    internal fun setPreRelease(value: String?) {
        preRelease = parsePreRelease(value)
    }

    override fun compareTo(other: MutableSemVersion): Int {
        return compareValuesBy(this, other,
            { it.major },
            { it.minor },
            { it.patch },
            { !it.isPreRelease },
            { it.preRelease.prefix },
            { it.preRelease.number },
            { it.commitCount }
        )
    }

    internal fun updateFromCommit(commit: IRefInfo, settings: SemverSettings, preReleaseVersion: MutableSemVersion?) {
        sha = commit.sha

        if (preReleaseVersion != null) {
            if (preReleaseVersion >= this) {
                major = preReleaseVersion.major
                minor = preReleaseVersion.minor
                patch = preReleaseVersion.patch
                preRelease = preReleaseVersion.preRelease
                commitCount = 0
                resetPendingChanges()
                logger.debug(
                    "Version after commit(\"{}\") with pre-release: {}",
                    commit,
                    this
                )
                return
            } else {
                logger.warn("Ignored given version lower than the current version: {} < {} ", preReleaseVersion, this)
            }
        }

        commitCount += 1
        checkCommitText(settings, commit.text)

        logger.debug(
            "Version after commit(\"{}\"): {} +({}.{}.{}-{})",
            commit,
            this,
            bumpMajor,
            bumpMinor,
            bumpPatch,
            bumpPre
        )
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
                } else if (preRelease.number != null) {
                    bumpPre += 1
                }
        }
    }

    internal fun applyPendingChanges(forceBumpIfNoChanges: Boolean, groupChanges: Boolean): Boolean {
        if (hasPendingChanges) {
            if (groupChanges) {
                applyChangesGrouped()
            } else {
                applyChangesNotGrouped()
            }
            resetPendingChanges()
            return true
        }

        if (!forceBumpIfNoChanges) {
            return false
        }

        val preReleaseNumber = preRelease.number
        if (preReleaseNumber != null) {
            preRelease = PreRelease(preRelease.prefix, preReleaseNumber + 1)
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
            else -> { // bumpPre > 0
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
        val preReleaseNumber = preRelease.number
        if (preReleaseNumber != null) {
            preRelease = PreRelease(preRelease.prefix, updateFunction(preReleaseNumber))
        }
    }

    internal fun mergeChanges(versions: List<MutableSemVersion>) {
        this.commitCount = versions.sumOf { it.commitCount }
        this.bumpPatch = versions.sumOf { it.bumpPatch }
        this.bumpMinor = versions.sumOf { it.bumpMinor }
        this.bumpMajor = versions.sumOf { it.bumpMajor }
        this.bumpPre = versions.sumOf { it.bumpPre }
    }

    private val hasPendingChanges
        get() = bumpMajor + bumpMinor + bumpPatch + bumpPre > 0

    private fun resetPendingChanges() {
        bumpMajor = 0
        bumpMinor = 0
        bumpPatch = 0
        bumpPre = 0
    }

    fun revisionString(): String {
        return "$lastReleaseMajor.$lastReleaseMinor.$lastReleasePatch.$commitCount"
    }

    fun toSemVersion(): SemInfoVersion {
        return SemInfoVersion(sha, major, minor, patch, preRelease, commitCount);
    }
}