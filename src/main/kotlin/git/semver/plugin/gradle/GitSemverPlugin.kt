package git.semver.plugin.gradle

import git.semver.plugin.scm.GitProvider
import git.semver.plugin.semver.SemverSettings
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * The plugin entry point
 */
class GitSemverPlugin: Plugin<Project> {
    companion object {
        const val VERSIONING_GROUP = "Versioning"

        internal fun formatLog(settings: SemverSettings, changeLog: List<String>): String {
            val log = changeLog.sorted().distinct()

            val builder = StringBuilder()
            settings.changeLogHeadings["#"]?.let {
                builder.appendLine("# $it")
            }

            log.filter { settings.majorRegex.containsMatchIn(it) }.takeIf { it.isNotEmpty() }?.let {
                formatLogItems(builder, settings.changeLogHeadings["!"], it)
            }

            val groupedByPrefix = log
                .mapNotNull { settings.changeLogRegex.find(it) }
                .groupBy({
                    it.groups["Type"]?.value?.let { it2 -> settings.changeLogHeadings[it2] }
                        ?: settings.changeLogHeadings["?"]
                }, {
                    (it.groups["Message"]?.value?.trim() ?: "") +
                            (it.groups["Scope"]?.value?.let { scope -> " ($scope)" } ?: "")
                })

            groupedByPrefix.forEach { (prefix, items) ->
                formatLogItems(builder, prefix, items)
            }
            return builder.toString()
        }

        private fun formatLogItems(
            sb: StringBuilder,
            prefix: String?,
            changeLog: List<String>
        ) {
            if (prefix.isNullOrEmpty()) {
                return;
            }
            sb.appendLine("\n## $prefix")
            changeLog.forEach { item ->
                sb.appendLine(
                    item.trim().lines()
                        .joinToString("\n    ", "  - ")
                )
            }
        }
    }

    override fun apply(project: Project) {
        val settings = project.extensions.create("semver", GitSemverPluginExtension::class.java, project)

        project.tasks.register("printVersion") { task ->
            task.group = VERSIONING_GROUP;
            task.description = "Prints the current project version";

            task.doLast {
                println(settings.version)
            }
        }

        project.tasks.register("printSemVersion") { task ->
            task.group = VERSIONING_GROUP;
            task.description = "Prints the current project semantic version";

            task.doLast {
                println(settings.semVersion)
            }
        }

        project.tasks.register("printInfoVersion") { task ->
            task.group = VERSIONING_GROUP;
            task.description = "Prints the current project info version";

            task.doLast {
                println(settings.infoVersion)
            }
        }

        project.tasks.register("printChangeLog") { task ->
            task.group = VERSIONING_GROUP;
            task.description = "Prints a change log";

            task.doLast {
                val changeLog = GitProvider(settings).getChangeLog(settings.gitDirectory)
                println(formatLog(settings, changeLog))
            }
        }

        project.tasks.register("releaseVersion", ReleaseTask::class.java, settings)
    }

}
