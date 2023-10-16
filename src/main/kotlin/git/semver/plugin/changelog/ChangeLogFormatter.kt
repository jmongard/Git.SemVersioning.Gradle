package git.semver.plugin.changelog

import git.semver.plugin.scm.Commit
import git.semver.plugin.semver.SemverSettings

data class ChangeLogFormatter(
    val groupByText: Boolean = true,
    val sortByText: Boolean = true,
    val changeLogPattern: String = "\\A(?<Type>\\w+)(?:\\((?<Scope>[^()]+)\\))?!?:\\s*(?<Message>(?:.|\n)*)",
    val builder: ChangeLogBuilder.() -> Unit
) {
    companion object {
        private const val SCOPE = "Scope"
        private const val TYPE = "Type"
        private const val MESSAGE = "Message"
    }

    fun formatLog(changeLog: List<Commit>, settings: SemverSettings, changeLogTexts: ChangeLogTexts): String {
        val changeLogRegex = changeLogPattern.toRegex(SemverSettings.REGEX_OPTIONS)
        val logEntries = if (sortByText) changeLog.sortedBy { it.text } else changeLog

        if (!groupByText) {
            return formatLog(changeLogTexts, logEntries.map { getCommitInfo(settings, changeLogRegex, it.text, listOf(it)) })
        }
        return formatLog(changeLogTexts, logEntries.groupBy { it.text }.map {
            getCommitInfo(settings, changeLogRegex, it.key, it.value)
        })
    }

    private fun formatLog(
        changeLogTexts: ChangeLogTexts,
        commitInfos: List<CommitInfo>
    ): String {
        val changeLogBuilder = ChangeLogBuilder(ChangeLogTexts.HEADER, commitInfos, Context(), changeLogTexts)
        changeLogBuilder.builder()
        return changeLogBuilder.build()
    }

    private fun getCommitInfo(
        settings: SemverSettings,
        changeLogRegex: Regex,
        text: String,
        commits: List<Commit>
    ): CommitInfo {
        val isBreakingChange = settings.majorRegex.containsMatchIn(text)
        return changeLogRegex.find(text)?.let {
            CommitInfo(
                commits,
                text,
                isBreakingChange,
                true,
                it.groupValue(TYPE),
                it.groupValue(SCOPE),
                it.groupValue(MESSAGE)
            )
        } ?: CommitInfo(commits, text, isBreakingChange)
    }

    private fun MatchResult.groupValue(groupId: String) = groups[groupId]?.value

    data class CommitInfo(
        val commits: List<Commit>,
        val text: String,
        val isBreaking: Boolean,
        val isChangelogPatternMatch: Boolean = false,
        val type: String? = null,
        val scope: String? = null,
        val message: String? = null
    )

    class Context {
        private val flaggedCommits = mutableSetOf<CommitInfo>()

        fun flagCommit(commit: CommitInfo) {
            flaggedCommits.add(commit)
        }

        fun isCommitFlagged(commit: CommitInfo): Boolean {
            return commit in flaggedCommits
        }
    }
}