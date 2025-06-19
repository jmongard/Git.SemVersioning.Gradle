package git.semver.plugin.changelog

import git.semver.plugin.changelog.ChangeLogTexts.Companion.BREAKING_CHANGE
import java.util.*

/**
 * A builder class for generating changelog content from commit information.
 * It is used in combination with ChangeLogFormatterto generate the change log.
 * 
 * This class provides functionality to filter, group, and format commit information
 * to generate a structured changelog document. It extends DocumentBuilder to
 * accumulate the generated content.
 *
 * @property groupKey The name of the group that is being processed
 * @property commitInfos The list of commit information to process
 * @property context The context that tracks which commits have been processed
 * @property constants Text constants used for formatting the changelog
 *
 * @see ChangeLogFormat for usage example
 * @see ChangeLogFormatter
 */
class ChangeLogBuilder(
    val groupKey: String? = null,
    private val commitInfos: List<ChangeLogFormatter.CommitInfo>,
    private val context: ChangeLogFormatter.Context,
    val constants: ChangeLogTexts
) : DocumentBuilder() {

    /**
     * Formats all remaining commits using the provided formatting block.
     *
     * This method applies the given formatting block to each remaining commit,
     * appends the formatted result to the document, and marks the commit as processed.
     *
     * @param block A function that configures the ChangeLogTextFormatter for each commit
     */
    fun formatChanges(block: ChangeLogTextFormatter.() -> Unit) {
        for (commitInfo in remainingCommitInfos()) {
            val formatter = ChangeLogTextFormatter(commitInfo, constants)
            formatter.block()
            append(formatter.build())
            context.flagCommit(commitInfo)
        }
    }

    /**
     * Skips all remaining commits by marking them as processed without including them in the output.
     */
    fun skip() {
        skip(remainingCommitInfos())
    }

    /**
     * Skips the specified list of commits by marking them as processed.
     *
     * @param commitInfoList The list of commits to skip
     */
    private fun skip(commitInfoList: List<ChangeLogFormatter.CommitInfo>) {
        for (commitInfo in commitInfoList) {
            context.flagCommit(commitInfo)
        }
    }

    //<editor-fold desc="filter">
    /**
     * Processes commits that contain breaking changes.
     *
     * @param block The processing block to apply to breaking change commits
     */
    fun withBreakingChanges(block: ChangeLogBuilder.() -> Unit) {
        with({ it.isBreaking }, BREAKING_CHANGE, block)
    }

    /**
     * Processes commits that match the specified scopes.
     *
     * @param scopes The scopes to filter commits by
     * @param block The processing block to apply to matching commits
     */
    fun withScope(vararg scopes: String, block: ChangeLogBuilder.() -> Unit) {
        for (scope in scopes) {
            with({ it.scope == scope }, scope, block)
        }
    }

    /**
     * Processes commits that match the specified types.
     *
     * @param types The commit types to filter by
     * @param block The processing block to apply to matching commits
     */
    fun withType(vararg types: String, block: ChangeLogBuilder.() -> Unit) {
        for (type in types) {
            with({ it.type == type }, type, block)
        }
    }

    /**
     * Processes all remaining commits that haven't been processed by other filters.
     *
     * @param block The processing block to apply to remaining commits
     */
    fun otherwise(block: ChangeLogBuilder.() -> Unit) {
        with({ true }, block)
    }

    /**
     * Processes commits that match the specified predicate.
     *
     * @param filter The predicate to filter commits by
     * @param block The processing block to apply to matching commits
     * @return The result of the processing
     */
    fun with(filter: (ChangeLogFormatter.CommitInfo) -> Boolean, block: ChangeLogBuilder.() -> Unit) =
        with(filter, null, block)

    /**
     * Processes commits that match the specified predicate with an optional group key.
     *
     * @param predicate The predicate to filter commits by
     * @param key The optional key used for grouping in the output
     * @param block The processing block to apply to matching commits
     */
    fun with(
        predicate: (ChangeLogFormatter.CommitInfo) -> Boolean,
        key: String?,
        block: ChangeLogBuilder.() -> Unit
    ) {
        val filteredCommits = remainingCommitInfos().filter(predicate)
        if (filteredCommits.isNotEmpty()) {
            processCommits(key, filteredCommits, block)
        }
    }
    //</editor-fold>

    //<editor-fold desc="grouping">
    /**
     * Groups commits by their scope and processes each group.
     *
     * The groups are sorted alphabetically by scope.
     *
     * @param block The processing block to apply to each group
     */
    fun groupByScope(block: ChangeLogBuilder.() -> Unit) {
        groupBySorted({ it.scope }, block)
    }

    /**
     * Groups commits by their type and processes each group.
     *
     * The groups are sorted alphabetically by type.
     *
     * @param block The processing block to apply to each group
     */
    fun groupByType(block: ChangeLogBuilder.() -> Unit) {
        groupBySorted({ it.type }, block)
    }

    /**
     * Groups commits by the result of the key selector function and processes each group.
     *
     * The groups maintain the order in which they were first encountered.
     *
     * @param groupingKeySelector A function that extracts the grouping key from a commit
     * @param block The processing block to apply to each group
     */
    fun groupBy(groupingKeySelector: (ChangeLogFormatter.CommitInfo) -> String?, block: ChangeLogBuilder.() -> Unit) {
        processCommitsGrouped(groupingKeySelector, LinkedHashMap(), block)
    }

    /**
     * Groups commits by the result of the key selector function and processes each group.
     *
     * The groups are sorted alphabetically by the grouping key.
     *
     * @param groupingKeySelector A function that extracts the grouping key from a commit
     * @param block The processing block to apply to each group
     */
    fun groupBySorted(
        groupingKeySelector: (ChangeLogFormatter.CommitInfo) -> String?,
        block: ChangeLogBuilder.() -> Unit
    ) {
        processCommitsGrouped(groupingKeySelector, TreeMap(), block)
    }

    /**
     * Groups commits by the result of the key selector function and processes each group.
     *
     * @param groupingKeySelector A function that extracts the grouping key from a commit
     * @param destination The map to store the grouped commits
     * @param block The processing block to apply to each group
     */
    private fun processCommitsGrouped(
        groupingKeySelector: (ChangeLogFormatter.CommitInfo) -> String?,
        destination: MutableMap<String, MutableList<ChangeLogFormatter.CommitInfo>>,
        block: ChangeLogBuilder.() -> Unit
    ) {
        val groupedCommits: Map<String, List<ChangeLogFormatter.CommitInfo>> = remainingCommitInfos()
            .mapNotNull { keyMapper(groupingKeySelector, it) }
            .groupByTo(destination, { it.first }, { it.second })

        for ((key, commits) in groupedCommits) {
            filterEmptyHeader(key, commits, block)
        }
    }

    /**
     * Filters out commits with empty headers and processes the rest.
     *
     * @param key The key for the group
     * @param block The processing block to apply
     */
    fun filterEmptyHeader(key: String?, block: ChangeLogBuilder.() -> Unit) {
        filterEmptyHeader(key ?: "", remainingCommitInfos(), block)
    }

    /**
     * Filters out commits with empty headers and processes the rest.
     *
     * @param key The key for the group
     * @param commits The list of commits to process
     * @param block The processing block to apply
     */
    private fun filterEmptyHeader(
        key: String,
        commits: List<ChangeLogFormatter.CommitInfo>,
        block: ChangeLogBuilder.() -> Unit
    ) {
        if (key.isEmpty()) {
            skip(commits)
            return
        }
        processCommits(key, commits, block)
    }

    /**
     * Maps a commit to a key-commit pair using the provided key selector.
     *
     * @param keySelector A function that extracts the key from a commit
     * @param it The commit to map
     * @return A pair of the key and commit, or null if the key is null
     */
    private fun keyMapper(
        keySelector: (ChangeLogFormatter.CommitInfo) -> String?,
        it: ChangeLogFormatter.CommitInfo
    ): Pair<String, ChangeLogFormatter.CommitInfo>? {
        return (keySelector(it) ?: return null) to it
    }

    /**
     * Processes a list of commits with the given key and block.
     *
     * @param key The key for the group
     * @param commits The list of commits to process
     * @param block The processing block to apply
     */
    private fun processCommits(
        key: String?,
        commits: List<ChangeLogFormatter.CommitInfo>,
        block: ChangeLogBuilder.() -> Unit
    ) {
        val builder = ChangeLogBuilder(key, commits, context, constants)
        block(builder)
        append(builder.build())
    }

    //</editor-fold>

    /**
     * Returns a list of commits that have not been processed yet.
     *
     * @return A filtered list of commits that have not been flagged as processed
     */
    fun remainingCommitInfos() = commitInfos.filter { !context.isCommitFlagged(it) }
}

