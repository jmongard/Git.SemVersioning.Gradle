package git.semver.plugin.gradle

import git.semver.plugin.semver.SemverSettings


class ChangeLogFormatter {
    companion object {
        const val HEADING = "#"
        const val OTHER = "?"
        const val BREAKING = "!"

        internal fun formatLog(settings: SemverSettings, changeLog: List<String>): String {
            val log = changeLog.sorted().distinct()

            val builder = StringBuilder()
            settings.changeLogHeadings[HEADING]?.let {
                builder.appendLine(it)
            }

            val breakingChangeHeading = settings.changeLogHeadings.getOrDefault(BREAKING, "")
            log.filter { settings.majorRegex.containsMatchIn(it) }.takeIf { it.isNotEmpty() }?.let {
                formatLogItems(builder, breakingChangeHeading, it)
            }

            val otherHeading = settings.changeLogHeadings.getOrDefault(OTHER, "")
            val groupedByHeading = log
                .mapNotNull { settings.changeLogRegex.find(it) }
                .groupBy({
                    it.groups["Type"]?.value?.let { type -> settings.changeLogHeadings[type] } ?: otherHeading
                }, {
                    (it.groups["Scope"]?.value?.let { scope -> "$scope: " } ?: "") +
                    (it.groups["Message"]?.value?.trim() ?: "")
                })

            groupedByHeading.forEach { (prefix, items) ->
                formatLogItems(builder, prefix, items)
            }

            return builder.toString()
        }

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
}