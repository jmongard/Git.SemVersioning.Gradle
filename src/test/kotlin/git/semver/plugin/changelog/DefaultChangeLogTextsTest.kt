package git.semver.plugin.changelog

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class DefaultChangeLogTextsTest {
    @Test
    fun header() {
        val oldValue = DefaultChangeLogTexts.header

        DefaultChangeLogTexts.header = "HEADER"

        assertThat(DefaultChangeLogTexts.headerTexts[ChangeLogTexts.HEADER])
            .isEqualTo("HEADER")
            .isEqualTo(DefaultChangeLogTexts.header)
            .isNotEqualTo(oldValue);
    }

    @Test
    fun footer() {
        val oldValue = DefaultChangeLogTexts.footer

        DefaultChangeLogTexts.footer = "FOOTER"

        assertThat(DefaultChangeLogTexts.headerTexts[ChangeLogTexts.FOOTER])
            .isEqualTo("FOOTER")
            .isEqualTo(DefaultChangeLogTexts.footer)
            .isNotEqualTo(oldValue);
    }

    @Test
    fun breakingChange() {
        val oldValue = DefaultChangeLogTexts.breakingChange

        DefaultChangeLogTexts.breakingChange = "BREAKING_CHANGE"

        assertThat(DefaultChangeLogTexts.headerTexts[ChangeLogTexts.BREAKING_CHANGE])
            .isEqualTo("BREAKING_CHANGE")
            .isEqualTo(DefaultChangeLogTexts.breakingChange)
            .isNotEqualTo(oldValue);
    }

    @Test
    fun otherChange() {
        val oldValue = DefaultChangeLogTexts.otherChange

        DefaultChangeLogTexts.otherChange = "OTHER_CHANGE"

        assertThat(DefaultChangeLogTexts.headerTexts[ChangeLogTexts.OTHER_CHANGE])
            .isEqualTo("OTHER_CHANGE")
            .isEqualTo(DefaultChangeLogTexts.otherChange)
            .isNotEqualTo(oldValue);
    }

    @Test
    fun typesOrder() {
        val actual = DefaultChangeLogTexts.typesOrder

        assertThat(actual).containsExactly("fix", "feat")
    }
}