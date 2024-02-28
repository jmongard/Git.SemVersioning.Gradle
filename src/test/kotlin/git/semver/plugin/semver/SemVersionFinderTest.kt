package git.semver.plugin.semver

import git.semver.plugin.scm.Commit
import git.semver.plugin.scm.Tag
import kotlin.test.Test
import kotlin.test.assertEquals

class SemVersionFinderTest {
    companion object {

        private const val FIFTH = "0000005"
        private const val FOURTH = "0000004"
        private const val THIRD = "0000003"
        private const val SECOND = "0000002"
        private const val FIRST = "0000001"
        private const val ZERO = "0000000"
    }

    @Test
    fun release_and_pre_release_tags() {
        // given
        val commits = generateSHAString(0..6)
        val tags = listOf(
            Tag("v1.2", "SHA0"),
            Tag("v1.3.1-RC", "SHA3")
        )

        // when
        val versions = getVersion(tags, asCommit(commits))

        // then
        assertEquals("1.3.2-RC", versions.toVersionString())
        assertEquals("1.3.2-RC+003", versions.toInfoVersionString())
    }

    @Test
    fun release_tag_only() {
        // given
        val commits = generateSHAString(0..6)
        val tags = listOf(Tag("v1.2.1", "SHA0"))

        // when
        val versions = getVersion(tags, asCommit(commits))

        // then
        assertEquals("1.2.2-SNAPSHOT", versions.toVersionString())
        assertEquals("1.2.2-SNAPSHOT+006", versions.toInfoVersionString())
    }

    @Test
    fun multiple_release_tags() {
        // given
        val commits = generateSHAString(0..6)
        val tags = listOf(
            Tag("v1.2", "SHA0"),
            Tag("v1.3.1-RC", "SHA3"),
            Tag("v1.3.1", "SHA5")
        )

        // when
        val versions = getVersion(tags, asCommit(commits))

        // then
        assertEquals("1.3.2-SNAPSHOT", versions.toVersionString())
        assertEquals("1.3.2-SNAPSHOT+001", versions.toInfoVersionString())
    }

    @Test
    fun multiple_release_tags_branching() {
        // given
        val tags = listOf(
            Tag("v1.2", "SHA_A0"),
            Tag("v1.3.1-RC", "SHA_B1")
        )

        val a0 = Commit("fix: msg a0", "SHA_A0", sequenceOf())
        val a1 = Commit("fix: msg a1", "SHA_A1", sequenceOf(a0))

        val b0 = Commit("fix: msg b0", "SHA_B0", sequenceOf(a1))
        val b1 = Commit("fix: msg b1", "SHA_B1", sequenceOf(b0))
        val b2 = Commit("fix: msg b2", "SHA_B2", sequenceOf(b1))

        val c0 = Commit("fix: msg c0", "SHA_C0", sequenceOf(a1))
        val c1 = Commit("fix: msg c1", "SHA_C1", sequenceOf(c0))
        val c2 = Commit("fix: msg c2", "SHA_C2", sequenceOf(c1))
        val c3 = Commit("fix: msg c3", "SHA_C3", sequenceOf(c2))

        val d0 = Commit("fix: msg d0", "SHA_D0", sequenceOf(c3, b2))
        val d1 = Commit("fix: msg d1", "SHA_D1", sequenceOf(d0))

        // when
        val versions = getVersion(tags, d1)

        // then
        assertEquals("1.3.2-RC", versions.toVersionString())
        assertEquals("1.3.2-RC+007", versions.toInfoVersionString())
    }

    @Test
    fun multiple_release_tags_branching_no_grouping() {
        // given
        val tags = listOf(
            Tag("v0.4.0", "SHA0")
        )

        val a0 = Commit("a msg1", "SHA0", sequenceOf())
        val a1 = Commit("feat: a feature", "SHA1", sequenceOf(a0))
        val a2 = Commit("a msg3", "SHA2", sequenceOf(a1))

        val b0 = Commit("fix: test 11", "SHA11", sequenceOf(a2))
        val b1 = Commit("fix: test 12", "SHA12", sequenceOf(b0))
        val b2 = Commit("fix: test 13", "SHA13", sequenceOf(b1))

        val c0 = Commit("fix: test 21", "SHA21", sequenceOf(a2))
        val c1 = Commit("fix: test 22", "SHA22", sequenceOf(c0))

        val d0 = Commit("merge msg", "SHA31", sequenceOf(b2, c1))
        val d1 = Commit("fix: msg", "SHA32", sequenceOf(d0))

        // when
        val versions = getVersion(tags, d1, groupVersions = false)

        // then
        assertEquals("0.5.6-SNAPSHOT", versions.toVersionString())
        assertEquals("0.5.6-SNAPSHOT+009", versions.toInfoVersionString())
        assertEquals("0.5.6-SNAPSHOT", versions.toSemVersion().toString())
    }

