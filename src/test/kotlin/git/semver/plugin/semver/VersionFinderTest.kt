package git.semver.plugin.semver

import git.semver.plugin.scm.Commit
import git.semver.plugin.scm.Tag
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionFinderTest {
    companion object {
        private const val FOURTH = "0000004"
        private const val THIRD = "0000003"
        private const val SECOND = "0000002"
        private const val FIRST = "0000001"
        private const val ZERO = "0000000"
    }

    @Test
    fun release_and_pre_release_tags() {
        // given
        val commits = generateCommitList(0..6)
        val tags = listOf(
                Tag("v1.2", commits[0]),
                Tag("v1.3.1-RC", commits[3])
        )

        // when
        val versions = getVersion(commits, tags)

        // then
        assertEquals("1.3.1-RC", versions.toVersionString())
        assertEquals("1.3.1-RC+003", versions.toInfoVersionString())
    }

    @Test
    fun release_tag_only() {
        // given
        val commits = generateCommitList(0..6)
        val tags = listOf(Tag("v1.2.1", commits[0]))

        // when
        val versions = getVersion(commits, tags)

        // then
        assertEquals("1.2.2-SNAPSHOT", versions.toVersionString())
        assertEquals("1.2.2-SNAPSHOT+006", versions.toInfoVersionString())
    }

    @Test
    fun multiple_release_tags() {
        // given
        val commits = generateCommitList(0..6)
        val tags = listOf(
                Tag("v1.2", commits[0]),
                Tag("v1.3.1-RC", commits[3]),
                Tag("v1.3.1", commits[5])
        )

        // when
        val versions = getVersion(commits, tags)

        // then
        assertEquals("1.3.2-SNAPSHOT", versions.toVersionString())
        assertEquals("1.3.2-SNAPSHOT+001", versions.toInfoVersionString())
    }

    @Test
    fun one_commit_no_tags() {
        // given
        val commits = listOf(FIRST)
        val tags = emptyList<Tag>()

        // when
        val versions = getVersion(commits, tags)

        // then
        assertEquals("0.0.1-SNAPSHOT", versions.toVersionString())
        assertEquals("0.0.1-SNAPSHOT+001", versions.toInfoVersionString())
    }

    @Test
    fun one_commit_one_tag() {
        // given
        val commits = listOf(FIRST)
        val tags = listOf(Tag("v1.0", FIRST))

        // when
        val versions = getVersion(commits, tags)

        // then
        assertEquals("1.0.0", versions.toVersionString())
        assertEquals("1.0.0", versions.toInfoVersionString())
    }

    @Test
    fun two_commit_one_pretag() {
        // given
        val commits = listOf(FIRST, SECOND)
        val tags = listOf(Tag("v1.0.0-Alpha.1", SECOND))

        // when
        val versions = getVersion(commits, tags)

        // then
        assertEquals("1.0.0-Alpha.1", versions.toVersionString())
        assertEquals("1.0.0-Alpha.1", versions.toInfoVersionString())
    }

    @Test
    fun multiple_tags_same_commit() {
        // given
        val commits = listOf(FIRST)
        val tags = listOf(
                Tag("foo", FIRST),
                Tag("v1.0", FIRST),
                Tag("bar", FIRST)
        )

        // when
        val versions = getVersion(commits, tags)

        // then
        assertEquals("1.0.0", versions.toVersionString())
        assertEquals("1.0.0", versions.toInfoVersionString())
    }

    @Test
    fun multiple_prerelease_tags_same_commit() {
        // given
        val commits = listOf(

            FIRST,
            SECOND,
            THIRD
        )
        val tags = listOf(
                Tag("v1.0.0", FIRST),
                Tag("foo", SECOND),
                Tag("v1.0.1-RC.2", SECOND),
                Tag("bar", SECOND)
        )

        // when
        val version = getVersion(commits, tags)

        // then
        assertEquals("1.0.1-RC.3", version.toVersionString())
        assertEquals("1.0.1-RC3", version.toVersionString(false))
        assertEquals("1.0.1-RC.3+001", version.toInfoVersionString())
    }

    @Test
    fun `test update from commit pre-release dirty`() {
        assertEquals("1.1.1-Beta.3+001", getVersionFromTagAndPreAndCommitDirty("v1.1.0", "1.1.1-Beta.2", "Commit 1"))
        assertEquals("2.2.3-Alpha+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Alpha", "Commit 1"))
        assertEquals("2.2.3-Alpha.2+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Alpha.1", "fix: bug"))
        assertEquals("2.2.3-Beta.4+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Beta.3", "fix: bug"))
        assertEquals("3.0.0-Beta.2+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "3.0.0-Beta.1", "refactor!: drop some support"))
        assertEquals("3.0.0-Beta.1+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Beta.1", "refactor!: drop some support"))
        assertEquals("3.0.0-Beta.2+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "3.0.0-Beta.1", "feat: new api\r\n\r\nA message\r\n\r\nBREAKING CHANGE: drop support"))
        assertEquals("3.0.0-Beta.1+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Beta.1", "feat: new api\r\n\r\nA message\r\n\r\nBREAKING CHANGE: drop support"))
        assertEquals("2.3.0-NEXT.2+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.3.0-NEXT.1", "feat: new"))
        assertEquals("2.3.0-NEXT.1+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-NEXT.1", "feat: new"))
        assertEquals("2.3.0-SNAPSHOT+002", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "1.0.0-IGNORED", "feat: new"))
        assertEquals("2.3.0-NEXT+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-NEXT", "feat: new"))
    }

    private fun getVersionFromTagAndPreAndCommitDirty(tagName: String, preRelease: String, commit: String): String {
        val commits = listOf(
            FIRST to "release tagged",
            SECOND to "pre-release tagged",
            THIRD to commit
        )
        val tags = listOf(
            Tag(tagName, FIRST),
            Tag(preRelease, SECOND)
        )

        return getVersion(tags, asCommits(commits.reversed()), true).toInfoVersionString()
    }

    @Test
    fun `test update from commit`() {
        assertEquals("1.1.1-SNAPSHOT+003", getVersionFromTagAndCommits("v1.1.0", "Commit 1", "Commit 2", "Commit 3"))
        assertEquals("1.2.1-SNAPSHOT+003", getVersionFromTagAndCommits("v1.2", "Commit 1", "Commit 2", "Commit 3"))
        assertEquals("2.2.3-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "Commit 1"))
        assertEquals("3.0.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "refactor!: drop some support"))
        assertEquals("3.0.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "feat:  new api\r\n\r\nReplacing the old API\r\n\r\nBREAKING CHANGE: drop support"))
        assertEquals("2.3.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "feat: new"))
        assertEquals("2.2.3-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "fix: bug"))
        assertEquals("1.0.1-SNAPSHOT+002", getVersionFromTagAndCommits("v1.0.0", "fix: bug", "fix: bug"))

        assertEquals("3.0.0", getVersionFromTagAndCommits("v2.2.2", "refactor!: drop some support", "release: 3.0.0"))
        assertEquals("2.2.3", getVersionFromTagAndCommits("v2.2.2", "fix:   bug", "release: 2.2.3"))
        assertEquals("1.0.2", getVersionFromTagAndCommits("v1.0.0", "fix:   bug", "fix: bug", "release: 1.0.2"))
        assertEquals("1.0.3-SNAPSHOT+002", getVersionFromTagAndCommits("v1.0.0", "fix:   bug", "fix: bug", "release: 1.0.2", "fix: bug", "fix: bug"))
        assertEquals("2.3.0", getVersionFromTagAndCommits("v2.2.2", "feat:  wow", "release: 2.3.0"))
        assertEquals("3.0.0", getVersionFromTagAndCommits("v2.2.2", "feat!: WOW", "release: 3.0.0"))
        assertEquals("2.2.4-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "fix:   bug", "release: 2.2.3", "fix: bug"))
        assertEquals("2.3.1-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "feat:  new", "release: 2.3.0", "fix: bug"))
        assertEquals("3.0.1-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "feat!: WOW", "release: 3.0.0", "fix: bug"))
        assertEquals("2.3.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "fix:   bug", "release: 2.2.3", "feat: new"))
        assertEquals("2.4.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "feat:  new", "release: 2.3.0", "feat: new"))
        assertEquals("3.1.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "feat!: WOW", "release: 3.0.0", "feat: new"))
        assertEquals("3.0.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "fix:   bug", "release: 2.2.3", "feat!: WOW"))
        assertEquals("3.0.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "feat:  new", "release: 2.3.0", "feat!: WOW"))
        assertEquals("4.0.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "feat!: WOW", "release: 3.0.0", "feat!: WOW"))
    }

    private fun getVersionFromTagAndCommits(tagName: String, vararg  commits: String): String {
        val commitList = commits.mapIndexed { idx, it-> "000%02x".format(idx) to it }.reversed() + Pair(FIRST, "release: $tagName")
        return getVersion(emptyList(), asCommits(commitList), false).toInfoVersionString()
    }

    @Test
    fun `test preRelease version`() {
        val commits = listOf(
            ZERO to "release: 0.1.0",
            FIRST to "tagged preRelease",
            SECOND to "fix!: test",
            THIRD to "release: 1.0.0-RC.2",
            FOURTH to "feat: test"
        )
        val tags = listOf(
            Tag("1.0.0-RC.1", FIRST)
        )

        val actual = getVersion(tags, asCommits(commits.reversed()), true)

        assertEquals("1.0.0-RC.3+001", actual.toInfoVersionString())
    }

    @Test
    fun testIncrementVersion_dirty() {
        assertEquals("1.1.1-SNAPSHOT", getVersionFromTagAndDirty("v1.1.0"))
        assertEquals("2.2.3-SNAPSHOT", getVersionFromTagAndDirty("v2.2.2"))
    }

    private fun getVersionFromTagAndDirty(tagName: String): String {
        return getVersion(listOf(Tag(tagName, FIRST)), asCommits(listOf(FIRST to "text")), true).toInfoVersionString()
    }

    @Test
    fun testDox() {
        // given
        val commits = mutableListOf("1")
        val tags = listOf(
                Tag("v1.2", "1"),
                Tag("v1.3-RC", "5"),
                Tag("v1.3", "9")
        )

        // when 
        var versions = getVersion(commits, tags)

        // then 
        assertEquals("1.2.0", versions.toVersionString())
        assertEquals("1.2.0", versions.toInfoVersionString())

        // given
        commits.add("2")
        commits.add("3")
        commits.add("4")

        // when 
        versions = getVersion(commits, tags)

        // then 
        assertEquals("1.2.1-SNAPSHOT", versions.toVersionString())
        assertEquals("1.2.1-SNAPSHOT+003", versions.toInfoVersionString())

        // given
        commits.add("5")

        // when 
        versions = getVersion(commits, tags)

        // then 
        assertEquals("1.3.0-RC", versions.toVersionString())
        assertEquals("1.3.0-RC", versions.toInfoVersionString())

        // given
        commits.add("6")
        commits.add("7")
        commits.add("8")

        // when 
        versions = getVersion(commits, tags)

        // then 
        assertEquals("1.3.0-RC", versions.toVersionString())
        assertEquals("1.3.0-RC+003", versions.toInfoVersionString())

        // given
        commits.add("9")

        // when 
        versions = getVersion(commits, tags)

        // then 
        assertEquals("1.3.0", versions.toVersionString())
        assertEquals("1.3.0", versions.toInfoVersionString())
    }

    private fun getVersion(commits: List<String>, tags: List<Tag>, dirty: Boolean = false): SemVersion {
        return getVersion(tags, asCommits(commits.reversed()), dirty)
    }

    private fun getVersion(
        tags: List<Tag>,
        commits: Sequence<Commit>,
        dirty: Boolean
    ): SemVersion {
        return VersionFinder(SemverSettings(), tags.groupBy { it.sha }).getVersion(commits.first(), dirty, "SNAPSHOT")
    }

    private fun asCommits(shas: List<String>): Sequence<Commit> {
        return shas.take(1).map { Commit("commit message", it, asCommits(shas.drop(1))) }.asSequence()
    }

    private fun asCommits(shas: Iterable<Pair<String, String>>): Sequence<Commit> {
        return shas.take(1).map { Commit(it.second, it.first, asCommits(shas.drop(1))) }.asSequence()
    }

    private fun generateCommitList(range:IntRange) : List<String> {
        return range.map { "SHA$it" }
    }
}