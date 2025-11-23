# Git Semantic Versioning Plugin for Gradle
[![Gradle Build](https://github.com/jmongard/Git.SemVersioning.Gradle/workflows/Gradle%20Build/badge.svg)](https://github.com/jmongard/Git.SemVersioning.Gradle/actions/workflows/gradle-push.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jmongard_Git.SemVersioning.Gradle&metric=alert_status)](https://sonarcloud.io/dashboard?id=jmongard_Git.SemVersioning.Gradle)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=jmongard_Git.SemVersioning.Gradle&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=jmongard_Git.SemVersioning.Gradle)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jmongard_Git.SemVersioning.Gradle&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jmongard_Git.SemVersioning.Gradle)
[![GitHub tag (with filter)](https://img.shields.io/github/v/tag/jmongard/Git.SemVersioning.Gradle?logo=gradle&label=Release)](https://plugins.gradle.org/plugin/com.github.jmongard.git-semver-plugin)

A Gradle plugin that automatically versions your project using semantic versioning and conventional commits. It analyzes Git commit messages to determine version increments and generates change logs based on your commit history.


## Usage

Apply the plugin using standard Gradle convention and set the version of the project: 

```groovy
plugins {
    id 'com.github.jmongard.git-semver-plugin' version '<current version>'
}

//semver { ... } // Optionally add configuration for the plugin before getting the version

//Set the version for the current project:
version = semver.version

//Or in a multi project build set the version of all projects:
def ver = semver.version
allprojects {
    version = ver
}

```

[For the latest published version see the plugins page at Gradle.org](https://plugins.gradle.org/plugin/com.github.jmongard.git-semver-plugin)

The plugin requires Gradle 8 and Java version 17 to run.
(Use version `0.16.1` if Gradle 7 is required.)
(Use version `0.13.0` if Java 8 is required.)

## Versioning

The versioning system follows semantic versioning as described at [semver.org](https://semver.org/).

The plugin works by traversing the Git commit history backwards from HEAD until it finds a version tag or release commit, then calculates the new version based on conventional commit messages since that point.

The plugin recognizes [conventional commit](https://www.conventionalcommits.org/) messages (`fix:`, `feat:`, `refactor!:`, etc.) and increments the corresponding version number accordingly.

By default, the plugin groups multiple fixes/features or breaking changes into a single release. This means the major, minor, or patch number will increase by at most one compared to the previous release (excluding pre-releases). Set `groupVersionIncrements = false` if you prefer each commit to increment the version individually.

### Releases

The plugin will search the commit tree until it finds a commit it interprets as a release commit with a version number. 

* Any commit tagged with a version number will be considered to be a release.
* Any commit with commit message starting with `release:` 

The version number should consist of three numbers separated by a dot e.g. `1.0.0`. The version number does not need to 
be at the start of the message e.g. `release: v1.2.3` will be matched.


### Uncommitted Changes

If no version increment has been triggered by conventional commit messages since the last release, the patch number will be increased by one to indicate development progress.

If the current version is not already a pre-release, `-SNAPSHOT` will be appended to indicate this is a development version.


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
`semver.semVersion.toInfoVersionString(commitCountStringFormat: String = "%03d", shaLength: Int = 0, version2: Boolean = true, appendPreReleaseLast: Boolean = false)`

If Version2 flag is set to false, then semVer version one will be used stripping any non alpha-numeric characters from
the pre-release string and removing the metadata part.

The `appendPreReleaseLast` option can help when publishing to maven repositories if metadata is included but the version 
string will not be semver compliant. 

* semver.version == semver.semVersion.toInfoVersionString("", 0, true, false)
* semver.infoVersion == semver.semVersion.toInfoVersionString("%03d", 0, true, false)
* semver.semVersion.toString() == semver.semVersion.toInfoVersionString("%03d", 7, true, false)

### Two-Digit Versioning

The plugin supports 2-digit versioning (major.minor) in addition to the standard 3-digit semantic versioning (major.minor.patch). 
This can be useful for projects that follow a simpler versioning scheme.

To enable 2-digit versioning, set `useTwoDigitVersion = true` in your configuration:

#### Accessing 2-Digit Versions

When `useTwoDigitVersion` is enabled, the standard version properties automatically use the 2-digit format:

* `semver.version` - Returns the 2-digit version (e.g., `5.2`)
* `semver.infoVersion` - Returns the 2-digit version with commit count (e.g., `5.2+001`)
* `semver.semVersion.toString()` - Returns the 2-digit version with SHA (e.g., `5.2+001.sha.1c792d5`)

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
to standard-out including commit count and SHA.

````shell
$ gradlew printSemVersion

> Task :printSemVersion
10.10.0-SNAPSHOT+072.sha.18b3106
````

## `printChangeLog`
This plugin adds a printChangeLog task, which will format the commit messages for the current version
and output them to standard-out. To avoid encoding problems in the console, the change log can be output
to a UTF-8 encoded file using `--file <filename>`, e.g. `./gradlew printChangeLog --file build/changelog.md`.

**Note:** Use an absolute path for the filename as the working directory might not be what you expect when running
using the Gradle daemon. The `printChangeLog` task is currently only registered on the root project when the plugin is applied there.

````shell
$ gradlew printChangeLog

> Task :printChangeLog
## What's Changed

### Breaking Changes
  - fix(#5)!: A breaking change

### Bug Fixes
  - #1: A bug fix
  - #2: Another bug fix
````
[Configuring the changelog](/ChangeLog.md)

## `releaseVersion`
The `releaseVersion` task creates both a release commit and a release tag by default. The task will fail with an error if there are uncommitted local modifications. You can modify this behavior using the following options:

* **--no-tag**: Skip creating a tag (can also be set in settings using `createReleaseTag=false`)
* **--tag**: Create a tag (default behavior, unless disabled in settings)
* **--no-commit**: Skip creating a commit (can also be set in settings using `createReleaseCommit=false`)
* **--commit**: Create a commit (default behavior, unless disabled in settings)
* **--no-dirty**: Skip the dirty working directory check (can also be set in settings using `noDirtyCheck=true`)
* **--message**="message": Add a custom message to the tag and/or commit
* **--preRelease**="version": Set the pre-release identifier (e.g., `--preRelease=alpha.1`). Use `--preRelease=-` to promote a pre-release to a full release

**Note:** The `releaseVersion` task is currently only registered on the root project when the plugin is applied there.

## Example of how version is calculated 

### With setting: `groupVersionIncrements = true` (default)

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

### With setting: `groupVersionIncrements = false`

| Command                                       | Commit Text               | Calculated version |
| --------------------------------------------- | ------------------------- | ------------------- |
| git commit -m "Initial commit"                | Initial commit            | 0.0.1-SNAPSHOT+001 |
| git commit -m "some changes"                  | some changes              | 0.0.1-SNAPSHOT+002 |
| gradle releaseVersion                         | release: v0.0.1           | 0.0.1              |
| git commit -m "some changes"                  | some changes              | 0.0.2-SNAPSHOT+001 |
| gradle releaseVersion                         | release: v0.0.2           | 0.0.2              |
| git commit -m "fix: a fix"                    | fix: a fix                | 0.0.3-SNAPSHOT+001 |
| git commit -m "fix: another fix"              | fix: another fix          | 0.0.4-SNAPSHOT+002 |
| gradle releaseVersion                         | release: v0.0.4           | 0.0.4              |
| git commit -m "feat: a feature"               | feat: a feature           | 0.1.0-SNAPSHOT+001 |
| git commit -m "feat: another feature"         | feat: another feature     | 0.2.0-SNAPSHOT+002 |
| git commit -m "feat!: breaking feature"       | feat!: breaking feature   | 1.0.0-SNAPSHOT+003 |
| git commit -m "some changes"                  | some changes              | 1.0.0-SNAPSHOT+004 |
| git commit -m "feat: changes"                 | feat: changes             | 1.1.0-SNAPSHOT+005 |
| git commit -m "feat: changes"                 | feat: changes             | 1.2.0-SNAPSHOT+006 |
| git commit -m "fix: a fix"                    | fix: a fix                | 1.2.1-SNAPSHOT+007 |
| gradle releaseVersion                         | release: v1.2.1           | 1.2.1              |
| git commit -m "some changes"                  | some changes              | 1.2.2-SNAPSHOT+001 |
| gradle releaseVersion --preRelease="alpha.1"  | release: v1.2.2-alpha.1   | 1.2.2-alpha.1      |
| git commit -m "some changes"                  | some changes              | 1.2.2-alpha.2+001  |
| gradle releaseVersion                         | release: v1.2.2-alpha.2   | 1.2.2-alpha.2      |
| git commit -m "fix: a fix"                    | fix: a fix                | 1.2.2-alpha.3+001  |
| git commit -m "fix: another fix"              | fix: another fix          | 1.2.2-alpha.4+002  |
| git commit -m "feat: a feature"               | feat: a feature           | 1.3.0-alpha.1+003  |
| gradle releaseVersion                         | release: v1.3.0-alpha.1   | 1.3.0-alpha.1      |
| git commit -m "feat: another feature"         | feat: another feature     | 1.3.0-alpha.2+001  |
| git commit -m "feat!: breaking feature"       | feat!: breaking feature   | 2.0.0-alpha.1+002  |
| gradle releaseVersion --preRelease="-"        | release: v2.0.0           | 2.0.0              |

## Configuration

The plugin can be configured using the `semver` extension. This needs to be done before retrieving the version:

```groovy
semver {
    //Example of each property with their respective default value. 
    //There is no need to set these unless you want to change the default. 
    defaultPreRelease = "SNAPSHOT"
    releasePattern = "\\Arelease(?:\\([^()]+\\))?:"
    majorPattern = "\\A\\w+(?:\\([^()]+\\))?!:|^BREAKING[ -]CHANGE:"
    minorPattern = "\\Afeat(?:\\([^()]+\\))?:"
    patchPattern = "\\Afix(?:\\([^()]+\\))?:"
    releaseCommitTextFormat = "release: v%s\n\n%s"
    releaseTagNameFormat = "%s"
    groupVersionIncrements = true
    noDirtyCheck = false
    noAutoBump = false
    gitDirectory = project.projectDir
    createReleaseCommit = true
    createReleaseTag = true
    metaSeparator = '+'
    useTwoDigitVersion = false
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
  Disabling dirty check can also be done from the command line e.g. `gradlew -PnoDirtyCheck=true someOtherTask`.
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
* **metaSeparator**: The character to use to separate build metadata from the version when printing info version.
* **useTwoDigitVersion**: If the version should be two digits instead of three.

Patterns is matched using [java regular expressions](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) 
with IGNORE_CASE and MULTILINE options enabled.

## Supported Gradle version

This plugin has been tested on Gradle 8.x and 9.x. (Version 0.4.3 and older should work on gradle 6.x and probably 5.x)

## Continuous Integration
The plugin calculates the version using the commit tree. Make sure you check out all commits relevant and not just
a shallow copy.

[GitHub Actions example project](https://github.com/jmongard/Git.SemVersioning.Gradle.Actions-Example)

### GitHub Actions checkout example
```yaml
- uses: actions/checkout@v3
  with:
    fetch-depth: 0
```

### GitHub Actions create release example
```yaml
  - name: Create GitHub release
    env:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    run: |
      tag=$(git describe --tags --abbrev=0)
      version=$(./gradlew -q printVersion)
       ./gradlew -q printChangeLog | gh release create $tag -d -t "Example Project $version" -F -
```


## Version 0.x.x
Breaking changes for version 0.x.x do not automatically increase the major version. If you want to increase the major
version, create a release commit manually and set the desired version number e.g.

```
git commit -m "release: 1.0.0" --allow-empty 
```


## Limitations

### Reverts
The plugin does not handle commits reverting previous commits and referring to the reverted commit in the commit message. 
Set the version to the correct version after reverting as in the example above. 


# Resources

* [Semantic Versioning](https://semver.org/)
* [Conventional Commit](https://www.conventionalcommits.org/)
* [Angular Git Commit Guidelines](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#-commit-message-format)
