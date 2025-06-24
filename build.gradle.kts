plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.git.semver)
    id("jacoco")
}

semver {
    releaseTagNameFormat = "v%s"
    createReleaseTag = false
}

version = semver.version
group = "com.github.jmongard"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.jgit.core)
    implementation(libs.jgit.gpg)
    implementation(libs.slf4j.api)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.assertj.core)
}

gradlePlugin {
    website.set("https://github.com/jmongard/Git.SemVersioning.Gradle")
    vcsUrl = "https://github.com/jmongard/Git.SemVersioning.Gradle"

    plugins {
        create("gitSemverPlugin") {
            id = "com.github.jmongard.git-semver-plugin"
            displayName = "Git Semantic Versioning Plugin"
            description = "Automatic project versioning based on semantic versioning using git tags and conventional commits"
            tags = listOf("git", "kotlin", "semver", "semantic-versioning", "automatic-versioning", "version", "semantic", "release", "conventional", "conventional-commits")
            implementationClass = "git.semver.plugin.gradle.GitSemverPlugin"
        }
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(functionalTest)
    // Aggregate all execution data into a single XML report.
    executionData(fileTree(project.layout.buildDirectory).include("/jacoco/*.exec"))
    reports {
        xml.required = true
        html.required = false
    }
}
