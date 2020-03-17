package git.semver.plugin.gradle.scm

data class Tag(override val text: String, override val sha: String) : IRefInfo
