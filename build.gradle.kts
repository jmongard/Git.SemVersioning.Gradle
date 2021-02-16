/*
 * User Manual available at https://docs.gradle.org/6.7.1/userguide/custom_plugins.html
 */

plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    kotlin("jvm") version "1.3.72"
    id("com.gradle.plugin-publish") version "0.12.0"
}

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test-junit"))

    testImplementation("org.assertj:assertj-core:3.18.1")

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")
    implementation("org.slf4j:slf4j-api:1.7.30")
}

gradlePlugin {
    // Define the plugin
    plugins {
        create("gitSemverPlugin") {
            id = "com.github.jmongard.git-semver-plugin"
            implementationClass = "git.semver.plugin.gradle.GitSemverPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/jmongard/Git.SemVersioning.Gradle"
    vcsUrl = "https://github.com/jmongard/Git.SemVersioning.Gradle"
    description = "Automatic project versioning based on semantic versioning using git tags and conventional commits"
    tags = listOf("git", "kotlin", "semver", "semantic-versioning", "automatic-versioning", "version", "semantic", "release", "conventional", "conventional-commits")

    (plugins) {
        "gitSemverPlugin" {
            // id is captured from java-gradle-plugin configuration
            displayName = "Git Semantic Versioning Plugin"
        }
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations.getByName("functionalTestImplementation").extendsFrom(configurations.getByName("testImplementation"))

// Add a task to run the functional tests
val functionalTest by tasks.creating(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

val check by tasks.getting(Task::class) {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}
