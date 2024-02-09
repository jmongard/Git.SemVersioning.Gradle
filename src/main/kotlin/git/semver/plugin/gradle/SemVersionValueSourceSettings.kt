package git.semver.plugin.gradle

import git.semver.plugin.semver.SemverSettings
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSourceParameters

internal interface SemVersionValueSourceSettings : ValueSourceParameters {
    fun getGitDir(): DirectoryProperty
    fun getSettings(): Property<SemverSettings>
}