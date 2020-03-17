package git.semver.plugin.semver

import git.semver.plugin.gradle.semver.SemverSettings
import git.semver.plugin.gradle.scm.Commit
import git.semver.plugin.gradle.scm.Tag
import git.semver.plugin.gradle.semver.SemVersion
import git.semver.plugin.gradle.semver.VersionFinder
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionFinderTest {
    @Test
    fun release_and_pre_release_tags() {
        // arrange
        val commits = listOf(
                "sixth",
                "fifth",
                "fourth",
                "third",
                "second",
                "first",
                "zero")
        val tags = listOf(
                Tag("v1.2", "zero"),
                Tag("v1.3.1-RC", "third"))

        // act
        val versions = getVersion(commits, tags)

        // assert
        assertEquals("1.3.1", versions.toVersionString())
        assertEquals("1.3.1-RC+003", versions.toInfoVersionString())
    }

    @Test
    fun release_tag_only() {
        // arrange
        val commits = listOf(
                "sixth",
                "fifth",
                "fourth",
                "third",
                "second",
                "first",
                "zero")
        val tags = listOf(Tag("v1.2.1", "zero"))

        // act
        val versions = getVersion(commits, tags)

        // assert
        assertEquals("1.2.2", versions.toVersionString())
        assertEquals("1.2.2-SNAPSHOT+006", versions.toInfoVersionString())
    }

    @Test
    fun multiple_release_tags() {
        // arrange
        val commits = listOf(
                "sixth",
                "fifth",
                "fourth",
                "third",
                "second",
                "first",
                "zero"
        )
        val tags = listOf(
                Tag("v1.2", "zero"),
                Tag("v1.3.1-RC", "third"),
                Tag("v1.3.1", "fifth")
        )

        // act
        val versions = getVersion(commits, tags)

        // assert
        assertEquals("1.3.2", versions.toVersionString())
        assertEquals("1.3.2-SNAPSHOT+001", versions.toInfoVersionString())
    }

    @Test
    fun one_commit_no_tags() {
        // arrange
        val commits = listOf("first")
        val tags = emptyList<Tag>()

        // act
        val versions = getVersion(commits, tags)

        // assert
        assertEquals("0.0.1", versions.toVersionString())
        assertEquals("0.0.1-SNAPSHOT+001", versions.toInfoVersionString())
    }

    @Test
    fun one_commit_one_tag() {
        // arrange
        val commits = listOf("first")
        val tags = listOf(Tag("v1.0", "first"))

        // act
        val versions = getVersion(commits, tags)

        // assert
        assertEquals("1.0.0", versions.toVersionString())
        assertEquals("1.0.0", versions.toInfoVersionString())
    }

    @Test
    fun two_commit_one_pretag() {
        // arrange
        val commits = listOf("first", "second")
        val tags = listOf(Tag("v1.0.0-Alpha.1", "first"))

        // act
        val versions = getVersion(commits, tags)

        // assert
        assertEquals("1.0.0", versions.toVersionString())
        assertEquals("1.0.0-Alpha.1", versions.toInfoVersionString())
    }

    @Test
    fun multiple_tags_same_commit() {
        // arrange
        val commits = listOf("first")
        val tags = listOf(
                Tag("foo", "first"),
                Tag("v1.0", "first"),
                Tag("bar", "first")
        )

        // act
        val versions = getVersion(commits, tags)

        // assert
        assertEquals("1.0.0", versions.toVersionString())
        assertEquals("1.0.0", versions.toInfoVersionString())
    }

    @Test
    fun multiple_prerelease_tags_same_commit() {
        // arrange
        val commits = listOf(

                "third",
                "second",
                "first"
        )
        val tags = listOf(
                Tag("v1.0.0", "first"),
                Tag("foo", "second"),
                Tag("v1.0.1-RC.2", "second"),
                Tag("bar", "second")
        )

        // act
        val versions = getVersion(commits, tags)

        // assert
        assertEquals("1.0.1", versions.toVersionString())
        assertEquals("1.0.1-RC.3+001", versions.toInfoVersionString())
    }

    @Test
    fun testDox() {
        // given
        val commits = mutableListOf("1")
        val tags = listOf(
                Tag("v1.2", "1"),
                Tag("v1.3-RC", "5"),
                Tag("v1.3", "9"))

        // when 
        var versions = getVersion(commits, tags)

        // then 
        assertEquals("1.2.0", versions.toVersionString())
        assertEquals("1.2.0", versions.toInfoVersionString())

        // given
        commits.add(0, "2")
        commits.add(0, "3")
        commits.add(0, "4")

        // when 
        versions = getVersion(commits, tags)

        // then 
        assertEquals("1.2.1", versions.toVersionString())
        assertEquals("1.2.1-SNAPSHOT+003", versions.toInfoVersionString())

        // given
        commits.add(0, "5")

        // when 
        versions = getVersion(commits, tags)

        // then 
        assertEquals("1.3.0", versions.toVersionString())
        assertEquals("1.3.0-RC", versions.toInfoVersionString())

        // given
        commits.add(0, "6")
        commits.add(0, "7")
        commits.add(0, "8")

        // when 
        versions = getVersion(commits, tags)

        // then 
        assertEquals("1.3.0", versions.toVersionString())
        assertEquals("1.3.0-RC+003", versions.toInfoVersionString())

        // given
        commits.add(0, "9")

        // when 
        versions = getVersion(commits, tags)

        // then 
        assertEquals("1.3.0", versions.toVersionString())
        assertEquals("1.3.0", versions.toInfoVersionString())
    }

    private fun getVersion(commits: List<String>, tags: List<Tag>, dirty: Boolean = false): SemVersion {
        return VersionFinder(SemverSettings(), tags.groupBy { it.sha }).getVersion(asCommits(commits).first(), dirty)
    }

    private fun asCommits(shas: List<String>): Sequence<Commit> {
        return shas.take(1).map { Commit("", it, asCommits(shas.drop(1))) }.asSequence()
    }
}