package git.semver.plugin.gradle

import git.semver.plugin.semver.SemverSettings


class ChangeLogFormatter(private val settings: SemverSettings) {
    companion object {
        const val HEADING = "#"
        const val OTHER = "*"
        const val NO_TYPE = "-"
        const val BREAKING = "!"
    }

    internal fun formatLog(changeLog: List<String>): String {

        val builder = StringBuilder()
        settings.changeLogHeadings[HEADING]?.let {
            builder.appendLine(it)
        }

        val groupedByHeading = mutableMapOf<String, MutableSet<String>>()
        changeLog.forEach { addChange(it, groupedByHeading) }
        groupedByHeading.toSortedMap().forEach { (prefix, items) ->
            formatLogItems(builder, prefix, items)
        }

        return builder.toString()
    }

    private fun addChange(
        it: String,
        groupedByHeading: MutableMap<String, MutableSet<String>>
    ) {
        if (settings.majorRegex.containsMatchIn(it)) {
            add(groupedByHeading, settings.changeLogHeadings[BREAKING], it)
            return
        }

        val match = settings.changeLogRegex.find(it);
        if (match != null) {
            val scope = matchValue(match, "Scope")?.let { settings.changeLogHeadings[it] }
            if (scope != null) {
                add(groupedByHeading, scope, it)
                return
            }

            val type = matchValue(match, "Type")?.let { settings.changeLogHeadings[it] }
            if (type != null) {
                add(groupedByHeading, type,
                    (matchValue(match, "Scope")?.let { s -> "$s: " } ?: "") +
                            (matchValue(match, "Message")?.trim() ?: it))
                return
            }

            add(groupedByHeading, settings.changeLogHeadings[OTHER], it)
            return
        }

        add(groupedByHeading, settings.changeLogHeadings[NO_TYPE], it)
    }

    private fun matchValue(match: MatchResult, id: String) = match.groups[id]?.value

    private fun add(
        groupedByHeading: MutableMap<String, MutableSet<String>>,
        category: String?,
        message: String
    ) {
        category?.let {
            groupedByHeading.computeIfAbsent(it) { mutableSetOf() }.add(message)
        }
    }

    private fun formatLogItems(
        sb: StringBuilder,
        heading: String,
        changeLog: Set<String>
    ) {
        if (heading.isEmpty()) {
            return;
        }
        sb.appendLine().appendLine(heading)
        changeLog.sorted().forEach { item ->
            sb.appendLine(
                item.trim().lines()
                    .joinToString("\n    ", "  - ")
            )
        }
    }
}