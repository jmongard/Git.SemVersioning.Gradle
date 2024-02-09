package git.semver.plugin.gradle

import git.semver.plugin.scm.GitProvider
import git.semver.plugin.semver.SemInfoVersion
import org.gradle.api.provider.ValueSource

internal abstract class SemInfoVersionValueSource : ValueSource<SemInfoVersion, SemVersionValueSourceSettings> {

    override fun obtain(): SemInfoVersion {
        val gitProvider = GitProvider(parameters.getSettings().get())
        return gitProvider.getSemVersion(parameters.getGitDir().asFile.get())
    }
}