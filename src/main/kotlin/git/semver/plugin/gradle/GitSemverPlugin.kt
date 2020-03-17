package git.semver.plugin.gradle

import git.semver.plugin.gradle.scm.GitProvider
import git.semver.plugin.gradle.semver.SemVersion
import git.semver.plugin.gradle.semver.SemverSettings
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The plugin entry point
 */
class GitSemverPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val settings = project.extensions.create("semver", GitSemverPluginExtension::class.java, project)

        project.tasks.register("printVersion") { task ->
            task.doLast {
                println("--------------------")
                println("Version: ${settings.semVersion}")
            }
        }
    }

}
