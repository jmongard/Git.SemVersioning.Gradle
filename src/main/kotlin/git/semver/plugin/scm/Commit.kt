package git.semver.plugin.scm

import java.util.Date

class Commit(override val text: String, override val sha: String, val parents: Sequence<Commit>,
    authorName:String = "", authorEmail:String = "", authorWhen:Date = Date()) : IRefInfo {
    override fun toString(): String = text
}

