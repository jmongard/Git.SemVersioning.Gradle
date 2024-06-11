package git.semver.plugin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

open class PrintTask @Inject constructor(private val printout: () -> Any, desc: String) : DefaultTask() {
    private var file:String? = null
    private val logger = LoggerFactory.getLogger(PrintTask::class.java)

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
            writeFile(fileName)
        }
        else {
            println(printout())
        }
    }

    private fun writeFile(fileName: String) {
        val path = Paths.get(fileName).toAbsolutePath()
        path.parent.createDirectories()
        logger.info("Writing to $path")
        path.writeText(printout().toString(), StandardCharsets.UTF_8)
    }
}