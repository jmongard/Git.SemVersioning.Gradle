package git.semver.plugin.semver

class ChangeLogSettings(
    var header: String? = null,
    var breakingChangeHeader: String? = null,
    var otherChangeHeader: String? = null,
    var missingTypeHeader: String? = null,
    var headerTexts: MutableMap<String, String> = mutableMapOf(),
    var changePrefix: String = "",
    var changePostfix: String = "",
    var changeLineSeparator: String? = null,
    var changeShaLength: Int = 7,
    var groupStart: String? = null,
    var groupEnd: String? = null,
    var footer: String? = null
)
{

}