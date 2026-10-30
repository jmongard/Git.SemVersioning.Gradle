package git.semver.plugin.scm

import org.assertj.core.api.Assertions.*
import org.eclipse.jgit.util.SystemReader
import kotlin.test.Test

class NoSystemConfigReaderTest {

    @Test
    fun `system reader is replaced while active and restored afterwards`() {
        val original = SystemReader.getInstance()

        NoSystemConfigReader.use {
            assertThat(SystemReader.getInstance()).isInstanceOf(NoSystemConfigReader::class.java)
            NoSystemConfigReader.use {
                assertThat(SystemReader.getInstance()).isInstanceOf(NoSystemConfigReader::class.java)
            }
            assertThat(SystemReader.getInstance()).isInstanceOf(NoSystemConfigReader::class.java)
            assertThat(SystemReader.getInstance().systemConfig.toText()).isEmpty()
        }

        assertThat(SystemReader.getInstance()).isSameAs(original)
    }

    @Test
    fun `system reader is restored when block throws`() {
        val original = SystemReader.getInstance()

        assertThatThrownBy {
            NoSystemConfigReader.use { error("fail") }
        }.isInstanceOf(IllegalStateException::class.java)

        assertThat(SystemReader.getInstance()).isSameAs(original)
    }
}
