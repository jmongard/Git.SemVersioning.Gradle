package git.semver.plugin.changelog

import org.junit.jupiter.api.Assertions.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class ChangeLogTextFormatterTest {

    @Test
    fun `test sanitizeHtml`() {
        val actual = ChangeLogTextFormatter.sanitizeHtml("A <b> test `<html>` </b> string")

        assertThat(actual)
            .isEqualTo("A \\<b> test `<html>` \\</b> string")
    }

    @Test
    fun `test sanitizeHtml double backtick`() {
        val actual = ChangeLogTextFormatter.sanitizeHtml("A <b> test ``<`html`>`` </b> string")

        assertThat(actual)
            .isEqualTo("A \\<b> test ``<`html`>`` \\</b> string")
    }
}