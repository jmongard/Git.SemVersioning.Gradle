package git.semver.plugin.gradle.semver

import git.semver.plugin.gradle.scm.Commit
import git.semver.plugin.gradle.scm.IRefInfo
import org.slf4j.LoggerFactory

class VersionFinder(private val settings: SemverSettings, private val tags: Map<String, List<IRefInfo>>) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val visitedCommits = mutableSetOf<String>()

    fun getVersion(commit: Commit, isDirty: Boolean): SemVersion {
        val semVersion = getSemVersion(commit) ?: SemVersion()
        semVersion.calculateNewVersion(isDirty, settings.defaultPreRelease)
        return semVersion
    }

    private fun getSemVersion(commit: Commit): SemVersion? {
        if (visitedCommits.contains(commit.sha)) {
            return null;
        }
        visitedCommits.add(commit.sha);
        try {
            val version = if (SemVersion.isRelease(commit, settings))
                SemVersion.tryParse(commit)
            else
                tags[commit.sha]?.mapNotNull(SemVersion.Companion::tryParse)?.max()

            if (version != null && !version.isPreRelease) {
                logger.debug("Stopping search at version: {}", version)
                return version
            }

            val parentVersion = commit.parents.mapNotNull(this::getSemVersion).max() ?: SemVersion()
            parentVersion.updateFromCommit(commit, settings, version)
            return parentVersion
        } catch (e: Exception) {
            logger.warn("Failed retrieving SemVer for commit: {}", commit.sha, e)
            return null
        }
    }
}