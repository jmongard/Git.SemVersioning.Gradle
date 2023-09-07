# Semantic versioning for Gradle using Git 
[![Gradle Build](https://github.com/jmongard/Git.SemVersioning.Gradle/workflows/Gradle%20Build/badge.svg)](https://github.com/jmongard/Git.SemVersioning.Gradle/actions/workflows/gradle-push.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jmongard_Git.SemVersioning.Gradle&metric=alert_status)](https://sonarcloud.io/dashboard?id=jmongard_Git.SemVersioning.Gradle)

Gradle plugin for automatically versioning a project with git using semantic versioning and conventional commits.


## Usage

Apply the plugin using standard Gradle convention and set the version of the project:

```groovy
plugins {
    id 'com.github.jmongard.git-semver-plugin' version '<current version>'
}

//semver { ... } // Optionally add configuration for the plugin before getting the version

//Set the version for the current project:
version = semver.version

//Or in a multi project build set the version of all projects (only include the plugin in the root project):
def ver = semver.version
allprojects {
    version = ver
}
```

[For the latest published version see the plugins page at Gradle.org](https://plugins.gradle.org/plugin/com.github.jmongard.git-semver-plugin)


## Versioning

The versioning system is designed to follow semantic versioning as described by https://semver.org/.

It works by recursively traversing the commit tree until it finds a version tag or release commit and then calculating 
the new version using from there using commit messages.

The plugin will look for [conventional commit](https://www.conventionalcommits.org/) messages (`fix:`, `feat:`, `refactor!:`, ...) 
and will increase the corresponding version number.

The plugin has the opinion that you want to group several fixes/features or breaking changes into a single release. 
Therefore, the major, minor or patch number will be increases by at most one compared to the previous release that is 
not a pre-release version. Set property `groupVersionIncrements = false` if you don't want the version changes to be combined.
(See [Configuration](#Configuration) reference below.)

### Releases

The plugin will search the commit tree until it finds a commit it interprets as a release commit with a version number. 

* Any commit tagged with a version number will be considered to be a release.
* Any commit with commit message starting with `release:` 

The version number should consist of three numbers separated by a dot e.g. `1.0.0`. The version number does not need to 
be at the start of the message e.g. `release: v1.2.3` will be matched.


### Uncommited changes or non release commits

If no version changed has been triggered by any commit messages since the last release 
then the patch number will be increased by one.

If the current version is not a pre-release then `-SNAPSHOT` will be added.


## Version format

The semantic version is accessible using the `semver` extension. 
There is several options for getting the version: 

| semver extension property        | example release tagged commit  | example release with local changes | example one commits ahead of pre-release alpha.1 |
|----------------------------------|--------------------------------|------------------------------------| -------------------------------------------------|
| `semver.version`                 | 1.0.1                          | 1.0.2-SNAPSHOT                     | 2.0.0-alpha.2                                    |
| `semver.infoVersion`             | 1.0.1                          | 1.0.2-SNAPSHOT                     | 2.0.0-alpha.2+001                                |
| `semver.semVersion.toString()`   | 1.0.1+sha.1c792d5              | 1.0.2-SNAPSHOT+sha.1c792d5         | 2.0.0-alpha.2+001.sha.1c792d5                    |


### Custom version format

There is also the possibility to customize the version string returned using:
`semver.semVersion.toInfoVersionString(commitCountStringFormat: String, shaLength: Int, version2: Boolean)`

If Version2 flag is set to false, then semVer version one will be used stripping any non alpha-numeric characters from
the pre-release string and removing the metadata part.

* semver.version == semver.semVersion.toInfoVersionString("", 0, true)
* semver.infoVersion == semver.semVersion.toInfoVersionString("%03d", 0, true)
* semver.semVersion.toString() == semver.semVersion.toInfoVersionString("%03d", 7, true)


## Tasks

## `printVersion`
This plugin adds a printVersion task, which will echo the project's calculated version
to standard-out.

````shell
$ gradlew printVersion

> Task :printVersion
10.10.0-SNAPSHOT
````

## `printInfoVersion`
This plugin adds a printInfoVersion task, which will echo the project's calculated version
to standard-out including commit count.

````shell
$ gradlew printInfoVersion

> Task :printInfoVersion
10.10.0-SNAPSHOT+072
````

## `printSemVersion`
This plugin adds a printSemVersion task, which will echo the project's calculated version
to standard-out includning commit count and sha.

````shell
$ gradlew printSemVersion

> Task :printSemVersion
10.10.0-SNAPSHOT+072.sha.18b3106
````

## `releaseVersion`
The `releaseVersion` task will by default create both a release commit, and a release tag. The releaseVersion task will 
fail with an error if there exists local modification. It is possible to change this behaviour with the following options:

 * **--no-tag**: skip creating a tag. (Can also be set in settings using `createReleaseTag=false`.)
 * **--tag**: create a tag (If this has been disabled by the `createReleaseTag=false` option otherwise this is the default.)
 * **--no-commit**: skip creating a commit. (Can also be set in settings using `createReleaseCommit=false`.)
 * **--commit**: create a commit (If this has been disabled by the `createReleaseCommit=false` option otherwise this is the default.)
 * **--no-dirty**: skip dirty check. (Can also be set in settings using `noDirtyCheck=true`.)
 * **--message**="a message": Add a message text to the tag and/or commit
 * **--preRelease**="pre-release": Change the current pre-release e.g. `--preRelease=alpha.1`.
   Set the pre-release to "-" e.g. `--preRelease=-` to promote a pre-release to a release.


## Example of how version is calculated 
With setting: `groupVersionIncrements = true` (default)

| Command                                       | Commit Text               | Calculated version  |
| --------------------------------------------- | ------------------------- | ------------------- |
| git commit -m "Initial commit"                | Initial commit            | 0.0.1-SNAPSHOT+001  |
| git commit -m "some changes"                  | some changes              | 0.0.1-SNAPSHOT+002  |
| gradle releaseVersion                         | release: v0.0.1           | 0.0.1               |
| git commit -m "some changes"                  | some changes              | 0.0.2-SNAPSHOT+001  |
| gradle releaseVersion                         | release: v0.0.2           | 0.0.2               |
| git commit -m "fix: a fix"                    | fix: a fix                | 0.0.3-SNAPSHOT+001  |
| git commit -m "fix: another fix"              | fix: another fix          | 0.0.3-SNAPSHOT+002  |
| gradle releaseVersion                         | release: v0.0.3           | 0.0.3               |
| git commit -m "feat: a feature"               | feat: a feature           | 0.1.0-SNAPSHOT+001  |
| git commit -m "feat: another feature"         | feat: another feature     | 0.1.0-SNAPSHOT+002  |
| git commit -m "feat!: breaking feature"       | feat!: breaking feature   | 1.0.0-SNAPSHOT+003  |
| git commit -m "some changes"                  | some changes              | 1.0.0-SNAPSHOT+004  |
| git commit -m "feat: changes"                 | feat: changes             | 1.0.0-SNAPSHOT+005  |
| git commit -m "feat: changes"                 | feat: changes             | 1.0.0-SNAPSHOT+006  |
| git commit -m "fix: a fix"                    | fix: a fix                | 1.0.0-SNAPSHOT+007  |
| gradle releaseVersion                         | release: v1.0.0           | 1.0.0               |
| git commit -m "some changes"                  | some changes              | 1.0.1-SNAPSHOT+001  |
| gradle releaseVersion --preRelease="alpha.1"  | release: v1.0.1-alpha.1   | 1.0.1-alpha.1       |
| git commit -m "some changes"                  | some changes              | 1.0.1-alpha.2+001   |
| gradle releaseVersion                         | release: v1.0.1-alpha.2   | 1.0.1-alpha.2       |
| git commit -m "fix: a fix"                    | fix: a fix                | 1.0.1-alpha.3+001   |
| git commit -m "fix: another fix"              | fix: another fix          | 1.0.1-alpha.3+002   |
| git commit -m "feat: a feature"               | feat: a feature           | 1.1.0-alpha.1+003   |
| gradle releaseVersion                         | release: v1.1.0-alpha.1   | 1.1.0-alpha.1       |
| git commit -m "feat: another feature"         | feat: another feature     | 1.1.0-alpha.2+001   |
| git commit -m "feat!: breaking feature"       | feat!: breaking feature   | 2.0.0-alpha.1+002   |
| gradle releaseVersion --preRelease="-"        | release: v2.0.0           | 2.0.0               |

With setting: `groupVersionIncrements = false`

| Command                                       | Commit Text               | Calculated version  |
| --------------------------------------------- | ------------------------- | ------------------- |
| git commit -m "Initial commit"                | Initial commit            | 0.0.1-SNAPSHOT+001  |
| git commit -m "some changes"                  | some changes              | 0.0.1-SNAPSHOT+002  |
| gradle releaseVersion                         | release: v0.0.1           | 0.0.1               |
| git commit -m "some changes"                  | some changes              | 0.0.2-SNAPSHOT+001  |
| gradle releaseVersion                         | release: v0.0.2           | 0.0.2               |
| git commit -m "fix: a fix"                    | fix: a fix                | 0.0.3-SNAPSHOT+001  |
| git commit -m "fix: another fix"              | fix: another fix          | 0.0.4-SNAPSHOT+002  |
| gradle releaseVersion                         | release: v0.0.4           | 0.0.4               |
| git commit -m "feat: a feature"               | feat: a feature           | 0.1.0-SNAPSHOT+001  |
| git commit -m "feat: another feature"         | feat: another feature     | 0.2.0-SNAPSHOT+002  |
| git commit -m "feat!: breaking feature"       | feat!: breaking feature   | 1.0.0-SNAPSHOT+003  |
| git commit -m "some changes"                  | some changes              | 1.0.1-SNAPSHOT+004  |
| git commit -m "feat: changes"                 | feat: changes             | 1.1.0-SNAPSHOT+005  |
| git commit -m "feat: changes"                 | feat: changes             | 1.2.0-SNAPSHOT+006  |
| git commit -m "fix: a fix"                    | fix: a fix                | 1.2.1-SNAPSHOT+007  |
| gradle releaseVersion                         | release: v1.2.1           | 1.2.1               |
| git commit -m "some changes"                  | some changes              | 1.2.2-SNAPSHOT+001  |
| gradle releaseVersion --preRelease="alpha.1"  | release: v1.2.2-alpha.1   | 1.2.2-alpha.1       |
| git commit -m "some changes"                  | some changes              | 1.2.2-alpha.2+001   |
| gradle releaseVersion                         | release: v1.2.2-alpha.2   | 1.2.2-alpha.2       |
| git commit -m "fix: a fix"                    | fix: a fix                | 1.2.2-alpha.3+001   |
| git commit -m "fix: another fix"              | fix: another fix          | 1.2.2-alpha.4+002   |
| git commit -m "feat: a feature"               | feat: a feature           | 1.3.0-alpha.1+003   |
| gradle releaseVersion                         | release: v1.3.0-alpha.1   | 1.3.0-alpha.1       |
| git commit -m "feat: another feature"         | feat: another feature     | 1.3.0-alpha.2+001   |
| git commit -m "feat!: breaking feature"       | feat!: breaking feature   | 2.0.0-alpha.1+002   |
| gradle releaseVersion --preRelease="-"        | release: v2.0.0           | 2.0.0               |


## Configuration

The plugin can be configured using the `semver` extension. This needs to be done before retrieving the version:

```groovy
semver {
    //Example of each property with their respective default value. 
    //There is no need to set these unless you want to change the default. 
    defaultPreRelease = "SNAPSHOT"
    releasePattern = "\\Arelease(?:\\(\\w+\\))?:"
    majorPattern = "\\A\\w+(?:\\(\\w+\\))?!:|^BREAKING[ -]CHANGE:"
    minorPattern = "\\Afeat(?:\\(\\w+\\))?:"
    patchPattern = "\\Afix(?:\\(\\w+\\))?:"
    releaseCommitTextFormat = "release: v%s\n\n%s"
    releaseTagNameFormat = "%s"
    groupVersionIncrements = true
    noDirtyCheck = false
    noAutoBump = false
    gitDirectory = project.projectDir
    createReleaseCommit = true
    createReleaseTag = true
}

//Remember to retrieve the version after plugin has been configured
version = semver.version
```

* **defaultPreRelease**: sets the default pre-release to use if there are commits or local modifications.
* **releasePattern**: used to identify commits that are used as release markers.
* **majorPattern, minorPattern and patchPattern**: used to identify conventional commits used for increasing version.
* **releaseCommitTextFormat**: String format used by `releaseVersion` task for creating release commits. First parameter
  is the version and second parameter is the message (if given using --message=).
  This text should preferably match the `releasePattern`.
* **releaseTagNameFormat**: String format used by `releaseVersion` task for creating release tags e.g. `"v%s"` to prefix 
  version tags with "v".
* **groupVersionIncrements**: Used to disable grouping of version increments so that each commit message counts.
* **noDirtyCheck**: Can be used to ignore all local modifications when calculating the version.
* **noAutoBump**: If set only commits matching majorPattern, minorPattern or patchPattern will increase the version.
  The default behaviour for the plugin is to assume you have begun the work on the next release for any commit you do
  after the last release. The patch level (or pre-release level, if the last release was a pre-release) of the version
  will be incremented by one if not already incremented by **majorPattern,  minorPattern or patchPattern**.
  (This option does not apply to the release task.)
* **gitDirectory**: The directory where the git repo can be found. 
* **createReleaseTag**: If a release tag should be created when running the release task. Setting this to false
  has the same effect as the --no-tag flag.
* **createReleaseCommit**: If a release commit should be created when running the release task. Setting this to false
  has the same effect as the --no-commit flag.

Patterns is matched using [java regular expressions](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) 
with IGNORE_CASE and MULTILINE options enabled.

## Supported Gradle version

This plugin has been tested on Gradle 7.x and 8.x. (Version 0.4.3 and older should work on gradle 6.x and probably 5.x)

## Continues Integration
The plugin calculates the version using the commit tree. Make sure you check out all commits relevant and not just
a shallow copy.

### GitHub Actions example
```yaml
- uses: actions/checkout@v3
  with:
    fetch-depth: 0
```
[GitHub Actions example project](https://github.com/jmongard/Git.SemVersioning.Gradle.Actions-Example)

## Limitations

### Version 0.x.x
There is currently no special handling for version 0.x.x. If you make breaking changes and want to preserve the 0.x.x 
version create a release commit yourself and set the number as desired e.g.
```
git commit -m "release: 0.1.2" --allow-empty 
```

### Reverts
The plugin does not handle commits reverting previous commits and referring to the reverted commit in the commit message. 
Set the version to the correct version after reverting as in the example above. 


# Resources

* [Semantic Versioning](https://semver.org/)
* [Conventional Commit](https://www.conventionalcommits.org/)
* [Angular Git Commit Guidelines](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#-commit-message-format)
