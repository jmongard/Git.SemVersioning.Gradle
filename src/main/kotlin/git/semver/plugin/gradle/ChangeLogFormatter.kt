package git.semver.plugin.gradle

import git.semver.plugin.semver.SemverSettings


class ChangeLogFormatter(val settings: SemverSettings) {
    companion object {
        const val HEADING = "#"
        const val OTHER = "?"
        const val BREAKING = "!"
    }

    internal fun formatLog(changeLog: List<String>): String {
        val log = changeLog.sorted().distinct()

        val builder = StringBuilder()
        settings.changeLogHeadings[HEADING]?.let {
            builder.appendLine(it)
        }

        log.filter { settings.majorRegex.containsMatchIn(it) }.takeIf { it.isNotEmpty() }?.let {
            formatLogItems(builder, settings.changeLogHeadings[BREAKING], it)
        }

        val otherHeading = settings.changeLogHeadings[OTHER]
        val groupedByHeading = log
            .mapNotNull { settings.changeLogRegex.find(it) }
            .groupBy({
                getHeading(it.groups["Scope"])
                    ?: getHeading(it.groups["Type"])
                    ?: otherHeading
            }, {
                if (getHeading(it.groups["Scope"]) != null || getHeading(it.groups["Type"]) == null)
                    it.value.trim()
                else
                    (it.groups["Scope"]?.value?.let { scope -> "$scope: " } ?: "") +
                            (it.groups["Message"]?.value?.trim() ?: it.value)
            })

        groupedByHeading.forEach { (prefix, items) ->
            formatLogItems(builder, prefix, items)
        }

        return builder.toString()
    }

    private fun getHeading(matchGroup: MatchGroup?) = matchGroup?.value?.let { settings.changeLogHeadings[it] }

    private fun formatLogItems(
        sb: StringBuilder,
        heading: String?,
        changeLog: List<String>
    ) {
        if (heading.isNullOrEmpty()) {
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