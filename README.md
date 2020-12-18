# Git.SemVersioning.Gradle

Gradle plugin for automatically versioning a project with git.

## Usage

Apply the plugin using standard Gradle convention:

```groovy
plugins {
    id 'com.github.jmongard.git-semver-plugin' version '<current version>'
}
```


Set the version of a project:

```groovy
version = semver.version
```

Set the version of all projects by calling:

```groovy
allprojects {
    version = semver.version
}
```

## Versioning

The versioning system is designed to follow semantic versioning as described by https://semver.org/.

It works by recursively traversing the commit tree until it finds a release tag or commit and then calculating the the new version using from there using commit messages.

The plugin will look for conventional commit messages https://www.conventionalcommits.org/ (`fix:`, `feat:`, `refactor!:`, ...). to and will increas the corresponding version number.
If no convetional commit message is found since the last release the patch number will be increased by one and add `SNAPSHOT` pre-release suffix.

### Releases

The plugin will serach the commit tree until it finds a commit it interpret as a release commit with a version number. 

* Any commit tagged with a version number will be considered to be a release.

* Any commit with commit message starting with `release:` 

The version number should consist of three numbers separated by a dot e.g. `1.0.0`. The version number does not need to be at the start of the message so `release: v1.2.3` will be match.

### Pre-releases

The plugin will find pre-releases but will continue to search the hierarchy until it finds a relase to count the commits from. If the plugin finds it is currently a pre release the version number will be increased by at most one major, minor or patch.

## Version format

The plugin will by default set use the version format `1.2.3-SNAPSHOT+001` where `001` is the commit count since last release.

Tasks
-----
This plugin adds a `printVersion` task, which will echo the project's configured version
to standard-out.

