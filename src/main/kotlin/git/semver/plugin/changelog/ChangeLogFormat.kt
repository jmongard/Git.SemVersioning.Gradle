package git.semver.plugin.changelog

import git.semver.plugin.scm.Commit
import git.semver.plugin.semver.SemverSettings

private const val SCOPE = "Scope"
private const val TYPE = "Type"
private const val MESSAGE = "Message"

const val HEADER = "#"
const val BREAKING_CHANGE = "!"
const val OTHER_CHANGE = "?"

data class ChangeLogFormat(
    val groupByText: Boolean = true,
    val sortByText: Boolean = true,
    val builder: ChangeLogBuilder.() -> Unit
) {
    var changeLogPattern = "\\A(?<Type>\\w+)(?:\\((?<Scope>[^()]+)\\))?!?:\\s*(?<Message>(?:.|\n)*)"

    companion object {
        val defaultHeaderTexts = mutableMapOf(
            HEADER to "## What's Changed",
            BREAKING_CHANGE to "### Breaking Changes 🛠",
            OTHER_CHANGE to "### Other Changes \uD83D\uDCA1",
            "fix" to "### Bug Fixes \uD83D\uDC1E",
            "feat" to "### New Features \uD83C\uDF89",
            "test" to "### Tests ✅",
            "docs" to "### Docs \uD83D\uDCD6",
            "deps" to "### Dependency updates \uD83D\uDE80",
            "build" to "### Build \uD83D\uDC18 & CI ⚙\uFE0F",
            "ci" to "### Build \uD83D\uDC18 & CI ⚙\uFE0F",
            "chore" to "### Chores \uD83D\uDD27",
            "perf" to "### Performance Enhancements ⚡",
            "refactor" to "### Refactorings \uD83D\uDE9C"
        )

        val defaultChangeLog = ChangeLogFormat {
            appendLine(defaultHeaderTexts[HEADER]).appendLine()

            withType("release") {
                skip()
            }
            withBreakingChanges {
                appendLine(defaultHeaderTexts[BREAKING_CHANGE])
                formatChanges {
                    append("- ").append(hash()).appendLine(fullHeader())
                }
                appendLine()
            }
            groupBySorted({ defaultHeaderTexts[it.scope] ?: defaultHeaderTexts[it.type] }) {
                appendLine(groupKey)
                with({ defaultHeaderTexts.containsKey(it.scope) }) {
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
                appendLine(defaultHeaderTexts[OTHER_CHANGE])
                formatChanges {
                    append("- ").append(hash()).appendLine(fullHeader())
                }
                appendLine()
            }
        }

        val simpleChangeLog = ChangeLogFormat {
            appendLine(defaultHeaderTexts[HEADER]).appendLine()

            withBreakingChanges {
                appendLine(defaultHeaderTexts[BREAKING_CHANGE])
                formatChanges {
                    append("- ").appendLine(fullHeader())
                }
                appendLine()
            }
            withType("fix", "feat") {
                appendLine(defaultHeaderTexts[groupKey])
                formatChanges {
                    append("- ").append(scope()).appendLine(header())
                }
                appendLine()
            }
        }

        val scopeChangeLog = ChangeLogFormat {
            appendLine(defaultHeaderTexts[HEADER]).appendLine()

            withType("release") {
                skip()
            }

            withBreakingChanges(formatGroupByScopeDisplayType(defaultHeaderTexts[BREAKING_CHANGE]))

            groupBySorted({ defaultHeaderTexts[it.type] }, {
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

            otherwise (formatGroupByScopeDisplayType(defaultHeaderTexts[OTHER_CHANGE]))
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

    fun formatLog(changeLog: List<Commit>, settings: SemverSettings): String {
        val context = Context()
        val commitInfos = getCommitInfos(changeLog, settings)
        val changeLogBuilder = ChangeLogBuilder(HEADER, commitInfos, context)
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