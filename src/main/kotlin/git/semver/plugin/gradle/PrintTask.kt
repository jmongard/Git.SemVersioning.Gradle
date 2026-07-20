package git.semver.plugin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@CacheableTask
abstract class PrintTask @Inject constructor(private val printout: () -> Any, desc: String, reason: String) : DefaultTask() {
    @get:OutputFile
    @get:Optional
    val file: RegularFileProperty = project.objects.fileProperty()

    private val logger = LoggerFactory.getLogger(PrintTask::class.java)

    init {
        group = GitSemverPlugin.VERSIONING_GROUP
        description = desc
        if (reason.isNotEmpty()) {
            notCompatibleWithConfigurationCache(reason)
        }
    }

    @Option(option = "file", description = "Print to a file ")
    fun setFile(file: String) {
        this.file.set(File(file).absoluteFile)
    }

    @TaskAction
    fun print() {
        if (file.isPresent) {
            writeFile(file.get().asFile.absolutePath)
        } else {
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