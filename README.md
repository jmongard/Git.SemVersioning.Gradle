# Git.SemVersioning.Gradle

Gradle plugin for automatically versioning a project with git.

## Usage

Apply the plugin using standard Gradle convention:

```groovy
plugins {
    id 'com.github.jmongard.git-semver-plugin' version '<current version>'
}
```

Set the version of a project by calling:

```groovy
allprojects {
	version = semver.version
}
```

## Versioning

The versioning system is designed to follow semantic versioning.


Tasks
-----
This plugin adds a `printVersion` task, which will echo the project's configured version
to standard-out.

