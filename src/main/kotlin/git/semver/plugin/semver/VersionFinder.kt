package git.semver.plugin.semver

import git.semver.plugin.scm.Commit
import git.semver.plugin.scm.IRefInfo
import org.slf4j.LoggerFactory
import java.util.ArrayDeque

class VersionFinder(private val settings: SemverSettings, private val tags: Map<String, List<IRefInfo>>) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getVersion(commit: Commit, isDirty: Boolean, defaultPreRelease: String?): SemVersion {
        val semVersion = findVersion(commit)
        val isModified = semVersion.commitCount > 0 || isDirty
        val updated = semVersion.applyPendingChanges(isModified && !settings.noAutoBump, settings.groupVersionIncrements)

        if (!semVersion.isPreRelease && updated) {
            semVersion.setPreRelease(defaultPreRelease)
        }
        return semVersion
    }

    fun getReleaseVersion(commit: Commit, newPreRelease: String?): SemVersion {
        val semVersion = findVersion(commit)
        semVersion.commitCount = 0
        semVersion.applyPendingChanges(!semVersion.isPreRelease || "" != newPreRelease, settings.groupVersionIncrements)

        if (newPreRelease != null) {
            semVersion.setPreRelease(newPreRelease)
        }
        return semVersion
    }

    fun getChangeLog(commit: Commit): List<Commit> {
        val changeLog = mutableListOf<Commit>()

        val release = getReleaseSemVersionFromCommit(commit)
        if (isRelease(release)) {
            findVersion(commit.parents, changeLog)
            changeLog.add(commit)
        } else {
            findVersion(commit, changeLog)
        }
        return changeLog
    }

    private fun findVersion(startCommit: Commit, changeLog: MutableList<Commit>? = null): SemVersion {
        if (startCommit.sha.isBlank()) {
            //This is a fake commit created when there exists no real commits
            return versionZero()
        }
        return findVersion(sequenceOf(startCommit), changeLog)
    }

    private fun findVersion(
        commitsList: Sequence<Commit>,
        changeLog: MutableList<Commit>?
    ): SemVersion {

        var lastFoundVersion = versionZero()
        // This code is a recursive algoritm rewritten as iterative to avoid stack overflow exception.
        // Unfortunately that makes it hard to understand.
        val commits = ArrayDeque(commitsList.map { it to ArrayList<String>(1) }.toList())
        val visitedCommits = mutableMapOf<String, SemVersion?>()
        while (commits.isNotEmpty()) {
            val peek = commits.peek()
            val currentCommit = peek.first
            val currentParentList = peek.second
            if (!visitedCommits.containsKey(currentCommit.sha)) {
                // First time we visit this commit
                val releaseVersion = getReleaseSemVersionFromCommit(currentCommit)
                visitedCommits[currentCommit.sha] = releaseVersion
                if (isRelease(releaseVersion)) {
                    logger.debug("Release version found: {}", releaseVersion)
                    // Release fond so no need to visit this commit again
                    commits.pop()
                    lastFoundVersion = releaseVersion!!
                } else {
                    currentCommit.parents.forEach {
                        currentParentList.add(it.sha)
                        if (!visitedCommits.containsKey(it.sha)) {
                            // prepare to visit parent commit
                            commits.push(it to ArrayList(1))
                        }
                    }
                }
            } else {
                // Second time we visit this commit after visiting parent commits
                addToChangeLog(currentCommit, changeLog, currentParentList.size > 1)

                // Check if we found a preRelease version first time we visited this commit
                val preReleaseVersion = visitedCommits[currentCommit.sha]

                // Get and clear the semVersions for the parents so that they are not counted twice
                val parentSemVersions = currentParentList
                    .mapNotNull { visitedCommits.put(it, null) }
                    .toList()

                val maxVersionFromParents = parentSemVersions.maxOrNull() ?: versionZero()
                maxVersionFromParents.mergeChanges(parentSemVersions)
                maxVersionFromParents.updateFromCommit(currentCommit, settings, preReleaseVersion)
                visitedCommits[currentCommit.sha] = maxVersionFromParents
                
                commits.pop()
                lastFoundVersion = maxVersionFromParents
            }
        }
        return lastFoundVersion
    }

    private fun addToChangeLog(
        currentCommit: Commit,
        changeLog: MutableList<Commit>?,
        isMergeCommit: Boolean
    ) {
        if (isMergeCommit) {
            //Ignore merge commits
            return
        }
        changeLog?.add(currentCommit)
    }

    private fun isRelease(releaseVersion: SemVersion?) =
        releaseVersion != null && !releaseVersion.isPreRelease

    private fun versionZero() = SemVersion()

    private fun getReleaseSemVersionFromCommit(commit: Commit): SemVersion? {
        return if (SemVersion.isRelease(commit, settings))
            SemVersion.tryParse(commit)
        else
            tags[commit.sha]?.mapNotNull(SemVersion::tryParse)?.maxOrNull()
    }
}