    @Test
    fun prerelease_no_grouping() {
        // given
        val tags = listOf(
            Tag("v0.4.0", "SHA0"),
            Tag("v0.4.2-Alpha.1", "SHA12"),
        )

        val a0 = Commit("a msg1", "SHA0", sequenceOf())
        val a1 = Commit("a msg2", "SHA1", sequenceOf(a0))
        val a2 = Commit("a msg3", "SHA2", sequenceOf(a1))

        val b0 = Commit("fix: test 11", "SHA11", sequenceOf(a2))
        val b1 = Commit("fix: test 12", "SHA12", sequenceOf(b0))
        val b2 = Commit("fix: test 13", "SHA13", sequenceOf(b1))
        val b3 = Commit("fix: msg", "SHA14", sequenceOf(b2))

        // when
        val versions = getVersion(tags, b3, groupVersions = false)

        // then
        assertEquals("0.4.2-Alpha.3", versions.toVersionString())
        assertEquals("0.4.2-Alpha.3+002", versions.toInfoVersionString())
        assertEquals("0.4.2-Alpha.3", versions.toSemVersion().toString())
    }


    @Test
    fun one_commit_no_tags() {
        // given
        val commits = listOf(FIRST)
        val tags = emptyList<Tag>()

        // when
        val versions = getVersion(tags, asCommit(commits))

        // then
        assertEquals("0.0.1-SNAPSHOT", versions.toVersionString())
        assertEquals("0.0.1-SNAPSHOT+001", versions.toInfoVersionString())
    }

    @Test
    fun one_commit_no_version_tags() {
        // given
        val commits = listOf(FIRST)
        val tags = listOf(Tag("dummy", FIRST))

        // when
        val versions = getVersion(tags, asCommit(commits))

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
        val versions = getVersion(tags, asCommit(commits))

        // then
        assertEquals("1.0.0", versions.toVersionString())
        assertEquals("1.0.0", versions.toInfoVersionString())
        assertEquals("1.0.0", versions.toSemVersion().toString())
    }

    @Test
    fun two_commit_one_pretag() {
        // given
        val commits = listOf(FIRST, SECOND)
        val tags = listOf(Tag("v1.0.0-Alpha.1", SECOND))

        // when
        val versions = getVersion(tags, asCommit(commits))

        // then
        assertEquals("1.0.0-Alpha.1", versions.toVersionString())
        assertEquals("1.0.0-Alpha.1", versions.toInfoVersionString())
        assertEquals("1.0.0-Alpha.1", versions.toSemVersion().toString())
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
        val versions = getVersion(tags, asCommit(commits))

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
        val version = getVersion(tags, asCommit(commits))

        // then
        assertEquals("1.0.1-RC.3", version.toVersionString())
        assertEquals("1.0.1-RC3", version.toVersionString(false))
        assertEquals("1.0.1-RC.3+001", version.toInfoVersionString())
    }

