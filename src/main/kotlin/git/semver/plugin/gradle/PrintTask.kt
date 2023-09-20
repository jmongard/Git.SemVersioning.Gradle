package git.semver.plugin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import java.nio.charset.StandardCharsets
import javax.inject.Inject

open class PrintTask @Inject constructor(private val prop: () -> Any, desc: String) : DefaultTask() {
    private var file:String? = null

    init {
        group = GitSemverPlugin.VERSIONING_GROUP
        description = desc
    }

    @Option(option = "file", description = "Print to a file ")
    fun setFile(file: String) {
        this.file = file
    }

    @TaskAction
    fun print() {
        val fileName = this.file;
        if (fileName != null) {
            File(fileName).writeText(prop().toString(), StandardCharsets.UTF_8)
        }
        else {
            println(prop().toString())
        }
    }
}