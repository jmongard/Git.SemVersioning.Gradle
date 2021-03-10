package git.semver.plugin.scm

import git.semver.plugin.semver.SemVersion
import git.semver.plugin.semver.SemverSettings
import git.semver.plugin.semver.VersionFinder
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.IllegalStateException

class GitProvider(private val settings: SemverSettings) {
    companion object {
        private const val REF_PREFIX = "refs/tags/"
        private val logger = LoggerFactory.getLogger(GitProvider::class.java)
    }

    internal fun getSemVersion(startingPath: File): SemVersion {
        return doWithRepository(startingPath, ::semVersion)
    }

    internal fun semVersion(it: Git): SemVersion {
        val versionFinder = VersionFinder(settings, getTags(it.repository))
        return versionFinder.getVersion(
            getHeadCommit(it.repository),
            !isClean(it),
            settings.defaultPreRelease
        )
    }

    internal fun createRelease(
        startingPath: File,
        tag: Boolean,
        commit: Boolean,
        preRelease: String?,
        message: String? = null,
        noDirtyCheck: Boolean
    ) {
        doWithRepository(startingPath) {
            createRelease(it, tag, commit, preRelease, message, noDirtyCheck)
        }
    }

    internal fun createRelease(
        it: Git,
        tag: Boolean,
        commit: Boolean,
        preRelease: String?,
        message: String? = null,
        noDirtyCheck: Boolean
    ) {
        if (noDirtyCheck || isClean(it)) {
            val versionFinder = VersionFinder(settings, getTags(it.repository))
            val version = versionFinder.getReleaseVersion(
                getHeadCommit(it.repository),
                preRelease?.trimStart('-')
            )
            val versionString = version.toInfoVersionString()
            logger.info("Saving new version: {}", versionString)
            if (commit) {
                it.commit().setMessage(settings.releaseCommitTextFormat.format(versionString, message ?: "").trim())
                    .call()
            }
            if (tag) {
                it.tag().setName(settings.releaseTagNameFormat.format(versionString)).setMessage(message).call()
            }
        } else {
            throw IllegalStateException("Local modifications exists")
        }
    }

    private inline fun <T> doWithRepository(startingPath: File, func: (Git) -> T): T {
        RepositoryBuilder()
            .setFS(FS.DETECTED)
            .findGitDir(startingPath)
            .setMustExist(true)
            .build().use {
                return func(Git(it))
            }
    }

    private fun isClean(git: Git): Boolean {
        val status = git.status().call()
        if (!status.isClean) {
            logger.info("The Git repository is dirty")
            logger.debug("added: {}", status.added)
            logger.debug("changed: {}", status.changed)
            logger.debug("removed: {}", status.removed)
            logger.debug("conflicting: {}", status.conflicting)
            logger.debug("missing: {}", status.missing)
            logger.debug("modified: {}", status.modified)
        }
        return status.isClean
    }

    private fun getTags(repository: Repository): Map<String, List<Tag>> {
        return repository.refDatabase.getRefsByPrefix(REF_PREFIX).map {
            Tag(it.name.removePrefix(REF_PREFIX), getObjectIdFromRef(repository, it).name)
        }.groupBy { it.sha }
    }

    internal fun getHeadCommit(it: Repository): Commit {
        val revWalk = RevWalk(it)
        val head = it.resolve("HEAD")
        val revCommit = revWalk.parseCommit(head)
        revWalk.markStart(revCommit)
        return getCommit(revCommit, revWalk)
    }

    private fun getCommit(commit: RevCommit, revWalk: RevWalk): Commit {
        return Commit(commit.fullMessage, commit.name, sequence {
            for (parent in commit.parents) {
                revWalk.parseHeaders(parent)
                yield(getCommit(parent, revWalk))
            }
        })
    }

    private fun getObjectIdFromRef(repository: Repository, ref: Ref): ObjectId =
        repository.refDatabase.peel(ref).peeledObjectId ?: ref.objectId
}