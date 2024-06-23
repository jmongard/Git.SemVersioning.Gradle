package git.semver.plugin.gradle

import git.semver.plugin.changelog.*
import git.semver.plugin.semver.BaseSettings
import git.semver.plugin.semver.SemInfoVersion
import git.semver.plugin.semver.SemVersion
import git.semver.plugin.semver.SemverSettings
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Internal


abstract class GitSemverPluginExtension(project: Project, providerFactory: ProviderFactory) : BaseSettings() {
    private val defaultPreReleaseProperty = project.findProperty("defaultPreRelease")
    private val noDirtyCheckProperty = project.findProperty("noDirtyCheck")
    private val projectDir = project.layout.projectDirectory;

    /**
     * Directory where to find the git project. If the git directory is a parent of this directory it will be found.
     * Default: project dir.
     */
    abstract val gitDirectory: DirectoryProperty

    /**
     * Should a release commit be created when running the release task.
     * This parameter can be overridden by command line argument to the release task.
     * Default: true.
     */
    var createReleaseCommit = true

    /**
     * Should a release tag be created when running the release task.
     * This parameter can be overridden by command line argument to the release task.
     * Default: true.
     */
    var createReleaseTag = true

    /**
     * Format used when producing change log. Used to configure any predefined logging format.
     * <ul>
     *   <li>{@link git.semver.plugin.changelog.ChangeLogFormat#defaultChangeLog defaultChangeLog}</li>
     *   <li>{@link git.semver.plugin.changelog.ChangeLogFormat#simpleChangeLog simpleChangeLog}</li>
     *   <li>{@link git.semver.plugin.changelog.ChangeLogFormat#scopeChangeLog scopeChangeLog}</li>
     * </ul>
     * The format can also be set using a builder.
     */
    var changeLogFormat = ChangeLogFormat.defaultChangeLog

    /**
     * Configure change log format using builder.
     *
     * semver {
     *   changeLogFormat {
     *     appendLine(constants.header).appendLine()
     *     withType("test") {
     *       appendLine("## Test")
     *       formatChanges {
     *         appendLine("- ${'$'}{scope()}${'$'}{header()}")
     *       }
     *       appendLine()
     *     }
     *   }
     * }
     */
    fun changeLogFormat(builder: ChangeLogBuilder.() -> Unit) {
        changeLogFormat = ChangeLogFormatter(builder = builder)
    }

    /**
     * Texts used when producing change log. Used to configure any predefined text format.
     * <ul>
     *   <li>{@link git.semver.plugin.changelog.DefaultChangeLogTexts}</li>
     *   <li>{@link git.semver.plugin.changelog.PlainChangeLogTexts}</li>
     * </ul>
     * The text can also be set using a builder.
     */
    var changeLogTexts: ChangeLogTexts = DefaultChangeLogTexts

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

    /**
     * The semantic version for the project with commit info e.g. 1.2.3-Alpha.4+005.sha.7f8c962
     */
    val semVersion: SemInfoVersion
        get() = semInfoVersionValueSource.get()

    /**
     * The semantic version for the project with commit info excluding sha as a string e.g. "1.2.3-Alpha.4+005"
     */
    val infoVersion: String by lazy { semVersion.toInfoVersionString() }

    /**
     * The semantic version for the project e.g. 1.2.3-Alpha.4
     */
    val versionValue: SemVersion
        get() = semVersionValueSource.get()

    /**
     * The semantic version for the project as a string e.g. "1.2.3-Alpha.4"
     */
    val version: String by lazy { versionValue.toString() }

    private var semInfoVersionValueSource = project.providers.of(SemInfoVersionValueSource::class.java) {
        it.parameters.getGitDir().set(gitDir);
        it.parameters.getSettings().set(providerFactory.provider {
            createSettings()
        })
    }
    private var semVersionValueSource = project.providers.of(SemVersionValueSource::class.java) {
        it.parameters.getGitDir().set(gitDir);
        it.parameters.getSettings().set(providerFactory.provider {
            createSettings()
        })
    }

    @get:Internal
    internal val gitDir: Provider<Directory>
        get() = gitDirectory.orElse(projectDir)

    internal fun createSettings(): SemverSettings {
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