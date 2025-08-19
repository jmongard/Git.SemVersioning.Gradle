package git.semver.plugin.gradle

import git.semver.plugin.scm.GitProvider
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import javax.inject.Inject


open class ReleaseTask @Inject constructor(private val settings: GitSemverPluginExtension) : DefaultTask() {
    private var preRelease: String? = null
    private var message: String? = null
    private var tag = settings.createReleaseTag
    private var commit = settings.createReleaseCommit
    private var noDirtyCheck = false

    init {
        group = GitSemverPlugin.VERSIONING_GROUP
        description = "Creates a release commit and / or local tag"
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

    @Option(option = "commit", description = "Create a release commit even if it is disabled in the settings")
    fun setCommit(commit: Boolean) {
        this.commit = commit
    }

    @Option(option = "no-tag", description = "Don't create a release tag")
    fun setNoTag(noTag: Boolean) {
        this.tag = !noTag
    }

    @Option(option = "tag", description = "Create a release tag even if it is disabled in the settings")
    fun setTag(tag: Boolean) {
        this.tag = tag
    }

    @Option(option = "no-dirty", description = "Don't check if repository is dirty")
    fun setNoDirty(noDirtyCheck: Boolean) {
        this.noDirtyCheck = noDirtyCheck
    }

    @TaskAction
    fun createRelease() {
        GitProvider(settings.createSettings()).createRelease(settings.gitDirectory.get().asFile, getReleaseParams())
    }

    @Internal
    internal fun getReleaseParams() = GitProvider.ReleaseParams(tag, commit, preRelease, message, noDirtyCheck)
}