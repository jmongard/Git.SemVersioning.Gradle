package git.semver.plugin.gradle

import git.semver.plugin.gradle.scm.GitProvider
import git.semver.plugin.gradle.semver.SemverSettings
import org.gradle.api.Project

open class GitSemverPluginExtension(project: Project) : SemverSettings() {
    val semVersion by lazy { GitProvider(this).GetSemVersion(project.projectDir) }
    val version by lazy { semVersion.toInfoVersionString() }
}