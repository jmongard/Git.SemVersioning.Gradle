package git.semver.plugin.changelog

import git.semver.plugin.scm.Commit
import git.semver.plugin.semver.SemverSettings
import java.util.TreeMap

private const val SCOPE = "Scope"
private const val TYPE = "Type"
private const val MESSAGE = "Message"

class ChangeLogFormatter(private val settings: SemverSettings, private val format: ChangeLogSettings) {

    private val changeLogRegex = format.changeLogPattern.toRegex(SemverSettings.REGEX_OPTIONS)

    internal fun formatLog(changeLog: List<Commit>): String {

        val groupedByHeading = TreeMap<String, MutableSet<String>>()
        changeLog.sortedBy { it.text }.groupBy { it.text }.forEach { addCommit(it, groupedByHeading) }

        val builder = StringBuilder()
        addStaticText(builder, format.header)

        groupedByHeading.forEach { (heading, items) ->
            addStaticText(builder, format.groupStart)
            builder.appendLine().appendLine(heading)
            items.forEach { item ->
                builder
                    .append(format.changePrefix)
                    .append(format.changeLineSeparator?.let { item.lineSequence().joinToString(it) }
                        ?: item.lineSequence().first())
                    .appendLine(format.changePostfix)
            }
            addStaticText(builder, format.groupEnd)
        }

        addStaticText(builder, format.footer)
        return builder.trim().toString()
    }

    private fun addCommit(
        commit: Map.Entry<String, List<Commit>>,
        resultMap: MutableMap<String, MutableSet<String>>
    ) {
        fun addChangeLogText(category: String?, message: String, scope: String? = null) {
            if (!category.isNullOrEmpty()) {
                resultMap.computeIfAbsent(category) { mutableSetOf() }.add(formatLine(scope, message, commit.value).trim())
            }
        }

        val text = commit.key
        if (format.breakingChangeHeader != null && settings.majorRegex.containsMatchIn(text)) {
            addChangeLogText(format.breakingChangeHeader, text)
            return
        }

        val match = changeLogRegex.find(text)
        if (match == null) {
            addChangeLogText(format.missingTypeHeader, text)
            return
        }

        val scope = match.groupValue(SCOPE)
        val scopeHeading = scope?.let { format.headerTexts[it] }
        if (scopeHeading != null) {
            addChangeLogText(scopeHeading, text)
            return
        }

        val typeHeading = format.headerTexts[match.groupValue(TYPE)]
        if (typeHeading != null) {
            val message = match.groupValue(MESSAGE)!!
            addChangeLogText(typeHeading, message, scope)
            return
        }

        addChangeLogText(format.otherChangeHeader, text)
    }

    private fun formatLine(scope: String?, message: String, commits: List<Commit>) = commits
        .map { it.sha.take(format.changeShaLength) }
        .filter { it.isNotEmpty() }
        .joinToString(" ", "", " ") +
            scope?.let { "$it: " }.orEmpty() +
            message

    private fun addStaticText(builder: StringBuilder, text: String?) =
        text?.let(builder::appendLine)

    private fun MatchResult.groupValue(groupId: String) = this.groups[groupId]?.value
}