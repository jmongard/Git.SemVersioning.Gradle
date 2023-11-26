package git.semver.plugin.changelog

/**
 * The texts used to build the change log.
 */
interface ChangeLogTexts {
     companion object {
        const val HEADER = "#"
        const val BREAKING_CHANGE = "!"
        const val OTHER_CHANGE = "?"
    }
    /**
     * All the header texts for change log.
     * Implementations must provide values for all conventional commit types and scopes.
     * @see DefaultChangeLogTexts
     */
    val headerTexts: MutableMap<String, String>

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
        get() = headerTexts["footer"].orEmpty()
        set(value) {
            headerTexts["footer"] = value
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
}