package git.semver.plugin.changelog

open class DocumentBuilder {
    private val out = StringBuilder()

    fun build(): String {
        return out.toString()
    }

    fun append(t: String?): DocumentBuilder {
        out.append(t)
        return this
    }

    fun appendLine(t: String? = ""): DocumentBuilder {
        out.appendLine(t)
        return this
    }
}