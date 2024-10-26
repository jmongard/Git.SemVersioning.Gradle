package git.semver.plugin.changelog

/**
 * The settings and texts used to build the change log.
 */
open class ChangeLogTexts(
    /**
     * All the header texts for change log.
     * Implementations must provide values for all conventional commit types and scopes.
     * @see DefaultChangeLogTexts
     */
    val headerTexts: MutableMap<String, String>) {

    companion object {
        const val HEADER = "#"
        const val BREAKING_CHANGE = "!"
        const val OTHER_CHANGE = "?"
        const val FOOTER = "footer"
    }

    /**
     * Topmost header of the change log.
     */
    var header: String
        get() = headerTexts[HEADER].orEmpty()
        set(value) {
            headerTexts[HEADER] = value
        }

    /**
     * Footer of the change log.
     */
    var footer: String
        get() = headerTexts[FOOTER].orEmpty()
        set(value) {
            headerTexts[FOOTER] = value
        }

    /**
     * Header for breaking changes.
     */
    var breakingChange: String
        get() = headerTexts[BREAKING_CHANGE].orEmpty()
        set(value) {
            headerTexts[BREAKING_CHANGE] = value
        }

    /**
     * Header for other changes.
     */
    var otherChange: String
        get() = headerTexts[OTHER_CHANGE].orEmpty()
        set(value) {
            headerTexts[OTHER_CHANGE] = value
        }

    /**
     * List of types in order presented after breaking changes
     */
    val typesOrder: LinkedHashSet<String> = linkedSetOf("fix", "feat")

    var groupByText: Boolean = true

    var sortByText: Boolean = true

    var changeLogPattern: String = "\\A(?<Type>\\w+)(?:\\((?<Scope>[^()]+)\\))?!?:\\s*(?<Message>(?:.|\n)*)"


    var headerFormat: (ChangeLogFormatter.CommitInfo) -> String = { commitInfo ->
        (commitInfo.message ?: commitInfo.text).lineSequence().first()
    }
    var fullHeaderFormat: (ChangeLogFormatter.CommitInfo) -> String = { commitInfo ->
        commitInfo.text.lineSequence().first()
    }
    var scopeFormat: (ChangeLogFormatter.CommitInfo) -> String = { commitInfo ->
        commitInfo.scope?.let { "$it: " }.orEmpty()
    }
    var typeFormat: (ChangeLogFormatter.CommitInfo) -> String = { commitInfo ->
        commitInfo.type?.let { "$it: " }.orEmpty()
    }
    var hashFormat: (ChangeLogFormatter.CommitInfo) -> String = { commitInfo ->
        commitInfo.commits.joinToString(" ", "", " ") { it.sha }
    }
}