/**
 * A formatter for individual commit entries in the changelog.
 *
 * This class provides methods to format different parts of a commit entry,
 * such as the header, scope, type, and body.
 *
 * @property commitInfo The commit information to format
 * @property constants Text constants used for formatting
 */
class ChangeLogTextFormatter(
    val commitInfo: ChangeLogFormatter.CommitInfo,
    val constants: ChangeLogTexts
) : DocumentBuilder() {
    companion object {
        /**
         * Sanitizes HTML content in a string by escaping '<' characters.
         *
         * This method preserves code blocks enclosed in backticks without escaping
         * their content, which allows for including code examples in the changelog.
         *
         * @param str The string to sanitize
         * @return The sanitized string with HTML characters escaped
         */
        fun sanitizeHtml(str: String): String {
            if (str.indexOf('<') == -1) {
                return str
            }

            val result = StringBuilder(str.length)
            var backtickCount = 0
            var index = 0
            while (index < str.length) {
                val token = str[index]
                when {
                    token == '`' -> backtickCount += 1

                    backtickCount > 0 -> {
                        val end = str.indexOf("`".repeat(backtickCount), index) + backtickCount
                        if (end > index) {
                            result.append(str, index, end)
                            index = end
                        }
                        backtickCount = 0
                        continue
                    }

                    token == '<' -> result.append("\\")
                }
                result.append(token)
                index += 1
            }

            return result.toString()
        }
    }

    /**
     * Returns the formatted header for the commit.
     *
     * @return The sanitized header text
     */
    fun header() = sanitizeHtml(constants.headerFormat(commitInfo))

    /**
     * Returns the full formatted header for the commit.
     *
     * @return The sanitized full header text
     */
    fun fullHeader() = sanitizeHtml(constants.fullHeaderFormat(commitInfo))

    /**
     * Returns the formatted scope of the commit.
     *
     * @return The scope text
     */
    fun scope() = constants.scopeFormat(commitInfo)

    /**
     * Returns the formatted type of the commit.
     *
     * @return The type text
     */
    fun type() = constants.typeFormat(commitInfo)

    /**
     * Returns the formatted hash of the commit.
     *
     * @return The hash text
     */
    fun hash() = constants.hashFormat(commitInfo)

    /**
     * Returns the formatted author name(s) of the commit.
     *
     * @param format The format string to use for the author name
     * @return The formatted author name(s)
     */
    fun authorName(format: String = "%s") =
        commitInfo.commits.map{ format.format(it.authorName) }.distinct().joinToString(" ", "", "")

    /**
     * Returns the formatted author name(s) and email(s) of the commit.
     *
     * @param format The format string to use for the author name and email
     * @return The formatted author name(s) and email(s)
     */
    fun authorNameAndEmail(format: String = "%s [%s]") =
        commitInfo.commits.map { format.format(it.authorName, it.authorEmail)}.distinct().joinToString(" ", "", "")

    /**
     * Returns the sanitized body of the commit.
     *
     * This method extracts the body text from the commit message,
     * skipping the first line (which is the header) and any empty lines,
     * and stopping at the first empty line after the body.
     *
     * @return The sanitized body text as a sequence of lines
     */
    fun body() = sanitizeHtml(commitInfo.text).lineSequence()
        .drop(1)
        .dropWhile { it.isEmpty() }
        .takeWhile { it.isNotEmpty() }
}

/**
 * A base class for building document content.
 *
 * This class provides methods to accumulate text content and build a final string.
 * It is used as a base class for more specialized builders like ChangeLogBuilder.
 */
open class DocumentBuilder {
    private val out = StringBuilder()

    /**
     * Builds and returns the accumulated document content as a string.
     *
     * @return The complete document content
     */
    fun build(): String {
        return out.toString()
    }

    /**
     * Appends text to the document.
     *
     * @param t The text to append
     * @return This builder instance for method chaining
     */
    fun append(t: String?): DocumentBuilder {
        out.append(t)
        return this
    }

    /**
     * Appends text followed by a line break to the document.
     *
     * @param t The text to append, defaults to an empty string
     * @return This builder instance for method chaining
     */
    fun appendLine(t: String? = ""): DocumentBuilder {
        out.appendLine(t)
        return this
    }
}
