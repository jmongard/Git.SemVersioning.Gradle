package git.semver.plugin.scm

import git.semver.plugin.semver.SemInfoVersion
import git.semver.plugin.semver.SemverSettings
import git.semver.plugin.semver.VersionFinder
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.util.FS
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

internal class GitProvider(private val settings: SemverSettings) {
    companion object {
        private const val REF_PREFIX = "refs/tags/"
        private val logger = LoggerFactory.getLogger(GitProvider::class.java)

        internal fun isFormatEnabled(flag: Boolean, format: String) = flag && format.isNotEmpty()
    }

    internal fun getSemVersion(startingPath: File): SemInfoVersion {
        getRepository(startingPath).use {
            return semVersion(Git(it))
        }
    }

    internal fun semVersion(it: Git): SemInfoVersion {
        val versionFinder = VersionFinder(settings, getTags(it.repository))
        return versionFinder.getVersion(
            getHeadCommit(it.repository),
            isClean(it),
            settings.defaultPreRelease
        )
    }

    internal fun getChangeLog(startingPath: File): List<Commit> {
        getRepository(startingPath).use {
            return changeLog(Git(it))
        }
    }

    internal fun changeLog(it: Git): List<Commit> {
        val versionFinder = VersionFinder(settings, getTags(it.repository))
        return versionFinder.getChangeLog(
            getHeadCommit(it.repository))
    }

    internal fun createRelease(
        startingPath: File,
        params: ReleaseParams
    ) {
        getRepository(startingPath).use {
            createRelease(Git(it), params)
        }
    }

    internal fun createRelease(
        it: Git,
        params: ReleaseParams
    ) {
        checkDirty(params.noDirtyCheck, isClean(it))

        val versionFinder = VersionFinder(settings, getTags(it.repository))
        val version = versionFinder.getReleaseVersion(
            getHeadCommit(it.repository),
            params.preRelease?.trimStart('-')
        )
        val versionString = version.toInfoVersionString()
        logger.info("Saving new version: {}", versionString)

        val isCommit = isFormatEnabled(params.commit, settings.releaseCommitTextFormat)
        if (isCommit) {
            val commitMessage = settings.releaseCommitTextFormat.format(versionString, params.message.orEmpty())
            val commitCommand = it.commit().setMessage(commitMessage.trim())
            settings.gitSigning?.let(commitCommand::setSign) // Set signing if not set to null
            commitCommand.call()
        }

        val isTag = isFormatEnabled(params.tag, settings.releaseTagNameFormat)
        if (isTag) {
            val name = settings.releaseTagNameFormat.format(versionString)
            val tagCommand = it.tag().setName(name).setMessage(params.message)
            settings.gitSigning?.let(tagCommand::setSigned) // Set signing if not set to null
            tagCommand.call()
            println("Created new local Git tag: $name")
        }

        if (!isCommit && !isTag) {
            println("Dry run - calculated version: $versionString")
        }
    }

    private fun getRepository(startingPath: File) = RepositoryBuilder()
        .setFS(FS.DETECTED)
        .findGitDir(startingPath)
        .setMustExist(true)
        .build()

    internal fun isClean(git: Git): Boolean {
        val status = git.status().call()
        if (status.isClean) {
            return true
        }
        logger.info("The Git repository is dirty. (Check: {})", settings.noDirtyCheck)
        val changes = mapOf(
            "added" to status.added.size,
            "changed" to status.changed.size,
            "removed" to status.removed.size,
            "missing" to status.missing.size,
            "modified" to status.modified.size,
            "conflicting" to status.conflicting.size,
            "untracked" to status.untracked.size
        )
        logger.info("Changes: {}", changes.filter { it.value > 0 }.keys.joinToString(", "))
        logger.debug("added: {}", status.added)
        logger.debug("changed: {}", status.changed)
        logger.debug("removed: {}", status.removed)
        logger.debug("conflicting: {}", status.conflicting)
        logger.debug("missing: {}", status.missing)
        logger.debug("modified: {}", status.modified)
        logger.debug("untracked: {}", status.untracked)
        return settings.noDirtyCheck
    }

    private fun getTags(repository: Repository): Map<String, List<Tag>> {
        return repository.refDatabase.getRefsByPrefix(REF_PREFIX).map {
            Tag(it.name.removePrefix(REF_PREFIX), getObjectIdFromRef(repository, it).name)
        }.groupBy { it.sha }
    }

    internal fun getHeadCommit(it: Repository): Commit {
        val revWalk = RevWalk(it)
        val head = it.resolve("HEAD") ?: return Commit("", "", 0, emptySequence())
        val revCommit = revWalk.parseCommit(head)
        revWalk.markStart(revCommit)
        return getCommit(revCommit, revWalk)
    }

    private fun getCommit(commit: RevCommit, revWalk: RevWalk): Commit {
        return Commit(commit.fullMessage, commit.name, commit.commitTime, sequence {
            for (parent in commit.parents) {
                revWalk.parseHeaders(parent)
                yield(getCommit(parent, revWalk))
            }
        }, commit.authorIdent.name, commit.authorIdent.emailAddress, Date.from(commit.authorIdent.whenAsInstant))
    }

    internal fun checkDirty(noDirtyCheck: Boolean, isClean: Boolean) {
        check(noDirtyCheck || isClean) { "Local modifications exists" }
    }

    private fun getObjectIdFromRef(repository: Repository, ref: Ref): ObjectId =
        repository.refDatabase.peel(ref).peeledObjectId ?: ref.objectId

    internal class ReleaseParams(
        val tag: Boolean,
        val commit: Boolean,
        val preRelease: String?,
        val message: String? = null,
        val noDirtyCheck: Boolean)
}