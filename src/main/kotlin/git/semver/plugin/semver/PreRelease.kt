package git.semver.plugin.semver

data class PreRelease(val prefix: String, val number: Int?) {
    companion object {
        val noPreRelease = PreRelease("", null)

        fun parse(value: String?): PreRelease {
            if (value == null) {
                return noPreRelease
            }

            val prefix = value.trimEnd { it.isDigit() }
            return PreRelease(prefix,
                if (prefix.length < value.length)
                    value.substring(prefix.length).toInt()
                else
                    null)
        }
    }

    val isPreRelease
        get() = prefix.isNotEmpty() || number != null

    override fun toString() = prefix + (number ?: "")
}