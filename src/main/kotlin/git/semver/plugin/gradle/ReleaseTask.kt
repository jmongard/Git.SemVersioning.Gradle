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

    @Option(option = "commit", description = "Create a release commit (default: true)")
    fun setCommit(commit: Boolean) {
        this.commit = commit
    }

    @Option(option = "tag", description = "Create a release tag (default: true)")
    fun setTag(tag: Boolean) {
        this.tag = tag
    }

    @TaskAction
    fun createRelease() {
        GitProvider(settings).createRelease(project.projectDir, tag, commit, preRelease, message)
    }
}