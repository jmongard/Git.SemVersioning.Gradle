package git.semver.plugin.semver

import git.semver.plugin.scm.Commit
import git.semver.plugin.scm.IRefInfo
import org.slf4j.LoggerFactory
import java.util.ArrayDeque

class VersionFinder(private val settings: SemverSettings, private val tags: Map<String, List<IRefInfo>>) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getVersion(commit: Commit, isDirty: Boolean, defaultPreRelease: String?): SemVersion {
        val semVersion = getSemVersion(commit)
        val isModified = semVersion.commitCount > 0 || isDirty
        val updated = semVersion.applyPendingChanges(isModified && !settings.noAutoBump, settings.groupVersionIncrements)

        if (!semVersion.isPreRelease && updated) {
            semVersion.setPreRelease(defaultPreRelease)
        }
        return semVersion
    }

    fun getReleaseVersion(commit: Commit, newPreRelease: String?): SemVersion {
        val semVersion = getSemVersion(commit)
        semVersion.commitCount = 0
        semVersion.applyPendingChanges(!semVersion.isPreRelease || "" != newPreRelease, settings.groupVersionIncrements)

        if (newPreRelease != null) {
            semVersion.setPreRelease(newPreRelease)
        }
        return semVersion
    }

    private fun getSemVersion(startCommit: Commit): SemVersion {
        if (startCommit.sha.isBlank()) {
            return versionZero()
        }
        val visitedCommitsVersionMap = mutableMapOf<String, SemVersion?>()
        val commits = ArrayDeque<Pair<Commit, MutableList<String>>>()
        commits.push(startCommit to ArrayList(1))

        // This code is a recursive algoritm rewritten as iterative to avoid stack overflow exception.
        // Unfortunately that makes it hard to understand.
        while (!commits.isEmpty()) {
            val peek = commits.peek()
            val currentCommit = peek.first
            val currentCommitParentSHAs = peek.second

            if (!visitedCommitsVersionMap.containsKey(currentCommit.sha)) {
                // unvisited commit
                val version = getSemVersionFromCommit(currentCommit)
                visitedCommitsVersionMap[currentCommit.sha] = version
                if (version != null && !version.isPreRelease) {
                    logger.debug("Release version found: {}", version)
                    // return to previous commit
                    commits.pop()
                } else {
                    currentCommit.parents.forEach {
                        currentCommitParentSHAs.add(it.sha)
                        if (!visitedCommitsVersionMap.containsKey(it.sha)) {
                            // prepare to visit parent commit
                            commits.push(it to ArrayList(1))
                        }
                    }
                }
            } else {
                // returning from parent commit
                val parentSemVersions = currentCommitParentSHAs.mapNotNull { getParentSemVersion(visitedCommitsVersionMap, it) }.toList()
                val previouslyVisitedPreRelease = visitedCommitsVersionMap[currentCommit.sha]
                visitedCommitsVersionMap[currentCommit.sha] = getMaxVersionFromParents(parentSemVersions, currentCommit, previouslyVisitedPreRelease)
                // return to previous commit
                commits.pop()
            }
        }
        return visitedCommitsVersionMap[startCommit.sha]!!
    }

    private fun getParentSemVersion(
        versionMap: MutableMap<String, SemVersion?>,
        sha: String
    ): SemVersion? {
        val parentSemVersion = versionMap[sha]
        if (parentSemVersion != null) {
            // parentSemVersion will be consumed in next step. Store a copy of it for future reference.
            versionMap[sha] = SemVersion(parentSemVersion)
        }
        return parentSemVersion
    }

    private fun getMaxVersionFromParents(
        parentSemVersions: List<SemVersion>,
        commit: Commit,
        previouslyVisitedPreRelease: SemVersion?
    ): SemVersion {
        val version = parentSemVersions.maxOrNull() ?: versionZero()
        version.mergeChanges(parentSemVersions)
        version.updateFromCommit(commit, settings, previouslyVisitedPreRelease)

        logger.debug("Version after commit(\"{}\"), (given version {}): {}", commit, previouslyVisitedPreRelease, version)
        return version
    }

    private fun versionZero() = SemVersion()

    private fun getSemVersionFromCommit(commit: Commit): SemVersion? {
        return if (SemVersion.isRelease(commit, settings))
            SemVersion.tryParse(commit)
        else
            tags[commit.sha]?.mapNotNull(SemVersion::tryParse)?.maxOrNull()
    }
}