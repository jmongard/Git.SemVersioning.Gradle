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
        assertThat(SemVersion.tryParse(Tag(tagName, SHA))).isNull()
    }

    @ParameterizedTest
    @CsvSource(
        "v1.2, 1, 2, 0, '', , false",
        "v1.2-alpha, 1, 2, 0, 'alpha', , true",
        "v1.2-alpha5, 1, 2, 0, 'alpha', 5, true",
        "v1.2-betaV.5, 1, 2, 0, 'betaV.', 5, true",
        "v1.2.0-beta.5, 1, 2, 0, 'beta.', 5, true",
        "v1.2.3-beta.5, 1, 2, 3, 'beta.', 5, true",
        "v1.2.3.4-beta, 1, 2, 3, 'beta', 4, true",
        "v1.2.3-5, 1, 2, 3, '', 5, true",
        "v1.2.3-alpha.beta, 1, 2, 3, 'alpha.beta', , true",
        "1.2.3.4, 1, 2, 3, '', , false",
        "v9.5.0.41-rc, 9, 5, 0, 'rc', 41, true"
    )
    fun `valid version tags`(
        versionTag: String,
        major: Int,
        minor: Int,
        patch: Int,
        suffix: String,
        preRelease: Int?,
        isPreRelease: Boolean
    ) {
        val version = assertNotNull(SemVersion.tryParse(Tag(versionTag, SHA)))

        assertThat(version.major).isEqualTo(major)
        assertThat(version.minor).isEqualTo(minor)
        assertThat(version.patch).isEqualTo(patch)
        assertThat(version.preRelease.prefix).isEqualTo(suffix)
        assertThat(version.preRelease.number).isEqualTo(preRelease)
        assertThat(version.isPreRelease).isEqualTo(isPreRelease)
    }

    @ParameterizedTest
    @CsvSource(
        "v2.0, 0, v1.0, 0",
        "v1.2, 0, v1.1, 0",
        "v1.1.1, 0, v1.1.0, 0",
        "v1.1.0, 0, v1.1.0-RC, 0",
        "v1.1.0-RC.0, 0, v1.1.0-RC, 0",
        "v1.1.0-Beta, 0, v1.1.0-Alpha, 0",
        "v1.1.0-Beta.1, 0, v1.1.0-Alpha.2, 0",
        "v1.1.0-RC2, 0, v1.1.0-RC1, 0",
        "v1.1.0-RC.2, 0, v1.1.0-RC.1, 0",
        "v0.2.0-beta.0, 0, v0.1.1-alpha.1, 0",
        "v11.14.2-net472, 15, 11.14.1, 0",
        "v1.3.1, 1, v1.3.1-RC, 0",
        "v1.3.1, 2, v1.3.1, 1"
    )
    fun `test SemVer ordering`(version1: String, c1: Int, version2: String, c2: Int) {
        val a = SemVersion.tryParse(Tag(version1, SHA))!!
        val b = SemVersion.tryParse(Tag(version2, SHA))!!
        a.commitCount += c1
        b.commitCount += c2

        assertThat(a).isGreaterThan(b)
    }

    @Test
    fun testInfoVersionSha() {
        val actualVersion = SemVersion.tryParse(Tag("1.0.0", SHA))
        assertThat(actualVersion).hasToString("1.0.0+sha.8727a3e")
    }

    @ParameterizedTest
    @CsvSource(value = [
        "1.2.3-SNAPSHOT, false, 1.2.3-SNAPSHOT",
        "1.2.3-SNAPSHOT, true, 1.2.3-SNAPSHOT",
        "1.0.0-Alpha.1, false, 1.0.0-Alpha1",
        "1.0.0-Alpha.1, true, 1.0.0-Alpha.1",
        "1.2.3, false, 1.2.3",
        "1.2.3, true, 1.2.3"
    ])
    fun toInfoVersionString(version: String, v2: Boolean, expected: String) {
        val actualVersion = SemVersion.tryParse(Tag(version, SHA))
        assertThat(actualVersion?.toInfoVersionString(v2 = v2)).isEqualTo(expected)
    }

    @Test
    fun toInfoVersionString_preReleaseLast() {
        val version = SemVersion.tryParse(Tag("1.2.3-SNAPSHOT", SHA))
        assertThat(version?.toInfoVersionString(shaLength = 7, appendPreReleaseLast = true))
            .isEqualTo("1.2.3+sha.8727a3e-SNAPSHOT")
    }
}