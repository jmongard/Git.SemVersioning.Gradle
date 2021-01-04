package git.semver.plugin.gradle

import git.semver.plugin.scm.GitProvider
import git.semver.plugin.semver.SemverSettings
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import javax.inject.Inject


open class ReleaseTask @Inject constructor(private val settings: SemverSettings) : DefaultTask() {
    private var preRelease: String? = null
    private var message: String? = null
    private var tag = true
    private var commit = true

    init {
        group = GitSemverPlugin.VERSIONING_GROUP
        description = "Creates a release commit"
    }

    @Option(option = "preRelease", description = "Set the current preRelease")
    fun setPreRelease(preRelease: String) {
        this.preRelease = preRelease
    }

    @Option(option = "message", description = "Set a message for the release")
    fun setMessage(message: String) {
        this.message = message
    }

    @Option(option = "no-commit", description = "Don't create a release commit")
    fun setNoCommit(noCommit: Boolean) {
        this.commit = !noCommit
    }

    @Option(option = "no-tag", description = "Don't create a release tag")
    fun setNoTag(noTag: Boolean) {
        this.tag = !noTag
    }

    @TaskAction
    fun createRelease() {
        GitProvider(settings).createRelease(project.projectDir, tag, commit, preRelease, message)
    }
}