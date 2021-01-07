package git.semver.plugin.gradle

import git.semver.plugin.scm.GitProvider
import git.semver.plugin.semver.SemverSettings
import org.gradle.api.Project
import java.io.File

open class GitSemverPluginExtension(project: Project) : SemverSettings() {
    var gitDirectory: File = project.projectDir
    val semVersion by lazy { GitProvider(this).getSemVersion(gitDirectory) }
    val version by lazy { semVersion.toVersionString() }
    val infoVersion by lazy { semVersion.toInfoVersionString() }
}