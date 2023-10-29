# Customizing the Change Log

## Header texts

Header texts used when generating a changelog is defined in a map. Entries can be added, changed or removed.

### Default header text

The default texts are defined like this:
```kotlin
semver {
    changeLogTexts {
        headerTexts = mutableMapOf(
            "fix" to "### Bug Fixes \uD83D\uDC1E",
            "feat" to "### New Features \uD83C\uDF89",
            "test" to "### Tests ✅",
            "docs" to "### Documentation \uD83D\uDCD6",
            "deps" to "### Dependency Updates \uD83D\uDE80",
            "build" to "### Build \uD83D\uDC18 & CI ⚙\uFE0F",
            "ci" to "### Build \uD83D\uDC18 & CI ⚙\uFE0F",
            "chore" to "### Chores \uD83D\uDD27",
            "perf" to "### Performance Enhancements ⚡",
            "refactor" to "### Refactorings \uD83D\uDE9C"
        )
        header = "## What's Changed" // Set your custom header text
        breakingChange = "### Breaking Changes 🛠" // Set your custom breaking change text
        otherChange = "### Other Changes \uD83D\uDCA1" // Set your custom other change text
    }
}
```

### Customizing header text

Header texts can be customized using changeLogTexts section in the semver settings:

```kotlin
semver {
    changeLogTexts {
        // Set the top header e.g
        header = "## My Custom heder" // Set your custom header text
        // Set the breaking change header e.g
        breakingChange = "### My Custom breaking changes heder ⚠️" // Set your custom breaking change text
        // Set the other changes header e.g
        otherChange = "### My Custom other changes heder  🧩" // Set your custom other change text

        //Or any other header by updating the headerTexts map directly. These are used for both type and scope e.g.
        headerTexts["fix"] = "### Bug Fixes 🐛"
        headerTexts["security"] = "### Security Fixes 🔒"

        // I you don't like a predefined category just remove it
        headerTexts.remove("refactor")
    }
}

```

## Change log template

### Predefined templates

There are three predefined templates for the change log:

- defaultChangeLog
- simpleChangeLog
- scopeChangeLog

```kotlin
semver {
    // This is the default
    changeLogFormat = git.semver.plugin.changelog.ChangeLogFormat.defaultChangeLog
    // Use the simple changelog
    changeLogFormat = git.semver.plugin.changelog.ChangeLogFormat.simpleChangeLog
    // Use the scope changelog
    changeLogFormat = git.semver.plugin.changelog.ChangeLogFormat.scopeChangeLog
}
```

### Custom template

A custom changelog template can be configured instead of using a predefined one. 
This is an example for printing only test changes. 
```kotlin
semver {
    changeLogFormat {
        appendLine("## My changed tests").appendLine()
        withType("test") {
            appendLine("### Test")
            formatChanges {
                appendLine("- ${scope()}${header()}")
            }
            appendLine()
        }
    }
}
```

#### Default template

[Se default template implementations for more examples](/src/main/kotlin/git/semver/plugin/changelog/ChangeLogFormat.kt)
