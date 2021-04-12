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

    init {
        val defaultPreReleaseProperty = project.findProperty("defaultPreRelease")
        if (defaultPreReleaseProperty is String) {
            defaultPreRelease = defaultPreReleaseProperty
        }
        val noDirtyCheckProperty = project.findProperty("noDirtyCheck")
        if (noDirtyCheckProperty is String) {
            noDirtyCheck = noDirtyCheckProperty.toBoolean();
        }
    }
}