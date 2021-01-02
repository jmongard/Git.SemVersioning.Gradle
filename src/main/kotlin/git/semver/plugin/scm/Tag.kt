package git.semver.plugin.scm

data class Tag(override val text: String, override val sha: String) : IRefInfo
