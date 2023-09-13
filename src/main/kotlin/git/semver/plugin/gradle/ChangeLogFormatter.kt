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
        val log = changeLog.sorted().distinct()

        val builder = StringBuilder()
        settings.changeLogHeadings[HEADING]?.let {
            builder.appendLine(it)
        }

        val groupedByHeading = mutableMapOf<String, MutableList<String>>()
        log.forEach { addChange(it, groupedByHeading) }
        groupedByHeading.toSortedMap().forEach { (prefix, items) ->
            formatLogItems(builder, prefix, items)
        }

        return builder.toString()
    }

    private fun addChange(
        it: String,
        groupedByHeading: MutableMap<String, MutableList<String>>
    ) {
        if (settings.majorRegex.containsMatchIn(it)) {
            add(groupedByHeading, settings.changeLogHeadings[BREAKING], it)
            return
        }

        val match = settings.changeLogRegex.find(it);
        if (match != null) {
            val scope = getHeading(match.groups["Scope"])
            if (scope != null) {
                add(groupedByHeading, scope, it)
                return
            }

            val type = getHeading(match.groups["Type"])
            if (type != null) {
                add(groupedByHeading, type,
                    (match.groups["Scope"]?.value?.let { s -> "$s: " } ?: "") +
                            (match.groups["Message"]?.value?.trim() ?: it))
                return
            }

            add(groupedByHeading, settings.changeLogHeadings[OTHER], it)
            return
        }

        add(groupedByHeading, settings.changeLogHeadings[NO_TYPE], it)
    }

    private fun add(
        groupedByHeading: MutableMap<String, MutableList<String>>,
        category: String?,
        message: String
    ) {
        category?.let {
            groupedByHeading.computeIfAbsent(it) { mutableListOf() }.add(message)
        }
    }

    private fun getHeading(matchGroup: MatchGroup?) = matchGroup?.value?.let { settings.changeLogHeadings[it] }

    private fun formatLogItems(
        sb: StringBuilder,
        heading: String,
        changeLog: List<String>
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