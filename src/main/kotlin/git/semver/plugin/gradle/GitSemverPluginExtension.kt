package git.semver.plugin.gradle

import git.semver.plugin.changelog.ChangeLogFormatter
import git.semver.plugin.changelog.ChangeLogSettings
import git.semver.plugin.scm.GitProvider
import git.semver.plugin.semver.SemverSettings
import org.gradle.api.Project
import java.io.File

open class GitSemverPluginExtension(project: Project) : SemverSettings() {
    var gitDirectory: File = project.projectDir
    var createReleaseCommit = true
    var createReleaseTag = true

    val semVersion by lazy { GitProvider(this).getSemVersion(gitDirectory) }
    val version by lazy { semVersion.toVersionString() }
    val infoVersion by lazy { semVersion.toInfoVersionString() }

    var changeLogSettings = ChangeLogSettings.defaultChangeLog
    val changeLogList by lazy { GitProvider(this).getChangeLog(gitDirectory) }
    val changeLog
        get() = ChangeLogFormatter(this, changeLogSettings).formatLog(changeLogList)

    init {
        val defaultPreReleaseProperty = project.findProperty("defaultPreRelease")
        if (defaultPreReleaseProperty is String) {
            defaultPreRelease = defaultPreReleaseProperty
        }
        val noDirtyCheckProperty = project.findProperty("noDirtyCheck")
        if (noDirtyCheckProperty is String) {
            noDirtyCheck = noDirtyCheckProperty.toBoolean()
        }
    }
}