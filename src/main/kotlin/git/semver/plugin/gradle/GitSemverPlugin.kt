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
        val extension = project.extensions.create("semver", GitSemverPluginExtension::class.java, project)

        project.tasks.register("printVersion", PrintTask::class.java, extension::semVersion, "Prints the current project version")
        project.tasks.register("printSemVersion", PrintTask::class.java, extension::semInfoVersion, "Prints the current project semantic version")
        project.tasks.register("printInfoVersion", PrintTask::class.java, extension::infoVersion, "Prints the current project info version")

        if (project == project.rootProject) {
            project.tasks.register("printChangeLog", PrintTask::class.java, {
                val settings = extension.createSettings()
                val changeLog = GitProvider(settings).getChangeLog(extension.gitDirectory)

                extension.changeLogFormat.formatLog(
                    changeLog,
                    settings,
                    extension.changeLogTexts
                )
            }, "Prints a change log")

            project.tasks.register("releaseVersion", ReleaseTask::class.java, extension)
        }
    }

}
