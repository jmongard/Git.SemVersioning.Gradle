package git.semver.plugin.semver

interface Version {
    val major: Int
    val minor: Int
    val patch: Int
    val preRelease: PreRelease
    val isPreRelease: Boolean
}