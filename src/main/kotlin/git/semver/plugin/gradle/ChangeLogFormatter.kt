package git.semver.plugin.gradle

import git.semver.plugin.semver.SemverSettings
import java.util.TreeMap

class ChangeLogFormatter(private val settings: SemverSettings) {
    companion object {
        const val HEADER = "#"
        const val FOOTER = "~"
        const val OTHER_TYPE = "*"
        const val MISSING_TYPE = "-"
        const val BREAKING_CHANGE = "!"
        const val CHANGE_PREFIX = "^"
        const val CHANGE_POSTFIX = "$"
        const val CHANGE_LINE_SEPARATOR = "\\"
    }

    private val prefix = settings.changeLogTexts[CHANGE_PREFIX].orEmpty()
    private val separator = settings.changeLogTexts[CHANGE_LINE_SEPARATOR].orEmpty()
    private val postfix = settings.changeLogTexts[CHANGE_POSTFIX].orEmpty()
    private val breakingChange = settings.changeLogTexts[BREAKING_CHANGE]
    private val missingType = settings.changeLogTexts[MISSING_TYPE]
    private val otherType = settings.changeLogTexts[OTHER_TYPE]

    internal fun formatLog(changeLog: List<String>): String {
        val groupedByHeading = TreeMap<String, MutableSet<String>>()
        changeLog.forEach { addChange(it, groupedByHeading) }

        val builder = StringBuilder()
        addText(builder, HEADER)

        groupedByHeading.forEach { (heading, items) ->
            builder.appendLine(heading)
            items.sorted().forEach { item ->
                builder.appendLine(item.trim().lines().joinToString(separator, prefix, postfix))
            }
            builder.appendLine()
        }

        addText(builder, FOOTER)
        return builder.trim().toString()
    }

    private fun addChange(
        text: String,
        resultMap: MutableMap<String, MutableSet<String>>
    ) {
        fun addChange(category: String?, message: String) {
            if (!category.isNullOrEmpty()) {
                resultMap.computeIfAbsent(category) { mutableSetOf() }.add(message)
            }
        }
        fun getValue(match: MatchResult, id: String) = match.groups[id]?.value

        if (settings.majorRegex.containsMatchIn(text)) {
            addChange(breakingChange, text)
            return
        }

        val match = settings.changeLogRegex.find(text);
        if (match == null) {
            addChange(missingType, text)
            return
        }

        val scope = getValue(match, "Scope")
        val scopeHeading = scope?.let { settings.changeLogTexts[it] }
        if (scopeHeading != null) {
            addChange(scopeHeading, text)
            return
        }

        val typeHeading = getValue(match, "Type")?.let { settings.changeLogTexts[it] }
        if (typeHeading != null) {
            val messageText = getValue(match, "Message")
            val scopeText = scope?.let { s -> "$s: " }.orEmpty()
            addChange(typeHeading, scopeText + (messageText ?: text).trim())
            return
        }

        addChange(otherType, text)
    }

    private fun addText(builder: StringBuilder, text: String) {
        settings.changeLogTexts[text]?.let {
            builder.appendLine(it).appendLine()
        }
    }

}