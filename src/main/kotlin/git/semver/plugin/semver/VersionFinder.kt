package git.semver.plugin.semver

import git.semver.plugin.scm.Commit
import git.semver.plugin.scm.IRefInfo
import org.slf4j.LoggerFactory

class VersionFinder(private val settings: SemverSettings, private val tags: Map<String, List<IRefInfo>>) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val visitedCommits = mutableSetOf("")

    fun getVersion(commit: Commit, isDirty: Boolean, defaultPreRelease: String?): SemVersion {
        val semVersion = getSemVersion(commit) ?: SemVersion()
        val isModified = semVersion.isSnapshot || isDirty
        semVersion.applyPendingChanges(isModified)

        if (!semVersion.isPreRelease && isModified) {
            semVersion.setPreRelease(defaultPreRelease)
        }
        return semVersion
    }

    fun getReleaseVersion(commit: Commit, newPreRelease: String?): SemVersion {
        val semVersion = getSemVersion(commit) ?: SemVersion()
        semVersion.commitCount = 0
        semVersion.applyPendingChanges(!semVersion.isPreRelease || "" != newPreRelease)

        if (newPreRelease != null) {
            semVersion.setPreRelease(newPreRelease)
        }
        return semVersion
    }

    private fun getSemVersion(commit: Commit): SemVersion? {
        if (visitedCommits.contains(commit.sha)) {
            return null
        }
        visitedCommits.add(commit.sha)
        try {
            val version = if (SemVersion.isRelease(commit, settings))
                SemVersion.tryParse(commit)
            else
                tags[commit.sha]?.mapNotNull(SemVersion.Companion::tryParse)?.max()

            if (version != null && !version.isPreRelease) {
                logger.debug("Release version found: {}", version)
                return version
            }

            val parentSemVersions = commit.parents.mapNotNull(this::getSemVersion).toList()
            val parentVersion = parentSemVersions.max() ?: SemVersion()
            parentVersion.commitCount = parentSemVersions.map { it.commitCount }.sum();
            parentVersion.updateFromCommit(commit, settings, /* pre-release */ version)

            logger.debug("Version after commit(\"{}\"), pre-release({}): {}", commit, version, parentVersion)
            return parentVersion
        } catch (e: Exception) {
            logger.warn("Failed retrieving SemVer for commit: {}", commit.sha, e)
            return null
        }
    }
}