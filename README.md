# Git.SemVersioning.Gradle ![Gradle Build](https://github.com/jmongard/Git.SemVersioning.Gradle/workflows/Gradle%20Build/badge.svg)

Gradle plugin for automatically versioning a project with git. 


## Usage

Apply the plugin using standard Gradle convention and set the version of the project:

```groovy
plugins {
    id 'com.github.jmongard.git-semver-plugin' version '<current version>'
}

//semver { ... } // Add optionally to configure the plugin before getting the version

//To set the version for the current project only:
version = semver.version

//Or set set the version of all projects:
allprojects {
    version = semver.version
}
```


## Versioning

The versioning system is designed to follow semantic versioning as described by https://semver.org/.

It works by recursively traversing the commit tree until it finds a release tag or commit and then calculating the new 
version using from there using commit messages.

The plugin will look for [conventional commit](https://www.conventionalcommits.org/) messages (`fix:`, `feat:`, `refactor!:`, ...) 
and will increase the corresponding version number. The major, minor or patch number will be grouped together so that 
the version increases by at most one compared to the previous release that is not a pre-release.


### Releases

The plugin will search the commit tree until it finds a commit it interprets as a release commit with a version number. 

* Any commit tagged with a version number will be considered to be a release.
* Any commit with commit message starting with `release:` 

The version number should consist of three numbers separated by a dot e.g. `1.0.0`. The version number does not need to 
be at the start of the message so `release: v1.2.3` will be match.


### Uncommited changes or non release commits

If no version changed has been triggered by any commit messages since the last release 
then the patch number will be increased by one.

If the current version is not a pre-release then `-SNAPSHOT` will be added.


## Version format

The semantic version is accessible using the `semver` extension. 
There is several options for getting the version: 

| semver extension property        | release tagged commit  | release with local changes | one commits ahead of pre-release alpha.1 |
|----------------------------------|------------------------|----------------------------| -----------------------------------------|
| **semver.version**               | 1.0.1                  | 1.0.2-SNAPSHOT             | 2.0.0-alpha.2                            |
| **semver.infoVersion**           | 1.0.1                  | 1.0.2-SNAPSHOT             | 2.0.0-alpha.2+001                        |
| **semver.semVersion.toString()** | 1.0.1+sha.1c792d5      | 1.0.2-SNAPSHOT+sha.1c792d5 | 2.0.0-alpha.2+001.sha.1c792d5            |

### Custom version format

There is also the possibility to customize the version string returned using:
`semver.semVersion.toInfoVersionString(commitCountStringFormat: String, shaLength: Int, version2: Boolean)`

* semver.version == semver.semVersion.toInfoVersionString("", 0, true)
* semver.infoVersion == semver.semVersion.toInfoVersionString("%03d", 0, true)
* semver.semVersion.toString() == semver.semVersion.toInfoVersionString("%03d", 7, true)


## Tasks

## `printVersion`
This plugin adds a `printVersion` task, which will echo the project's calculated version
to standard-out using the long format (e.g. 1.0.0-beta.3+004.sha.1c792d5).

## `releaseVersion`
The `releaseVersion` task will by default create both a release commit, and a release tag. The releaseVersion task will 
fail with an error if there exists local modification. It is possible to change this behaviour with the following options:

 * **--no-tag**: skip creating a tag
 * **--no-commit**: skipp creating a commit
 * **--no-dirty**: skipp dirty check
 * **--message**="a message": Add a message text to the tag and/or commit
 * **--preRelease**="pre-release": Change the current pre-release. An empty string will promote a pre-release to a release.
   e.g. --preRelease=alpha.1

## Example of how version is calculated

| Commit Text                    | Calculated version   |  Using release task: gradle ...       |
| ------------------------------ | -------------------- | ------------------------------------- |
| Initial commit                 | 0.0.1-SNAPSHOT+001   |                                       |
| release: v0.0.1                | 0.0.1                | releaseVersion                        |
| some changes                   | 0.0.2-SNAPSHOT+001   |                                       |
| release: v0.0.2                | 0.0.2                | releaseVersion                        |
| fix: a fix                     | 0.0.3-SNAPSHOT+001   |                                       |
| fix: another fix               | 0.0.3-SNAPSHOT+002   |                                       |
| release: v0.0.3                | 0.0.3                | releaseVersion                        |
| feat: a feature                | 0.1.0-SNAPSHOT+001   |                                       |
| feat: another feature          | 0.1.0-SNAPSHOT+002   |                                       |
| feat!: breaking feature        | 1.0.0-SNAPSHOT+003   |                                       |
| some changes                   | 1.0.0-SNAPSHOT+004   |                                       |
| feat: changes                  | 1.0.0-SNAPSHOT+005   |                                       |
| release: v1.0.0                | 1.0.0                | releaseVersion                        |
| some changes                   | 1.0.1-SNAPSHOT+001   |                                       |
| release: v1.0.1-alpha.1        | 1.0.1-alpha.1        | releaseVersion --preRelease="alpha.1" |
| some changes                   | 1.0.1-alpha.2+001    |                                       |
| release: v1.0.1-alpha.2        | 1.0.1-alpha.2        | releaseVersion                        |
| fix: a fix                     | 1.0.1-alpha.3+001    |                                       |
| fix: another fix               | 1.0.1-alpha.3+002    |                                       |
| feat: a feature                | 1.1.0-alpha.1+003    |                                       |
| release: v1.1.0-alpha.1        | 1.1.0-alpha.1        | releaseVersion                        |
| feat: another feature          | 1.1.0-alpha.2+001    |                                       |
| feat!: breaking feature        | 2.0.0-alpha.1+002    |                                       |
| release: v2.0.0                | 2.0.0                | releaseVersion --preRelease=""        |


## Configuration

The plugin can be configured using the `semver` extension. This needs to be done before retrieving the version:

```groovy
semver {
    defaultPreRelease = "SNAPSHOT"
    releasePattern = "\\Arelease(?:\\(\\w+\\))?:"
    majorPattern = "\\A\\w+(?:\\(\\w+\\))?!:|^BREAKING[ -]CHANGE:"
    minorPattern = "\\Afeat(?:\\(\\w+\\))?:"
    patchPattern = "\\Afix(?:\\(\\w+\\))?:"
    releaseCommitTextFormat = "release: v%s\n\n%s"
    releaseTagNameFormat = "%s"
}

//Remember to retrieve the version after plugin has been configured
version = semver.version
```

* **defaultPreRelease**: sets the default pre-release to use if there are commits or local modifications.
* **releasePattern**: used to identify commits thar are used as release markers.
* **majorPattern, minorPattern and patchPattern**: used to identify conventional commits used for increasing version.
* **releaseCommitTextFormat**: Pattern used by releaseVersion task for creating release commits. First parameter
  is the version and second parameter is the message (if given using --message=).
* **releaseTagNameFormat**: Pattern used by releaseVersion task for creating release tags e.g. `"v%s"` to prefix version
  tags with "v".

Patterns is matched using [java regular expressions](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) 
with IGNORE_CASE and MULTILINE options enabled.


# Resources

* [Angular Git Commit Guidelines](https://github.com/angular/angular.js/blob/master/DEVELOPERS.md#-git-commit-guidelines)