package git.semver.plugin.semver

import git.semver.plugin.scm.Tag
import kotlin.test.*


class SemVersionTest {
    companion object {
        const val SHA = "8727a3e1234567"
    }

    @Test
    fun `test invalid tags`() {
        assertNull(SemVersion.tryParse(Tag("foo", SHA)))
        assertNull(SemVersion.tryParse(Tag("foo", SHA)))
        assertNull(SemVersion.tryParse(Tag("v1", SHA)))
        assertNull(SemVersion.tryParse(Tag("v1.", SHA)))
        assertNull(SemVersion.tryParse(Tag("va1,2", SHA)))
        assertNull(SemVersion.tryParse(Tag("av1,2", SHA)))
        assertNull(SemVersion.tryParse(Tag("v1.-2", SHA)))
        assertNull(SemVersion.tryParse(Tag("v-1,2", SHA)))
    }

    @Test
    fun `valid version tags`() {
        checkValid("v1.2", 1, 2, 0, "", null)
        checkValid("v1.2-alpha", 1, 2, 0, "alpha", null)
        checkValid("v1.2-alpha5", 1, 2, 0, "alpha", 5)
        checkValid("v1.2-betaV.5", 1, 2, 0, "betaV.", 5)
        checkValid("v1.2.0-beta.5", 1, 2, 0, "beta.", 5)
        checkValid("v1.2.3-beta.5", 1, 2, 3, "beta.", 5)
        checkValid("v1.2.3-5", 1, 2, 3, "", 5)
        checkValid("v1.2.3-alpha.beta", 1, 2, 3, "alpha.beta", null)
        checkValid("1.2.3.4", 1, 2, 3, "", null)
        checkValid("v9.5.0.41-rc", 9, 5, 0, "rc", 41)
    }

    private fun checkValid(tagName: String, majorVersion: Int, minorVersion: Int, patchVersion: Int, suffix: String?, preRelease: Int?) {
        val version = assertNotNull(SemVersion.tryParse(Tag(tagName, SHA)))
        assertEquals(majorVersion, version.major)
        assertEquals(minorVersion, version.minor)
        assertEquals(patchVersion, version.patch)
        assertEquals(suffix, version.preReleasePrefix)
        assertEquals(preRelease, version.preReleaseVersion)
    }

    @Test
    fun `test SemVer ordering`() {
        assertTrue(SemVersion.tryParse(Tag("v2.0", SHA))!! > SemVersion.tryParse(Tag("v1.0", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.2", SHA))!! > SemVersion.tryParse(Tag("v1.1", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.1", SHA))!! > SemVersion.tryParse(Tag("v1.1.0", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-RC", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0-RC.0", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-RC", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0-Beta", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-Alpha", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0-Beta.1", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-Alpha.2", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0-RC2", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-RC1", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v1.1.0-RC.2", SHA))!! > SemVersion.tryParse(Tag("v1.1.0-RC.1", SHA))!!)
        assertTrue(SemVersion.tryParse(Tag("v0.2.0-beta.0", SHA))!! > SemVersion.tryParse(Tag("v0.1.1-alpha.1", SHA))!!)
    }

    @Test
    fun testInfoVersionSha() {
        val actualVersion = SemVersion.tryParse(Tag("1.0.0", SHA))
        assertEquals("1.0.0+sha.8727a3e", actualVersion.toString())
    }
}