package git.semver.plugin.changelog

import git.semver.plugin.changelog.ChangeLogTexts.Companion.BREAKING_CHANGE
import java.util.*

class ChangeLogBuilder(
    val groupKey: String? = null,
    private val commitInfos: List<ChangeLogFormatter.CommitInfo>,
    private val context: ChangeLogFormatter.Context,
    val constants: ChangeLogTexts
) : DocumentBuilder() {

    fun formatChanges(block: ChangeLogTextFormatter.() -> Unit) {
        for (commitInfo in remainingCommitInfos()) {
            val formatter = ChangeLogTextFormatter(commitInfo)
            formatter.block()
            append(formatter.build())
            context.flagCommit(commitInfo)
        }
    }

    fun skip() {
        skip(remainingCommitInfos())
    }

    private fun skip(commitInfoList: List<ChangeLogFormatter.CommitInfo>) {
        for (commitInfo in commitInfoList) {
            context.flagCommit(commitInfo)
        }
    }

    //<editor-fold desc="filter">
    fun withBreakingChanges(block: ChangeLogBuilder.() -> Unit) {
        with({ it.isBreaking }, BREAKING_CHANGE, block)
    }

    fun withScope(vararg scopes: String, block: ChangeLogBuilder.() -> Unit) {
        for (scope in scopes) {
            with({ it.scope == scope }, scope, block)
        }
    }

    fun withType(vararg types: String, block: ChangeLogBuilder.() -> Unit) {
        for (type in types) {
            with({ it.type == type }, type, block)
        }
    }

    fun otherwise(block: ChangeLogBuilder.() -> Unit) {
        with({ true }, block)
    }

    fun with(filter: (ChangeLogFormatter.CommitInfo) -> Boolean, block: ChangeLogBuilder.() -> Unit) =
        with(filter, null, block)

    fun with(
        predicate: (ChangeLogFormatter.CommitInfo) -> Boolean,
        key: String?,
        block: ChangeLogBuilder.() -> Unit
    ) {
        val filteredCommits = remainingCommitInfos().filter(predicate)
        if (filteredCommits.isNotEmpty()) {
            processCommits(key, filteredCommits, block)
        }
    }
    //</editor-fold>

    //<editor-fold desc="grouping">
    fun groupByScope(block: ChangeLogBuilder.() -> Unit) {
        groupBySorted({ it.scope }, block)
    }

    fun groupByType(block: ChangeLogBuilder.() -> Unit) {
        groupBySorted({ it.type }, block)
    }

    fun groupBy(keySelector: (ChangeLogFormatter.CommitInfo) -> String?, block: ChangeLogBuilder.() -> Unit) {
        processCommitsGrouped(keySelector, LinkedHashMap(), block)
    }

    fun groupBySorted(
        keySelector: (ChangeLogFormatter.CommitInfo) -> String?,
        block: ChangeLogBuilder.() -> Unit
    ) {
        processCommitsGrouped(keySelector, TreeMap(), block)
    }

    private fun processCommitsGrouped(
        keySelector: (ChangeLogFormatter.CommitInfo) -> String?,
        destination: MutableMap<String, MutableList<ChangeLogFormatter.CommitInfo>>,
        block: ChangeLogBuilder.() -> Unit
    ) {
        val groupedCommits = remainingCommitInfos()
            .mapNotNull { keyMapper(keySelector, it) }
            .groupByTo(destination, { it.first }, { it.second })

        for ((key, commits) in groupedCommits) {
            if (key.isEmpty()) {
                skip(commits)
                continue;
            }
            processCommits(key, commits, block)
        }
    }

    private fun keyMapper(
        keySelector: (ChangeLogFormatter.CommitInfo) -> String?,
        it: ChangeLogFormatter.CommitInfo
    ): Pair<String, ChangeLogFormatter.CommitInfo>? {
        return (keySelector(it) ?: return null) to it
    }

    private fun processCommits(
        key: String?,
        commits: List<ChangeLogFormatter.CommitInfo>,
        block: ChangeLogBuilder.() -> Unit
    ) {
        val builder = ChangeLogBuilder(key, commits, context, constants)
        block(builder)
        append(builder.build())
    }

    //</editor-fold>

    fun remainingCommitInfos() = commitInfos.filter { !context.isCommitFlagged(it) }
}

class ChangeLogTextFormatter(
    val commitInfo: ChangeLogFormatter.CommitInfo
) : DocumentBuilder() {
    companion object {
        fun sanitizeHtml(str: String): String {
            if (str.indexOf('<') == -1) {
                return str
            }

            val result = StringBuilder(str.length)
            var backtickCount = 0
            var index = 0;
            while (index < str.length) {
                val token = str[index]
                when {
                    token == '`' -> backtickCount += 1

                    backtickCount > 0 -> {
                        val end = str.indexOf("`".repeat(backtickCount), index) + backtickCount
                        if (end > index) {
                            result.append(str, index, end)
                            index = end
                        }
                        backtickCount = 0
                        continue
                    }

                    token == '<' -> result.append("\\")
                }
                result.append(token)
                index += 1;
            }

            return result.toString()
        }
    }

    fun header() = (sanitizeHtml(commitInfo.message ?: commitInfo.text)).lineSequence().first()

    fun body() = sanitizeHtml(commitInfo.text).lineSequence()
        .drop(1)
        .dropWhile { it.isEmpty() }
        .takeWhile { it.isNotEmpty() }

    fun fullHeader() = sanitizeHtml(commitInfo.text).lineSequence().first()

    fun scope(format: String = "%s: ") = commitInfo.scope?.let { format.format(it) }.orEmpty()

    fun type(format: String = "%s: ") = commitInfo.type?.let { format.format(it) }.orEmpty()
    fun hash(format: String = "%s", len: Int = 40) =
        commitInfo.commits.joinToString(" ", "", " ") { format.format(it.sha.take(len)) }

    fun authorName(format: String = "%s") =
        commitInfo.commits.map{ format.format(it.authorName) }.distinct().joinToString(" ", "", "")
    fun authorNameAndEmail(format: String = "%s <%s>") =
        commitInfo.commits.map { format.format(it.authorName, it.authorEmail)}.distinct().joinToString(" ", "", "")
}

open class DocumentBuilder {
    private val out = StringBuilder()

    fun build(): String {
        return out.toString()
    }

    fun append(t: String?): DocumentBuilder {
        out.append(t)
        return this
    }

    fun appendLine(t: String? = ""): DocumentBuilder {
        out.appendLine(t)
        return this
    }
}