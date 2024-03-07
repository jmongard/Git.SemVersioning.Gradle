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
            .isNotEqualTo(oldValue)

        //---------------

        DefaultChangeLogTexts.headerTexts.remove(ChangeLogTexts.HEADER)

        assertThat(DefaultChangeLogTexts.header).isEmpty()

        //---------------

        DefaultChangeLogTexts.header = oldValue
    }

    @Test
    fun footer() {
        val oldValue = DefaultChangeLogTexts.footer

        DefaultChangeLogTexts.footer = "FOOTER"

        assertThat(DefaultChangeLogTexts.headerTexts[ChangeLogTexts.FOOTER])
            .isEqualTo("FOOTER")
            .isEqualTo(DefaultChangeLogTexts.footer)
            .isNotEqualTo(oldValue)

        //---------------

        DefaultChangeLogTexts.headerTexts.remove(ChangeLogTexts.FOOTER)

        assertThat(DefaultChangeLogTexts.footer).isEmpty()

        //---------------

//        DefaultChangeLogTexts.footer = oldValue
    }

    @Test
    fun breakingChange() {
        val oldValue = DefaultChangeLogTexts.breakingChange


        DefaultChangeLogTexts.breakingChange = "BREAKING_CHANGE"

        assertThat(DefaultChangeLogTexts.headerTexts[ChangeLogTexts.BREAKING_CHANGE])
            .isEqualTo("BREAKING_CHANGE")
            .isEqualTo(DefaultChangeLogTexts.breakingChange)
            .isNotEqualTo(oldValue)

        //---------------

        DefaultChangeLogTexts.headerTexts.remove(ChangeLogTexts.BREAKING_CHANGE)

        assertThat(DefaultChangeLogTexts.breakingChange).isEmpty()

        //---------------

        DefaultChangeLogTexts.breakingChange = oldValue
    }

    @Test
    fun otherChange() {
        val oldValue = DefaultChangeLogTexts.otherChange

        DefaultChangeLogTexts.otherChange = "OTHER_CHANGE"

        assertThat(DefaultChangeLogTexts.headerTexts[ChangeLogTexts.OTHER_CHANGE])
            .isEqualTo("OTHER_CHANGE")
            .isEqualTo(DefaultChangeLogTexts.otherChange)
            .isNotEqualTo(oldValue)

        //---------------

        DefaultChangeLogTexts.headerTexts.remove(ChangeLogTexts.OTHER_CHANGE)

        assertThat(DefaultChangeLogTexts.otherChange).isEmpty()

        //---------------

        DefaultChangeLogTexts.otherChange = oldValue
    }

    @Test
    fun typesOrder() {
        DefaultChangeLogTexts.typesOrder.add("test")

        assertThat(DefaultChangeLogTexts.typesOrder)
            .containsExactly("fix", "feat", "test")
    }

    @Test
    fun `has same texts heder text keys as PlainChangeLogTexts`() {
        assertThat(DefaultChangeLogTexts.headerTexts)
            .containsOnlyKeys(PlainChangeLogTexts.headerTexts.keys)
    }
}