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

        project.tasks.register("printVersion", PrintTask::class.java, settings::version, "Prints the current project version")
        project.tasks.register("printSemVersion", PrintTask::class.java, settings::semVersion, "Prints the current project semantic version")
        project.tasks.register("printInfoVersion", PrintTask::class.java, settings::infoVersion, "Prints the current project info version")
        project.tasks.register("printChangeLog", PrintTask::class.java, settings::changeLog, "Prints a change log")
        project.tasks.register("releaseVersion", ReleaseTask::class.java, settings)
    }

}
