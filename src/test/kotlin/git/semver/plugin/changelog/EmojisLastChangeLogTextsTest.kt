package git.semver.plugin.changelog

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class EmojisLastChangeLogTextsTest {

    @Test
    fun `has same texts heder text keys as PlainChangeLogTexts`() {
        assertThat(EmojisLastChangeLogTexts.headerTexts)
            .containsOnlyKeys(PlainChangeLogTexts.headerTexts.keys)
    }
}