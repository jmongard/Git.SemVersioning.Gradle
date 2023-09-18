package git.semver.plugin.gradle

import git.semver.plugin.scm.Commit
import git.semver.plugin.semver.SemverSettings
import java.util.TreeMap

private const val SCOPE = "Scope"
private const val TYPE = "Type"
private const val MESSAGE = "Message"

class ChangeLogFormatter(private val settings: SemverSettings, private val changeLogTexts: MutableMap<String, String>) {
    companion object {
        const val HEADER = "#"
        const val FOOTER = "~"
        const val GROUP_START = "#^"
        const val GROUP_END = "#$"
        const val OTHER_TYPE = "*"
        const val MISSING_TYPE = "-"
        const val BREAKING_CHANGE = "!"
        const val CHANGE_PREFIX = "^"
        const val CHANGE_POSTFIX = "$"
        const val CHANGE_LINE_SEPARATOR = "\\"
    }

    private val prefix = changeLogTexts[CHANGE_PREFIX].orEmpty()
    private val separator = changeLogTexts[CHANGE_LINE_SEPARATOR].orEmpty()
    private val postfix = changeLogTexts[CHANGE_POSTFIX].orEmpty()
    private val breakingChange = changeLogTexts[BREAKING_CHANGE]
    private val missingType = changeLogTexts[MISSING_TYPE]
    private val otherType = changeLogTexts[OTHER_TYPE]

    internal fun formatLog(changeLog: List<Commit>): String {
        val groupedByHeading = TreeMap<String, MutableSet<String>>()
        changeLog.forEach { addChange(it.text, groupedByHeading) }

        val builder = StringBuilder()
        addText(builder, HEADER)

        groupedByHeading.forEach { (heading, items) ->
            addText(builder, GROUP_START)
            builder.appendLine().appendLine(heading)
            items.sorted().forEach { item ->
                builder.appendLine(item.trim().lines().joinToString(separator, prefix, postfix))
            }
            addText(builder, GROUP_END)
        }

        addText(builder, FOOTER)
        return builder.trim().toString()
    }

    private fun addChange(
        text: String,
        resultMap: MutableMap<String, MutableSet<String>>
    ) {
        fun addChange(category: String?, message: String, scope: String? = null) {
            if (!category.isNullOrEmpty()) {
                resultMap.computeIfAbsent(category) { mutableSetOf() }.add(
                    scope?.let { "$it: " }.orEmpty() + message.trim()
                )
            }
        }
        fun getValue(match: MatchResult, id: String) = match.groups[id]?.value

        if (settings.majorRegex.containsMatchIn(text)) {
            addChange(breakingChange, text)
            return
        }

        val match = settings.changeLogRegex.find(text)
        if (match == null) {
            addChange(missingType, text)
            return
        }

        val scope = getValue(match, SCOPE)
        val scopeHeading = scope?.let { changeLogTexts[it] }
        if (scopeHeading != null) {
            addChange(scopeHeading, text)
            return
        }

        val typeHeading = changeLogTexts[getValue(match, TYPE)]
        if (typeHeading != null) {
            val message = getValue(match, MESSAGE)!!
            addChange(typeHeading, message, scope)
            return
        }

        addChange(otherType, text)
    }

    private fun addText(builder: StringBuilder, text: String) {
        changeLogTexts[text]?.let {
            builder.appendLine(it)
        }
    }

}