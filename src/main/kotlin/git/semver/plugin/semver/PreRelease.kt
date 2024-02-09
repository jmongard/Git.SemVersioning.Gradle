package git.semver.plugin.semver

data class PreRelease(val prefix: String, val number: Int?) {
    companion object {
        val noPreRelease = PreRelease("", null)
    }

    val isPreRelease
        get() = prefix.isNotEmpty() || number != null

    override fun toString() = prefix + (number ?: "")
}