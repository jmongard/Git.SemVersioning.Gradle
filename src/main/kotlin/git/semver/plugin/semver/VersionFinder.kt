package git.semver.plugin.semver

import git.semver.plugin.scm.Commit
import git.semver.plugin.scm.IRefInfo
import org.slf4j.LoggerFactory
import java.util.ArrayDeque
import java.util.PriorityQueue

class VersionFinder(private val settings: SemverSettings, private val tags: Map<String, List<IRefInfo>>) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getVersion(commit: Commit, isClean: Boolean, defaultPreRelease: String?): SemInfoVersion {
        val semVersion = findVersion(commit)
        val isModified = semVersion.commitCount > 0 || !isClean
        val updated = semVersion.applyPendingChanges(isModified && !settings.noAutoBump, settings.groupVersionIncrements)

        if (!semVersion.isPreRelease && updated) {
            semVersion.setPreRelease(defaultPreRelease)
        }
        return semVersion.toSemVersion()
    }

    fun getReleaseVersion(commit: Commit, newPreRelease: String?): SemInfoVersion {
        val semVersion = findVersion(commit)
        semVersion.commitCount = 0
        semVersion.applyPendingChanges(!semVersion.isPreRelease || "" != newPreRelease, settings.groupVersionIncrements)

        if (newPreRelease != null) {
            semVersion.setPreRelease(newPreRelease)
        }
        return semVersion.toSemVersion()
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

    private fun findVersion(startCommit: Commit, changeLog: MutableList<Commit>? = null): MutableSemVersion {
        if (startCommit.sha.isBlank()) {
            //This is a fake commit created when there exists no real commits
            return versionZero()
        }
        return findVersion(sequenceOf(startCommit), changeLog)
    }

    data class CommitData(
        val commit: Commit,
        val parents: MutableList<String>,
        val isParentOfReleaseCommit: Boolean
    ) : Comparable<CommitData> {

        override fun compareTo(other: CommitData): Int {
            return other.commit.commitTime.compareTo(commit.commitTime)
        }
    }

    private fun findVersion(
        commitsList: Sequence<Commit>,
        changeLog: MutableList<Commit>?
    ): MutableSemVersion {

        var liveBranchCount = 1;
        var lastFoundVersion = versionZero()

        // This code is a recursive algoritm rewritten as iterative to avoid stack overflow exception.
        // Unfortunately that makes it hard to understand.

        val commits = PriorityQueue(commitsList.map { CommitData(it, ArrayList(1), false) }.toList())
        val visitedCommits = mutableMapOf<String, MutableSemVersion?>()
        val includedCommits = ArrayDeque<CommitData>()

        while (commits.isNotEmpty()) {
            val commitData = commits.remove()
            val currentCommit = commitData.commit

            // First time we visit this commit

            if (commitData.isParentOfReleaseCommit) {
                // This commit is a parent of a release commit
                markParentCommitsAsVisited(liveBranchCount, currentCommit, visitedCommits, commits)
            } else if (!visitedCommits.containsKey(currentCommit.sha)) {

                val releaseVersion = getReleaseSemVersionFromCommit(currentCommit)
                visitedCommits[currentCommit.sha] = releaseVersion

                if (isRelease(releaseVersion)) {
                    logger.debug("Release version found: {}", releaseVersion)
                    // Release fond so no need to visit this commit again
                    lastFoundVersion = releaseVersion!!

                    liveBranchCount -= 1

                    markParentCommitsAsVisited(liveBranchCount, currentCommit, visitedCommits, commits)
                } else {
                    // This is a normal commit or a pre-release. We will visit this again in the second phase.
                    includedCommits.push(commitData)

                    currentCommit.parents.forEach {
                        commitData.parents.add(it.sha)
                        if (!visitedCommits.containsKey(it.sha)) {
                            // prepare to visit parent commit
                            commits.add(CommitData(it, ArrayList(1), false))
                        }
                    }
                    liveBranchCount += commitData.parents.size - 1
                }
            }
        }

        while (includedCommits.isNotEmpty()) {

            val commitData = includedCommits.pop()
            val currentCommit = commitData.commit
            val currentParentList = commitData.parents

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

            lastFoundVersion = maxVersionFromParents

        }
        return lastFoundVersion
    }

    private fun markParentCommitsAsVisited(
        liveBranchCount: Int,
        currentCommit: Commit,
        visitedCommits: MutableMap<String, MutableSemVersion?>,
        commits: PriorityQueue<CommitData>
    ) {
        if (liveBranchCount == 0) {
            return
        }
        currentCommit.parents.filter { !visitedCommits.containsKey(it.sha) }.forEach {
            visitedCommits[it.sha] = null
            commits.add(CommitData(it, ArrayList(1), true))
        }
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

    private fun isRelease(releaseVersion: MutableSemVersion?) =
        releaseVersion != null && !releaseVersion.isPreRelease

    private fun versionZero() = MutableSemVersion()

    private fun getReleaseSemVersionFromCommit(commit: Commit): MutableSemVersion? {
        return if (MutableSemVersion.isRelease(commit, settings))
            MutableSemVersion.tryParse(commit)
        else
            tags[commit.sha]?.mapNotNull(MutableSemVersion::tryParse)?.maxOrNull()
    }
}