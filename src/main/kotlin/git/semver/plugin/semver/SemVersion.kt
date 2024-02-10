package git.semver.plugin.semver

import java.io.Serializable

data class SemVersion(
    override val major: Int,
    override val minor: Int,
    override val patch: Int,
    override val preRelease: PreRelease = PreRelease.noPreRelease
) : Serializable, Version {

    override fun toString(): String {
        val builder = StringBuilder().append(major).append('.').append(minor).append('.').append(patch)
        if (isPreRelease) {
            builder.append('-').append(preRelease)
        }
        return builder.toString()
    }

}
