package git.semver.plugin.semver

import git.semver.plugin.scm.Commit
import git.semver.plugin.scm.IRefInfo
import org.slf4j.LoggerFactory
import java.util.*

class VersionFinder(private val settings: SemverSettings, private val tags: Map<String, List<IRefInfo>>) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getVersion(commit: Commit, isClean: Boolean, defaultPreRelease: String?): SemInfoVersion {
        val semVersion = findVersion(commit)
        val isModified = semVersion.commitCount > 0 || !isClean
        val updated = semVersion.applyPendingChanges(isModified && !settings.noAutoBump, settings.groupVersionIncrements, settings.useTwoDigitVersion)

        if (!semVersion.isPreRelease && updated) {
            semVersion.setPreRelease(PreRelease.parse(defaultPreRelease))
        }
        return semVersion.toSemInfoVersion()
    }

    fun getReleaseVersion(commit: Commit, newPreRelease: String?): SemInfoVersion? {
        val semVersion = findVersion(commit)
        if (!semVersion.hasPendingChanges && settings.noReleaseAutoBump) {
            return null;
        }
        semVersion.commitCount = 0
        semVersion.applyPendingChanges(!semVersion.isPreRelease || "" != newPreRelease, settings.groupVersionIncrements, settings.useTwoDigitVersion)

        if (newPreRelease != null) {
            semVersion.setPreRelease(PreRelease.parse(newPreRelease))
        }
        return semVersion.toSemInfoVersion()
    }

    fun getChangeLog(commit: Commit): List<Commit> {
        val release = getReleaseSemVersionFromCommit(commit)
        return if (isRelease(release, true)) {
            // If this is a release commit, find all changes from the parent commits
            val changeLog = getChangeLog(getIncludedCommits(commit.parents, true).includedCommits)
            changeLog.add(commit)
            changeLog
        } else {
            getChangeLog(getIncludedCommits(sequenceOf(commit), true).includedCommits)
        }
    }

    private fun getChangeLog(commitData: List<CommitData>): MutableList<Commit> = commitData.asReversed()
        .filter { it.parents.size <= 1 }
        .map { it.commit }
        .toMutableList()

    private fun findVersion(startCommit: Commit): MutableSemVersion {
        if (startCommit.sha.isBlank()) {
            //This is a fake commit created when there exists no real commits
            return versionZero()
        }

        val result = getIncludedCommits(sequenceOf(startCommit), false)
        var lastFoundVersion = result.lastFoundVersion
        for (commitData in result.includedCommits.asReversed()) {
            // Second time we visit this commit after visiting parent commits

            // Check if we found a preRelease version first time we visited this commit
            val preReleaseVersion = result.visitedCommits[commitData.commit.sha]
            // Get and clear the semVersions for the parents so that they are not counted twice
            val parentSemVersions = commitData.parents
                .mapNotNull { result.visitedCommits.remove(it) }
                .toList()
            val maxVersionFromParents = getCombinedParentVersion(parentSemVersions)
            maxVersionFromParents.updateFromCommit(commitData.commit, settings, preReleaseVersion)
            result.visitedCommits[commitData.commit.sha] = maxVersionFromParents
            lastFoundVersion = maxVersionFromParents
        }
        return lastFoundVersion
    }

    private class CommitData(
        val commit: Commit,
        val parents: MutableList<String> = ArrayList(1),
        val isParentOfReleaseCommit: Boolean = false
    ) : Comparable<CommitData> {

        override fun compareTo(other: CommitData): Int {
            return other.commit.commitTime.compareTo(commit.commitTime)
        }
    }

    private class IncludedCommits(
        val lastFoundVersion: MutableSemVersion,
        val visitedCommits: MutableMap<String, MutableSemVersion?>,
        val includedCommits: List<CommitData>
    )

    private fun getIncludedCommits(commitsList: Sequence<Commit>, stopAtPreRelease: Boolean): IncludedCommits {
        var liveBranchCount = 1
        var lastFoundVersion = versionZero()

        // This code is a recursive algoritm rewritten as iterative to avoid stack overflow exception.
        // Unfortunately that makes it hard to understand.

        val commits = PriorityQueue(commitsList.map { CommitData(it) }.toList())
        val visitedCommits = mutableMapOf<String, MutableSemVersion?>()
        val includedCommits = mutableListOf<CommitData>()

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

                if (isRelease(releaseVersion, stopAtPreRelease)) {
                    logger.debug("Release version found: {}", releaseVersion)
                    // Release fond so no need to visit this commit again
                    lastFoundVersion = releaseVersion!!
                    liveBranchCount -= 1

                    markParentCommitsAsVisited(liveBranchCount, currentCommit, visitedCommits, commits)
                } else {
                    // This is a normal commit or a pre-release. We will visit this again in the second phase.
                    includedCommits.add(commitData)

                    currentCommit.parents.forEach {
                        commitData.parents.add(it.sha)
                        if (!visitedCommits.containsKey(it.sha)) {
                            // prepare to visit parent commit
                            commits.add(CommitData(it))
                        }
                    }
                    liveBranchCount += commitData.parents.size - 1
                }
            }
        }
        return IncludedCommits(lastFoundVersion, visitedCommits, includedCommits)
    }

    private fun getCombinedParentVersion(parentSemVersions: List<MutableSemVersion>): MutableSemVersion {
        return when {
            parentSemVersions.isEmpty() -> versionZero()
            parentSemVersions.size == 1 -> parentSemVersions.first()
            else -> parentSemVersions.max().combineChanges(parentSemVersions)
        }
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

    private fun isRelease(releaseVersion: MutableSemVersion?, stopAtPreRelease: Boolean) =
        releaseVersion != null && (stopAtPreRelease || !releaseVersion.isPreRelease)

    private fun versionZero() = MutableSemVersion()

    private fun getReleaseSemVersionFromCommit(commit: Commit): MutableSemVersion? {
        return if (MutableSemVersion.isRelease(commit, settings))
            MutableSemVersion.tryParse(commit)
        else
            tags[commit.sha]?.mapNotNull(MutableSemVersion::tryParse)?.maxOrNull()
    }
}