package git.semver.plugin.gradle

import git.semver.plugin.scm.GitProvider
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * The plugin entry point
 */
class GitSemverPlugin : Plugin<Project> {
    companion object {
        const val VERSIONING_GROUP = "Versioning"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create("semver", GitSemverPluginExtension::class.java, project)

        project.tasks.register(
            "printVersion", PrintTask::class.java, {
                extension.versionValue.toString(extension.useTwoDigitVersion)
            }, "Prints the current project version", ""
        )
        project.tasks.register(
            "printSemVersion", PrintTask::class.java, {
                extension.semVersion.toInfoVersionString(
                    shaLength = 7,
                    useTwoDigitVersion = extension.useTwoDigitVersion,
                    metaSeparator = extension.metaSeparator
                )
            }, "Prints the current project semantic version", ""
        )
        project.tasks.register(
            "printInfoVersion", PrintTask::class.java, {
                extension.semVersion.toInfoVersionString(
                    metaSeparator = extension.metaSeparator,
                    useTwoDigitVersion = extension.useTwoDigitVersion
                )
            }, "Prints the current project info version", ""
        )

        if (project == project.rootProject) {
            project.tasks.register("printChangeLog", PrintTask::class.java, {
                val settings = extension.createSettings()
                val changeLog = GitProvider(settings).getChangeLog(extension.gitDirectory.get().asFile)

                extension.changeLogFormat.formatLog(
                    changeLog,
                    settings,
                    extension.changeLogTexts
                )
            }, "Prints a change log", "Not serializable configuration")

            project.tasks.register("releaseVersion", ReleaseTask::class.java, extension)
        }
    }

}
