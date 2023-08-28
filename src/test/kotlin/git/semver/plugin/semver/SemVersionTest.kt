package git.semver.plugin.semver

import git.semver.plugin.scm.Tag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.*


class SemVersionTest {
    companion object {
        const val SHA = "8727a3e1234567"
    }

    @ParameterizedTest
    @ValueSource(strings = ["foo", "foo", "v1", "v1.", "va1,2", "av1,2", "v1.-2", "v-1,2"])
    fun `test invalid tags`(tagName: String) {
        assertNull(SemVersion.tryParse(Tag(tagName, SHA)))
    }

    @ParameterizedTest
    @CsvSource(
        "v1.2, 1, 2, 0, '', ",
        "v1.2-alpha, 1, 2, 0, 'alpha', ",
        "v1.2-alpha5, 1, 2, 0, 'alpha', 5",
        "v1.2-betaV.5, 1, 2, 0, 'betaV.', 5",
        "v1.2.0-beta.5, 1, 2, 0, 'beta.', 5",
        "v1.2.3-beta.5, 1, 2, 3, 'beta.', 5",
        "v1.2.3-5, 1, 2, 3, '', 5",
        "v1.2.3-alpha.beta, 1, 2, 3, 'alpha.beta', ",
        "1.2.3.4, 1, 2, 3, '', ",
        "v9.5.0.41-rc, 9, 5, 0, 'rc', 41"
    )
    fun `valid version tags`(
        versionTag: String,
        major: Int,
        minor: Int,
        patch: Int,
        suffix: String,
        preRelease: Int?
    ) {
        val version = assertNotNull(SemVersion.tryParse(Tag(versionTag, SHA)))

        assertThat(version.major).isEqualTo(major)
        assertThat(version.minor).isEqualTo(minor)
        assertThat(version.patch).isEqualTo(patch)
        assertThat(version.preRelease.first).isEqualTo(suffix)
        assertThat(version.preRelease.second).isEqualTo(preRelease)

    }

    @Test
    fun `test SemVer ordering`() {
        assertTrue(semVersion("v2.0") > semVersion("v1.0"))
        assertTrue(semVersion("v1.2") > semVersion("v1.1"))
        assertTrue(semVersion("v1.1.1") > semVersion("v1.1.0"))
        assertTrue(semVersion("v1.1.0") > semVersion("v1.1.0-RC"))
        assertTrue(semVersion("v1.1.0-RC.0") > semVersion("v1.1.0-RC"))
        assertTrue(semVersion("v1.1.0-Beta") > semVersion("v1.1.0-Alpha"))
        assertTrue(semVersion("v1.1.0-Beta.1") > semVersion("v1.1.0-Alpha.2"))
        assertTrue(semVersion("v1.1.0-RC2") > semVersion("v1.1.0-RC1"))
        assertTrue(semVersion("v1.1.0-RC.2") > semVersion("v1.1.0-RC.1"))
        assertTrue(semVersion("v0.2.0-beta.0") > semVersion("v0.1.1-alpha.1"))
        assertTrue(semVersion("v11.14.2-net472") > semVersion("11.14.1", 15))
        assertTrue(semVersion("v1.3.1", 1) > semVersion("v1.3.1-RC"))
    }

    private fun semVersion(v: String, commitCount: Int): SemVersion {
        val snapthot = semVersion(v)
        snapthot.commitCount += commitCount
        return snapthot
    }

    private fun semVersion(v: String) = SemVersion.tryParse(Tag(v, SHA))!!

    @Test
    fun testInfoVersionSha() {
        val actualVersion = SemVersion.tryParse(Tag("1.0.0", SHA))
        assertEquals("1.0.0+sha.8727a3e", actualVersion.toString())
    }
}