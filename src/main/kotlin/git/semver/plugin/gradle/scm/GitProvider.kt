package git.semver.plugin.gradle.scm

import git.semver.plugin.gradle.semver.SemVersion
import git.semver.plugin.gradle.semver.SemverSettings
import git.semver.plugin.gradle.semver.VersionFinder
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

class GitProvider(private val settings: SemverSettings) {
    companion object {
        private const val REF_PREFIX = "refs/tags/"
        private val logger = LoggerFactory.getLogger(GitProvider::class.java)
    }

    internal fun GetSemVersion(startingPath: File): SemVersion {
       RepositoryBuilder()
                .setFS(FS.DETECTED)
                .findGitDir(startingPath)
                .setMustExist(true)
                .build().use {
            return GetSemVersion(it)
        }
    }

    internal fun GetSemVersion(repository: Repository): SemVersion {
        val revWalk = RevWalk(repository)
        val head = repository.resolve("HEAD")
        val revCommit = revWalk.parseCommit(head)
        revWalk.markStart(revCommit);

        val git = Git(repository)

        val tags = getTags(repository)
        val commit = createCommit(revCommit, revWalk)
        val status = git.status().call();
        if (!status.isClean) {
            logger.info("The GIT repository is dirty")
            logger.debug("added: {}", status.added)
            logger.debug("changed: {}", status.changed)
            logger.debug("removed: {}", status.removed)
            logger.debug("conflicting: {}", status.conflicting)
            logger.debug("missing: {}", status.missing)
            logger.debug("modified: {}", status.modified)
        }
        return VersionFinder(settings, tags).getVersion(commit, !status.isClean)
    }

    private fun getTags(repository: Repository): Map<String, List<Tag>> {
        return repository.refDatabase.getRefsByPrefix(REF_PREFIX).map {
            Tag(it.name.removePrefix(REF_PREFIX), getObjectIdFromRef(repository, it).name)
        }.groupBy { it.sha }
    }

    private fun createCommit(commit: RevCommit, revWalk: RevWalk): Commit {
         return Commit(commit.fullMessage, commit.name, sequence {
            for (parent in commit.parents) {
                revWalk.parseHeaders(parent)
                yield(createCommit(parent, revWalk))
            }
        })
    }

    private fun getObjectIdFromRef(repository: Repository, ref : Ref) : ObjectId =
            repository.refDatabase.peel(ref).peeledObjectId ?: ref.objectId;
}