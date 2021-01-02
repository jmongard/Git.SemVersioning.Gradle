package git.semver.plugin.gradle

import git.semver.plugin.scm.GitProvider
import git.semver.plugin.semver.SemverSettings
import org.gradle.api.Project

open class GitSemverPluginExtension(project: Project) : SemverSettings() {
    val semVersion by lazy { GitProvider(this).getSemVersion(project.projectDir) }
    val version by lazy { semVersion.toInfoVersionString() }
}