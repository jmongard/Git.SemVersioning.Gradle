package git.semver.plugin.scm

class Commit(override val text: String, override val sha: String, val parents: Sequence<Commit>) : IRefInfo {
    override fun toString(): String = text
}

