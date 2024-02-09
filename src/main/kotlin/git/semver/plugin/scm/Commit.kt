package git.semver.plugin.scm

import java.util.Date

class Commit(override val text: String, override val sha: String, val parents: Sequence<Commit>,
    val authorName:String = "", val authorEmail:String = "", val authorWhen:Date = Date()) : IRefInfo {
    override fun toString(): String = text
}

