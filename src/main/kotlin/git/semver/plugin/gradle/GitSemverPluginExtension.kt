package git.semver.plugin.gradle

import git.semver.plugin.changelog.*
import git.semver.plugin.semver.SemInfoVersion
import git.semver.plugin.semver.SemverSettings
import git.semver.plugin.semver.SemVersion
import org.gradle.api.Project
import java.io.File

abstract class GitSemverPluginExtension(project: Project) :SemverSettings() {
    private val defaultPreReleaseProperty = project.findProperty("defaultPreRelease")
    private val noDirtyCheckProperty = project.findProperty("noDirtyCheck")

    var gitDirectory: File = project.projectDir
    var createReleaseCommit = true
    var createReleaseTag = true
    var changeLogFormat = ChangeLogFormat.defaultChangeLog
    var changeLogTexts : ChangeLogTexts = DefaultChangeLogTexts

    /**
     * Configure change log format using builder.
     *
     * semver {
     *   changeLogFormat {
     *     appendLine(constants.header).appendLine()
     *     withType("test") {
     *     appendLine("## Test")
     *     formatChanges {
     *         appendLine("- ${'$'}{scope()}${'$'}{header()}")
     *     }
     *     appendLine()
     *   }
     * }
     */
    fun changeLogFormat(builder: ChangeLogBuilder.() -> Unit) {
        changeLogFormat = ChangeLogFormatter(builder = builder)
    }

    /**
     * Configure change log texts using builder.
     *
     * changeLogTexts {
     *   header = "# Test changelog"
     * }
     */
    fun changeLogTexts(builder: ChangeLogTexts.() -> Unit) {
        builder(changeLogTexts)
    }

    private var semInfoVersionValueSource = project.providers.of(SemInfoVersionValueSource::class.java) {
        it.parameters.getGitDir().set(gitDirectory);
        it.parameters.getSettings().set(createSettings())
    }
    val semInfoVersion: SemInfoVersion by lazy { semInfoVersionValueSource.get() }
    val infoVersion by lazy { semInfoVersion.toInfoVersionString() }

    private var semVersionValueSource = project.providers.of(SemVersionValueSource::class.java) {
        it.parameters.getGitDir().set(gitDirectory);
        it.parameters.getSettings().set(createSettings())
    }
    val semVersion: SemVersion by lazy { semVersionValueSource.get() }
    val version by lazy { semVersion.toString() }

    internal fun createSettings() : SemverSettings{
        return SemverSettings(this).apply {
            if (defaultPreReleaseProperty is String) {
                defaultPreRelease = defaultPreReleaseProperty
            }
            if (noDirtyCheckProperty is String) {
                noDirtyCheck = noDirtyCheckProperty.toBoolean()
            }
        }
    }
}