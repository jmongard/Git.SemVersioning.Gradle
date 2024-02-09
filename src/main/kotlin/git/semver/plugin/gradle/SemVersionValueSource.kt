package git.semver.plugin.gradle

import git.semver.plugin.scm.GitProvider
import git.semver.plugin.semver.SemVersion
import org.gradle.api.provider.ValueSource

internal abstract class SemVersionValueSource : ValueSource<SemVersion, SemVersionValueSourceSettings> {
    override fun obtain(): SemVersion {
        val gitProvider = GitProvider(parameters.getSettings().get())
        val semInfoVersion = gitProvider.getSemVersion(parameters.getGitDir().asFile.get())
        return semInfoVersion.toSemVersion();
    }
}