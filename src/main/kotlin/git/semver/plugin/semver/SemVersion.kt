package git.semver.plugin.semver

import java.io.Serializable

data class SemVersion(
    override val major: Int = 0,
    override val minor: Int = 0,
    override val patch: Int = 0,
    override val preRelease: PreRelease = PreRelease.noPreRelease
) : Serializable, Version {
    override val isPreRelease
        get() = preRelease.isPreRelease

    override fun toString(): String {
        val builder = StringBuilder().append(major).append('.').append(minor).append('.').append(patch)
        if (isPreRelease) {
            builder.append('-').append(preRelease)
        }
        return builder.toString()
    }

}
