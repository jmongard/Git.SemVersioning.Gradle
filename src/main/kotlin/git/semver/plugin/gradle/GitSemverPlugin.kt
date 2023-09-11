package git.semver.plugin.gradle

import git.semver.plugin.scm.GitProvider
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * The plugin entry point
 */
class GitSemverPlugin: Plugin<Project> {
    companion object {
        const val VERSIONING_GROUP = "Versioning"
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
                val log = GitProvider(settings).getChangeLog(settings.gitDirectory)

                val groupedByPrefix = log.sorted().distinct().groupBy { it
                    .substringBefore("\n")
                    .substringBefore(":","?")
                    .substringBefore("(") }

                groupedByPrefix.filter { it.key != "?" }.forEach { (prefix, items) ->
                    println("# $prefix")
                    items.sorted().forEach { item ->
                        println(item.trim().lines()
                            .joinToString("\n    ", "  - "))
                    }
                }
            }
        }


        project.tasks.register("releaseVersion", ReleaseTask::class.java, settings)
    }
}
