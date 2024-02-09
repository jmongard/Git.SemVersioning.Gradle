package git.semver.plugin.semver

import java.io.Serializable

data class SemVersion(
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0,
    val preRelease: PreRelease = PreRelease.noPreRelease
) : Serializable {

    override fun toString(): String {
        val builder = StringBuilder().append(major).append('.').append(minor).append('.').append(patch)
        if (preRelease.isPreRelease) {
            builder.append('-').append(preRelease)
        }
        return builder.toString()
    }
}
