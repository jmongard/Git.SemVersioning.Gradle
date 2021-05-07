package git.semver.plugin.semver

import git.semver.plugin.scm.Commit
import git.semver.plugin.scm.IRefInfo
import org.slf4j.LoggerFactory
import java.util.ArrayDeque

class VersionFinder(private val settings: SemverSettings, private val tags: Map<String, List<IRefInfo>>) {
    private val logger = LoggerFactory.getLogger(javaClass)

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

    private fun getSemVersion(startCommit: Commit): SemVersion? {
        if (startCommit.sha.isBlank()) {
            return null
        }
        val versions = mutableMapOf<String, SemVersion?>()
        val commits = ArrayDeque<Pair<Commit, MutableList<String>>>()
        commits.push(startCommit to ArrayList(1))

        while (!commits.isEmpty()) {
            val peek = commits.peek()
            val commit = peek.first
            val parentList = peek.second

            if (!versions.containsKey(commit.sha)) {
                val version = getSemVersionFromCommit(commit)
                versions[commit.sha] = version
                if (version != null && !version.isPreRelease) {
                    logger.debug("Release version found: {}", version)
                    commits.pop()
                } else {
                    commit.parents.forEach {
                        parentList.add(it.sha)
                        commits.push(it to ArrayList(1))
                    }
                }
            } else {
                val parentSemVersions = parentList.mapNotNull { versions.put(it, null) }.toList()
                versions[commit.sha] = getMaxVersionFromParents(parentSemVersions, commit, versions[commit.sha])
                commits.pop();
            }
        }
        return versions[startCommit.sha]
    }
    
    private fun getMaxVersionFromParents(
        parentSemVersions: List<SemVersion>,
        commit: Commit,
        preReleaseFromCommit: SemVersion?
    ): SemVersion {
        val version = parentSemVersions.max() ?: SemVersion()
        version.commitCount = parentSemVersions.map { it.commitCount }.sum();
        version.updateFromCommit(commit, settings, preReleaseFromCommit)

        logger.debug("Version after commit(\"{}\"), pre-release({}): {}", commit, preReleaseFromCommit, version)
        return version
    }

    private fun getSemVersionFromCommit(commit: Commit): SemVersion? {
        return if (SemVersion.isRelease(commit, settings))
            SemVersion.tryParse(commit)
        else
            tags[commit.sha]?.mapNotNull(SemVersion.Companion::tryParse)?.max()
    }
}