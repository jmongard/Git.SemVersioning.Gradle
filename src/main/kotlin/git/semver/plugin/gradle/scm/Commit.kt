package git.semver.plugin.gradle.scm

class Commit(override val text: String, override val sha: String, val parents: Sequence<Commit>) : IRefInfo