    @Test
    fun `test update from commit pre-release dirty`() {
        assertEquals("1.1.1-Beta.3+001", getVersionFromTagAndPreAndCommitDirty("v1.1.0", "1.1.1-Beta.2", "Commit 1"))
        assertEquals("2.2.4-Alpha+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Alpha", "Commit 1"))
        assertEquals("2.2.3-Alpha.2+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Alpha.1", "fix: bug"))
        assertEquals("2.2.3-Beta.4+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Beta.3", "fix: bug"))
        assertEquals(
            "3.0.0-Beta.2+001",
            getVersionFromTagAndPreAndCommitDirty("v2.2.2", "3.0.0-Beta.1", "refactor!: drop some support")
        )
        assertEquals(
            "3.0.0-Beta.1+001",
            getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-Beta.1", "refactor!: drop some support")
        )
        assertEquals(
            "3.0.0-Beta.2+001",
            getVersionFromTagAndPreAndCommitDirty(
                "v2.2.2",
                "3.0.0-Beta.1",
                "feat: new api\r\n\r\nA message\r\n\r\nBREAKING CHANGE: drop support"
            )
        )
        assertEquals(
            "3.0.0-Beta.1+001",
            getVersionFromTagAndPreAndCommitDirty(
                "v2.2.2",
                "2.2.3-Beta.1",
                "feat: new api\r\n\r\nA message\r\n\r\nBREAKING CHANGE: drop support"
            )
        )
        assertEquals("2.3.0-NEXT.2+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.3.0-NEXT.1", "feat: new"))
        assertEquals("2.3.0-NEXT.1+001", getVersionFromTagAndPreAndCommitDirty("v2.2.2", "2.2.3-NEXT.1", "feat: new"))
        assertEquals(
            "2.3.0-SNAPSHOT+002",
            getVersionFromTagAndPreAndCommitDirty("v2.2.2", "1.0.0-IGNORED", "feat: new")
        )
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

        return getVersion(tags, asCommits(commits.reversed()).first(), true).toInfoVersionString()
    }

    @Test
    fun `test update from commit`() {
        assertEquals("1.1.1-SNAPSHOT+003", getVersionFromTagAndCommits("v1.1.0", "Commit 1", "Commit 2", "Commit 3"))
        assertEquals("1.2.1-SNAPSHOT+003", getVersionFromTagAndCommits("v1.2", "Commit 1", "Commit 2", "Commit 3"))
        assertEquals("2.2.3-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "Commit 1"))
        assertEquals("3.0.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "refactor!: drop some support"))
        assertEquals(
            "3.0.0-SNAPSHOT+001",
            getVersionFromTagAndCommits(
                "v2.2.2",
                "feat:  new api\r\n\r\nReplacing the old API\r\n\r\nBREAKING CHANGE: drop support"
            )
        )
        assertEquals("2.3.0-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "feat: new"))
        assertEquals("2.2.3-SNAPSHOT+001", getVersionFromTagAndCommits("v2.2.2", "fix: bug"))
        assertEquals("1.0.1-SNAPSHOT+002", getVersionFromTagAndCommits("v1.0.0", "fix: bug", "fix: bug"))

        assertEquals("3.0.0", getVersionFromTagAndCommits("v2.2.2", "refactor!: drop some support", "release: 3.0.0"))
        assertEquals("2.2.3", getVersionFromTagAndCommits("v2.2.2", "fix:   bug", "release: 2.2.3"))
        assertEquals("1.0.2", getVersionFromTagAndCommits("v1.0.0", "fix:   bug", "fix: bug", "release: 1.0.2"))
        assertEquals(
            "1.0.3-SNAPSHOT+002",
            getVersionFromTagAndCommits("v1.0.0", "fix:   bug", "fix: bug", "release: 1.0.2", "fix: bug", "fix: bug")
        )
        assertEquals("2.3.0", getVersionFromTagAndCommits("v2.2.2", "feat:  wow", "release: 2.3.0"))
        assertEquals("3.0.0", getVersionFromTagAndCommits("v2.2.2", "feat!: WOW", "release: 3.0.0"))
        assertEquals(
            "2.2.4-SNAPSHOT+001",
            getVersionFromTagAndCommits("v2.2.2", "fix:   bug", "release: 2.2.3", "fix: bug")
        )
        assertEquals(
            "2.3.1-SNAPSHOT+001",
            getVersionFromTagAndCommits("v2.2.2", "feat:  new", "release: 2.3.0", "fix: bug")
        )
        assertEquals(
            "3.0.1-SNAPSHOT+001",
            getVersionFromTagAndCommits("v2.2.2", "feat!: WOW", "release: 3.0.0", "fix: bug")
        )
        assertEquals(
            "2.3.0-SNAPSHOT+001",
            getVersionFromTagAndCommits("v2.2.2", "fix:   bug", "release: 2.2.3", "feat: new")
        )
        assertEquals(
            "2.4.0-SNAPSHOT+001",
            getVersionFromTagAndCommits("v2.2.2", "feat:  new", "release: 2.3.0", "feat: new")
        )
        assertEquals(
            "3.1.0-SNAPSHOT+001",
            getVersionFromTagAndCommits("v2.2.2", "feat!: WOW", "release: 3.0.0", "feat: new")
        )
        assertEquals(
            "3.0.0-SNAPSHOT+001",
            getVersionFromTagAndCommits("v2.2.2", "fix:   bug", "release: 2.2.3", "feat!: WOW")
        )
        assertEquals(
            "3.0.0-SNAPSHOT+001",
            getVersionFromTagAndCommits("v2.2.2", "feat:  new", "release: 2.3.0", "feat!: WOW")
        )
        assertEquals(
            "4.0.0-SNAPSHOT+001",
            getVersionFromTagAndCommits("v2.2.2", "feat!: WOW", "release: 3.0.0", "feat!: WOW")
        )
    }

    private fun getVersionFromTagAndCommits(tagName: String, vararg commits: String): String {
        val commitList =
            commits.mapIndexed { idx, it -> "000%02x".format(idx) to it }.reversed() + Pair(FIRST, "release: $tagName")
        return getVersion(emptyList(), asCommits(commitList).first(), false).toInfoVersionString()
    }

    @Test
    fun `test not grouping should count every change`() {
        val commits = listOf(
            ZERO to "feat!: A",
            FIRST to "feat!: B",
            SECOND to "feat: test",
            THIRD to "feat: test",
            FOURTH to "fix: test",
            FIFTH to "fix: test"
        )
        val tags = listOf(
            Tag("1.0.0", ZERO)
        )

        val actual = getVersion(tags, asCommits(commits.reversed()).first(), false, false)

        assertEquals("2.2.2-SNAPSHOT+005", actual.toInfoVersionString())
    }

    @Test
    fun `test no auto bump`() {
        val commits = listOf(
            ZERO to "feat!: A",
            FIRST to "feat!: B",
            SECOND to "feat: test",
            THIRD to "release: 2.2.0",
            FOURTH to "docs: test",
            FIFTH to "docs: test"
        )
        val tags = listOf(
            Tag("1.0.0", ZERO)
        )

        val actual = getVersion(tags, asCommits(commits.reversed()).first(), false, disableAutoBump = true)

        assertEquals("2.2.0+002", actual.toInfoVersionString())
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

        val actual = getVersion(tags, asCommits(commits.reversed()).first(), true)

        assertEquals("1.0.0-RC.3+001", actual.toInfoVersionString())
    }

    @Test
    fun testIncrementVersion_dirty() {
        assertEquals("1.1.1-SNAPSHOT", getVersionFromTagAndDirty("v1.1.0"))
        assertEquals("2.2.3-SNAPSHOT", getVersionFromTagAndDirty("v2.2.2"))
    }

    private fun getVersionFromTagAndDirty(tagName: String): String {
        return getVersion(
            listOf(Tag(tagName, FIRST)),
            asCommits(listOf(FIRST to "text")).first(),
            true
        ).toInfoVersionString()
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
        var versions = getVersion(tags, asCommit(commits))

        // then 
        assertEquals("1.2.0", versions.toVersionString())
        assertEquals("1.2.0", versions.toInfoVersionString())

        // given
        commits.add("2")
        commits.add("3")
        commits.add("4")

        // when 
        versions = getVersion(tags, asCommit(commits))

        // then 
        assertEquals("1.2.1-SNAPSHOT", versions.toVersionString())
        assertEquals("1.2.1-SNAPSHOT+003", versions.toInfoVersionString())

        // given
        commits.add("5")

        // when 
        versions = getVersion(tags, asCommit(commits))

        // then 
        assertEquals("1.3.0-RC", versions.toVersionString())
        assertEquals("1.3.0-RC", versions.toInfoVersionString())

        // given
        commits.add("6")
        commits.add("7")
        commits.add("8")

        // when 
        versions = getVersion(tags, asCommit(commits))

        // then 
        assertEquals("1.3.1-RC", versions.toVersionString())
        assertEquals("1.3.1-RC+003", versions.toInfoVersionString())

        // given
        commits.add("9")

        // when 
        versions = getVersion(tags, asCommit(commits))

        // then 
        assertEquals("1.3.0", versions.toVersionString())
        assertEquals("1.3.0", versions.toInfoVersionString())
    }

    private fun getVersion(
        tags: List<Tag>,
        commit: Commit,
        dirty: Boolean = false,
        groupVersions: Boolean = true,
        disableAutoBump: Boolean = false
    ): SemInfoVersion {
        val settings = SemverSettings().apply {
            groupVersionIncrements = groupVersions
            noAutoBump = disableAutoBump
        }
        return VersionFinder(settings, tags.groupBy { it.sha }).getVersion(commit, !dirty, "SNAPSHOT")
    }

    private fun asCommit(commits: List<String>) = asCommits(commits.reversed()).first()

    private fun asCommits(commits: List<String>): Sequence<Commit> {
        return commits.take(1).map { Commit("commit message", it, asCommits(commits.drop(1))) }.asSequence()
    }

    private fun asCommits(shas: Iterable<Pair<String, String>>): Sequence<Commit> {
        return shas.take(1).map { Commit(it.second, it.first, asCommits(shas.drop(1))) }.asSequence()
    }

    private fun generateSHAString(range: IntRange): List<String> {
        return range.map { "SHA$it" }
    }
}