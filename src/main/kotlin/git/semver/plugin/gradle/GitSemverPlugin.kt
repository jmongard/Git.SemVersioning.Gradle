package git.semver.plugin.gradle

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
                println("--------------------")
                println("Version: ${settings.semVersion}")
            }
        }

        project.tasks.register("releaseVersion", ReleaseTask::class.java, settings)
    }
}
