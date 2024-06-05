package git.semver.plugin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

open class PrintTask @Inject constructor(private val printout: () -> Any, desc: String) : DefaultTask() {
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
        val fileName = this.file
        if (fileName != null) {
            val path = Paths.get(fileName)
            path.parent.createDirectories()
            path.writeText(printout().toString(), StandardCharsets.UTF_8)
        }
        else {
            println(printout())
        }
    }
}