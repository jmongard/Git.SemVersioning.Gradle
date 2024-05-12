package git.semver.plugin.changelog

import org.junit.jupiter.api.Assertions.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class ChangeLogTextFormatterTest {
    @Test
    fun `test sanitizePLainText`() {
        val actual = ChangeLogTextFormatter.sanitizeHtml("A test string")

        assertThat(actual)
            .isEqualTo("A test string")
    }

    @Test
    fun `test sanitizeHtml hole string`() {
        val actual = ChangeLogTextFormatter.sanitizeHtml("`A <b> test <html> </b> string`")

        assertThat(actual)
            .isEqualTo("`A <b> test <html> </b> string`")
    }

  @Test
    fun `test sanitizeHtml hole string bad`() {
        val actual = ChangeLogTextFormatter.sanitizeHtml("```````A <b> test <html> </b> string")

        assertThat(actual)
            .isEqualTo("```````A \\<b> test \\<html> \\</b> string")
    }

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

    @Test
    fun `test sanitizeHtml triple backtick`() {
        val actual = ChangeLogTextFormatter.sanitizeHtml("A <b> test ```<`html`>``` </b> string")

        assertThat(actual)
            .isEqualTo("A \\<b> test ```<`html`>``` \\</b> string")
    }

    @Test
    fun `test sanitizeHtml bad backtick`() {
        val actual = ChangeLogTextFormatter.sanitizeHtml("A <b> test ```<`html`>`` </b> string")

        assertThat(actual)
            .isEqualTo("A \\<b> test ```\\<`html`>`` \\</b> string")
    }
}