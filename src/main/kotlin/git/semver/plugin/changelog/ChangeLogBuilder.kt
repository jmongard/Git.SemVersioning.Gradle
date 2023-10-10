package git.semver.plugin.changelog

import java.util.*

open class ChangeLogBuilder(
    val groupKey: String? = null,
    private val commitInfos: List<ChangeLogFormat.CommitInfo>,
    private val context: ChangeLogFormat.Context
) : DocumentBuilder() {

    fun formatChanges(block: ChangeLogTextFormatter.() -> Unit) {
        for (commitInfo in commitInfos()) {
            val formatter = ChangeLogTextFormatter(commitInfo)
            formatter.block()
            append(formatter.build())
            context.flagCommit(commitInfo)
        }
    }

    fun skip() {
        for (commitInfo in commitInfos()) {
            context.flagCommit(commitInfo)
        }
    }

    //<editor-fold desc="filter">
    fun withBreakingChanges(block: ChangeLogBuilder.() -> Unit) {
        with({ it.isBreaking }, "!", block)
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

    fun with(filter: (ChangeLogFormat.CommitInfo) -> Boolean, block: ChangeLogBuilder.() -> Unit) =
        with(filter, null, block)

    fun with(
        filter: (ChangeLogFormat.CommitInfo) -> Boolean,
        key: String?,
        block: ChangeLogBuilder.() -> Unit
    ) {
        val filteredCommits = commitInfos().filter(filter)
        if (filteredCommits.isNotEmpty()) {
            val builder = ChangeLogBuilder(key, filteredCommits, context)
            builder.block()
            append(builder.build())
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

    fun groupBy(keySelector: (ChangeLogFormat.CommitInfo) -> String?, block: ChangeLogBuilder.() -> Unit) {
        processGroups(commitInfos().mapNotNull { keyMapper(keySelector, it) }
            .groupBy({ it.first }, { it.second }), block)
    }

    fun groupBySorted(
        keySelector: (ChangeLogFormat.CommitInfo) -> String?,
        block: ChangeLogBuilder.() -> Unit
    ) {
        processGroups(commitInfos().mapNotNull { keyMapper(keySelector, it) }
            .groupByTo(TreeMap(), { it.first }, { it.second }), block)
    }

    private fun keyMapper(
        keySelector: (ChangeLogFormat.CommitInfo) -> String?,
        it: ChangeLogFormat.CommitInfo
    ): Pair<String, ChangeLogFormat.CommitInfo>? {
        return (keySelector(it) ?: return null) to it
    }

    private fun processGroups(
        groupedByScope: Map<String, List<ChangeLogFormat.CommitInfo>>,
        block: ChangeLogBuilder.() -> Unit
    ) {
        for ((key, scopeCommits) in groupedByScope.filterKeys { it.isNotEmpty() }) {
            val builder = ChangeLogBuilder(key, scopeCommits, context)
            block(builder)
            append(builder.build())
        }
    }
    //</editor-fold>

    fun commitInfos() = commitInfos.filter { !context.isCommitFlagged(it) }
}

class ChangeLogTextFormatter(
    private val commitInfo: ChangeLogFormat.CommitInfo
) : DocumentBuilder() {
    fun header() = (commitInfo().message ?: commitInfo().text).lineSequence().first()

    fun body() = commitInfo().text.lineSequence()
        .drop(1)
        .dropWhile { it.isEmpty() }
        .takeWhile { it.isNotEmpty() }

    fun fullHeader() = commitInfo().text.lineSequence().first()

    fun scope(format: String = "%s: ") = commitInfo().scope?.let { format.format(it) }.orEmpty()
    fun type(format: String = "%s: ") = commitInfo().type?.let { format.format(it) }.orEmpty()

    fun hash(format: String = "%s", len: Int = 40) =
        commitInfo().commits.joinToString(" ", "", " ") { format.format(it.sha.take(len)) }

    fun commitInfo() = commitInfo
}