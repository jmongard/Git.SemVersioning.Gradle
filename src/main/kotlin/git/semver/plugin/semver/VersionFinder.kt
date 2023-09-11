package git.semver.plugin.semver

import git.semver.plugin.scm.Commit
import git.semver.plugin.scm.IRefInfo
import org.slf4j.LoggerFactory
import java.util.ArrayDeque

class VersionFinder(private val settings: SemverSettings, private val tags: Map<String, List<IRefInfo>>) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getVersion(commit: Commit, isDirty: Boolean, defaultPreRelease: String?): SemVersion {
        val semVersion = startFindVersion(commit)
        val isModified = semVersion.commitCount > 0 || isDirty
        val updated = semVersion.applyPendingChanges(isModified && !settings.noAutoBump, settings.groupVersionIncrements)

        if (!semVersion.isPreRelease && updated) {
            semVersion.setPreRelease(defaultPreRelease)
        }
        return semVersion
    }

    fun getReleaseVersion(commit: Commit, newPreRelease: String?): SemVersion {
        val semVersion = startFindVersion(commit)
        semVersion.commitCount = 0
        semVersion.applyPendingChanges(!semVersion.isPreRelease || "" != newPreRelease, settings.groupVersionIncrements)

        if (newPreRelease != null) {
            semVersion.setPreRelease(newPreRelease)
        }
        return semVersion
    }

    fun getChangeLog(commit: Commit): List<String> {
        val changeLog = mutableListOf<String>()

        val release = getReleaseSemVersionFromCommit(commit)
        if (isRelease(release)) {
            findVersions(ArrayDeque(commit.parents.map { newEntry(it) }.toList()), changeLog)
        } else {
            findVersions(ArrayDeque(listOf(newEntry(commit))), changeLog)
        }
        return changeLog
    }

    private fun startFindVersion(startCommit: Commit): SemVersion {
        if (startCommit.sha.isBlank()) {
            return versionZero()
        }
        val versionMap = findVersions(ArrayDeque(listOf(newEntry(startCommit))))
        return versionMap[startCommit.sha] ?: versionZero()
    }

    private fun findVersions(
        commits: ArrayDeque<Pair<Commit, MutableList<String>>>,
        changeLog: MutableList<String>? = null
    ): MutableMap<String, SemVersion?> {
        // This code is a recursive algoritm rewritten as iterative to avoid stack overflow exception.
        // Unfortunately that makes it hard to understand.
        val visitedCommits = mutableMapOf<String, SemVersion?>()
        while (commits.isNotEmpty()) {
            val peek = commits.peek()
            val currentCommit = peek.first
            val currentParentList = peek.second
            if (!visitedCommits.containsKey(currentCommit.sha)) {
                // unvisited commit
                val releaseVersion = getReleaseSemVersionFromCommit(currentCommit)
                visitedCommits[currentCommit.sha] = releaseVersion
                if (isRelease(releaseVersion)) {
                    logger.debug("Release version found: {}", releaseVersion)
                    // return to previous commit
                    commits.pop()
                } else {
                    currentCommit.parents.forEach<Commit> {
                        currentParentList.add(it.sha)
                        if (!visitedCommits.containsKey(it.sha)) {
                            // prepare to visit parent commit
                            commits.push(newEntry(it))
                        }
                    }
                }
            } else {
                // returning from parent commit
                val parentSemVersions =
                    currentParentList.mapNotNull { getParentSemVersion(visitedCommits, it) }.toList()
                val preReleaseVersion = visitedCommits[currentCommit.sha]
                visitedCommits[currentCommit.sha] =
                    getMaxVersionFromParents(parentSemVersions, currentCommit, preReleaseVersion)

                addToChangeLog(preReleaseVersion, currentCommit, changeLog)

                // return to previous commit
                commits.pop()
            }
        }
        return visitedCommits
    }

    private fun addToChangeLog(
        preReleaseVersion: SemVersion?,
        currentCommit: Commit,
        changeLog: MutableList<String>?
    ) {
        if (preReleaseVersion == null || !SemVersion.isRelease(currentCommit, settings)) {
            changeLog?.add(currentCommit.text)
        }
    }

    private fun isRelease(releaseVersion: SemVersion?) =
        releaseVersion != null && !releaseVersion.isPreRelease

    private fun newEntry(startCommit: Commit): Pair<Commit, ArrayList<String>> =
        startCommit to ArrayList(1)

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

    private fun getReleaseSemVersionFromCommit(commit: Commit): SemVersion? {
        return if (SemVersion.isRelease(commit, settings))
            SemVersion.tryParse(commit)
        else
            tags[commit.sha]?.mapNotNull(SemVersion::tryParse)?.maxOrNull()
    }
}