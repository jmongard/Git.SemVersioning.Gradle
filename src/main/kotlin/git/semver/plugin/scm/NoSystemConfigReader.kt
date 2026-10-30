package git.semver.plugin.scm

import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader

/**
 * Workaround for JGit starting the external git executable to locate the system configuration file
 * (https://github.com/eclipse-jgit/jgit/issues/24). External processes are not allowed at configuration
 * time when the Gradle configuration cache is enabled. The downside is that settings in the system
 * configuration file are ignored while the workaround is active.
 */
internal class NoSystemConfigReader(delegate: SystemReader) : SystemReader.Delegate(delegate) {
    companion object {
        private val lock = Any()
        private var activeCount = 0
        private var originalReader: SystemReader? = null

        internal fun <T> use(block: () -> T): T {
            synchronized(lock) {
                if (activeCount++ == 0) {
                    val reader = getInstance()
                    originalReader = reader
                    setInstance(NoSystemConfigReader(reader))
                }
            }
            try {
                return block()
            } finally {
                synchronized(lock) {
                    if (--activeCount == 0) {
                        setInstance(originalReader)
                        originalReader = null
                    }
                }
            }
        }
    }

    override fun openSystemConfig(parent: Config?, fs: FS?): FileBasedConfig {
        return object : FileBasedConfig(parent, null, fs) {
            override fun load() {
                // empty, do not load
            }

            override fun isOutdated() = false
        }
    }
}
