package git.semver.plugin.changelog

import git.semver.plugin.scm.Commit
import git.semver.plugin.semver.SemverSettings

data class ChangeLogFormat(
    val groupByText: Boolean = true,
    val sortByText: Boolean = true,
    val builder: ChangeLogBuilder.() -> Unit
) {
    var changeLogPattern = "\\A(?<Type>\\w+)(?:\\((?<Scope>[^()]+)\\))?!?:\\s*(?<Message>(?:.|\n)*)"

    companion object {
        private const val SCOPE = "Scope"
        private const val TYPE = "Type"
        private const val MESSAGE = "Message"

        val defaultChangeLog = ChangeLogFormat {
            appendLine(constants.header).appendLine()

            withType("release") {
                skip()
            }
            withBreakingChanges {
                appendLine(constants.breakingChange)
                formatChanges {
                    append("- ").append(hash()).appendLine(fullHeader())
                }
                appendLine()
            }
            groupBySorted({ constants.headerTexts[it.scope] ?: constants.headerTexts[it.type] }) {
                appendLine(groupKey)
                with({ constants.headerTexts.containsKey(it.scope) }) {
                    formatChanges {
                        append("- ").append(hash()).append(type()).appendLine(header())
                    }
                }
                formatChanges {
                    append("- ").append(hash()).append(scope()).appendLine(header())
                }
                appendLine()
            }
            otherwise {
                appendLine(constants.otherChange)
                formatChanges {
                    append("- ").append(hash()).appendLine(fullHeader())
                }
                appendLine()
            }
        }

        val simpleChangeLog = ChangeLogFormat {
            appendLine(constants.header).appendLine()

            withBreakingChanges {
                appendLine(constants.breakingChange)
                formatChanges {
                    append("- ").appendLine(fullHeader())
                }
                appendLine()
            }
            withType("fix", "feat") {
                appendLine(constants.headerTexts[groupKey])
                formatChanges {
                    append("- ").append(scope()).appendLine(header())
                }
                appendLine()
            }
        }

        val scopeChangeLog = ChangeLogFormat {
            appendLine(constants.header).appendLine()

            withType("release") {
                skip()
            }

            withBreakingChanges(formatGroupByScopeDisplayType(constants.breakingChange))

            groupBySorted({ constants.headerTexts[it.type] }, {
                appendLine(groupKey).appendLine()
                groupByScope {
                    append("#### ").appendLine(groupKey)
                    formatChanges {
                        append("- ").append(hash()).appendLine(header())
                    }
                    appendLine()
                }
                otherwise {
                    appendLine("#### Missing scope")
                    formatChanges {
                        append("- ").append(hash()).appendLine(header())
                    }
                    appendLine()
                }
                appendLine()
            })

            otherwise (formatGroupByScopeDisplayType(constants.otherChange))
        }

        private fun formatGroupByScopeDisplayType(header: String?): ChangeLogBuilder.() -> Unit = {
            appendLine(header).appendLine()
            groupByScope {
                append("#### ").appendLine(groupKey)
                formatChanges {
                    append("- ").append(hash()).append(type()).appendLine(header())
                }
                appendLine()
            }
            otherwise {
                appendLine("#### Missing scope")
                formatChanges {
                    append("- ").append(hash()).append(type()).appendLine(header())
                }
                appendLine()
            }
            appendLine()
        }
    }

    fun formatLog(changeLog: List<Commit>, settings: SemverSettings, changeLogTexts: ChangeLogTexts): String {
        val context = Context()
        val commitInfos = getCommitInfos(changeLog, settings)
        val changeLogBuilder = ChangeLogBuilder(ChangeLogTexts.HEADER, commitInfos, context, changeLogTexts)
        changeLogBuilder.builder()
        return changeLogBuilder.build()
    }

    private fun getCommitInfos(changeLog: List<Commit>, settings: SemverSettings): List<CommitInfo> {
        val changeLogRegex = changeLogPattern.toRegex(SemverSettings.REGEX_OPTIONS)
        val log = if (sortByText) changeLog.sortedBy {it.text} else changeLog

        if (!groupByText) {
            return log.map { commitInfo(settings, changeLogRegex, it.text, listOf(it)) }
        }
        return log.groupBy { it.text }.map {
            commitInfo(settings, changeLogRegex, it.key, it.value)
        }
    }

    private fun commitInfo(
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