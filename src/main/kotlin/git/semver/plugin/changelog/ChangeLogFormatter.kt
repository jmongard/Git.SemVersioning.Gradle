package git.semver.plugin.changelog

import git.semver.plugin.scm.Commit
import git.semver.plugin.semver.SemverSettings

data class ChangeLogFormatter(
    val builder: ChangeLogBuilder.() -> Unit
) {
    companion object {
        private const val SCOPE = "Scope"
        private const val TYPE = "Type"
        private const val MESSAGE = "Message"
    }

    fun formatLog(changeLog: List<Commit>, settings: SemverSettings, changeLogTexts: ChangeLogTexts): String {
        val commitInfos = commitInfos(
            changeLog,
            settings,
            changeLogTexts.changeLogPattern,
            changeLogTexts.sortByText,
            changeLogTexts.groupByText
        )

        val changeLogBuilder = ChangeLogBuilder(ChangeLogTexts.HEADER, commitInfos, Context(), changeLogTexts)
        changeLogBuilder.builder()
        return changeLogBuilder.build()
    }

    private fun commitInfos(
        changeLog: List<Commit>,
        settings: SemverSettings,
        pattern: String,
        sorByText: Boolean,
        groupByText: Boolean
    ): List<CommitInfo> {
        val changeLogRegex = pattern.toRegex(SemverSettings.REGEX_OPTIONS)
        val logEntries = if (sorByText) changeLog.sortedBy { it.text } else changeLog

        return if (groupByText) {
            logEntries.groupBy { it.text }.map { getCommitInfo(settings, changeLogRegex, it.key, it.value) }
        } else {
            logEntries.map { getCommitInfo(settings, changeLogRegex, it.text, listOf(it)) }
        }
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