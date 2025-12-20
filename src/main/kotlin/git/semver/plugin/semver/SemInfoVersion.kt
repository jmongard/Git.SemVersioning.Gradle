package git.semver.plugin.semver

import java.io.Serializable

/**
 * Extended semantic version information that includes Git metadata and commit details.
 *
 * Provides version information with additional context such as commit SHA, commit count
 * since the last version, and the previous semantic version for enhanced versioning workflows.
 *
 * @property sha The current Git commit SHA hash
 * @property major The major version number
 * @property minor The minor version number
 * @property patch The patch version number
 * @property preRelease The pre-release version information
 * @property commitCount The number of commits since the previous version
 * @property previousVersion The previous semantic version
 */
data class SemInfoVersion(
    val sha: String,
    override val major: Int,
    override val minor: Int,
    override val patch: Int,
    override val preRelease: PreRelease,
    val commitCount: Int,
    val previousVersion: SemVersion
) : Serializable, Version {
    /**
     * Converts this extended version information to a standard semantic version.
     *
     * @return A SemVersion containing only the core version components
     */
    fun toSemVersion(): SemVersion {
        return SemVersion(major, minor, patch, preRelease)
    }

    /**
     * Converts to a detailed version string with customizable formatting options.
     *
     * @param commitCountStringFormat Format string for commit count (default: "%03d")
     * @param shaLength Length of SHA to include, 0 to exclude (default: 0)
     * @param v2 Whether to use v2 formatting (default: true)
     * @param appendPreReleaseLast Whether to append pre-release info at the end (default: false)
     * @param metaSeparator Character to separate metadata components (default: '+')
     * @return The formatted version string with metadata
     */
    fun toInfoVersionString(
        commitCountStringFormat: String = "%03d",
        shaLength: Int = 0,
        v2: Boolean = true,
        appendPreReleaseLast: Boolean = false,
        metaSeparator: Char = '+',
        useTwoDigitVersion: Boolean = false
    ): String {
        if (!v2) {
            return toVersionString(false, useTwoDigitVersion);
        }
        if (appendPreReleaseLast) {
            return VersionBuilder()
                .appendVersion(useTwoDigitVersion)
                .appendBuildMetadata(commitCountStringFormat, shaLength, metaSeparator)
                .appendPrerelease()
                .toString();
        }
        return VersionBuilder()
            .appendVersion(useTwoDigitVersion)
            .appendPrerelease()
            .appendBuildMetadata(commitCountStringFormat, shaLength, metaSeparator)
            .toString();
    }

    /**
     * Converts to a version string using default formatting.
     *
     * @param v2 Whether to use v2 formatting (default: true)
     * @return The formatted version string
     */
    fun toVersionString(v2: Boolean = true,
                        useTwoDigitVersion: Boolean = false): String {
        return VersionBuilder()
            .appendVersion(useTwoDigitVersion)
            .appendPrerelease(v2)
            .toString();
    }

    /**
     * Returns a string representation with a 7-character SHA.
     *
     * @return The version string including SHA metadata
     */
    override fun toString(): String = toInfoVersionString(shaLength = 7)

    /**
     * Generates a revision string in the format "major.minor.patch.commitCount".
     *
     * @return The revision string based on the previous version and commit count
     */
    fun revisionString(): String {
        return "${previousVersion.major}.${previousVersion.minor}.${previousVersion.patch}.$commitCount"
    }

    private inner class VersionBuilder() {
        private val builder: StringBuilder = StringBuilder()

        fun appendVersion(useTwoDigitVersion: Boolean): VersionBuilder {
            builder.append(major).append('.').append(minor)
            if (!useTwoDigitVersion) {
                builder.append('.').append(patch)
            }
            return this
        }

        fun appendPrerelease(v2: Boolean = true): VersionBuilder {
            if (isPreRelease) {
                if (v2) {
                    builder.append('-').append(preRelease)
                } else {
                    builder.append("-")
                        .append(preRelease.prefix.replace("[^0-9A-Za-z-]".toRegex(), ""))
                        .append(preRelease.number ?: "")
                }
            }
            return this
        }

        fun appendBuildMetadata(
            commitCountStringFormat: String,
            shaLength: Int,
            metaSeparator: Char
        ): VersionBuilder {
            var currentMetaSeparator = metaSeparator
            if (commitCount > 0 && commitCountStringFormat.isNotEmpty()) {
                val commitCountStr = commitCountStringFormat.format(commitCount)
                builder.append(currentMetaSeparator).append(commitCountStr)
                currentMetaSeparator = '.'
            }
            val shaTake = sha.take(shaLength)
            if (shaTake.isNotEmpty()) {
                builder.append(currentMetaSeparator).append("sha.").append(shaTake)
            }
            return this
        }

        override fun toString(): String = builder.toString()
    }
}
