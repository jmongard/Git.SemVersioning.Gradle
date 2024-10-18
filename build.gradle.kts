plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    kotlin("jvm") version "1.9.10"
    id("com.gradle.plugin-publish") version "1.2.1"
    id("com.github.jmongard.git-semver-plugin") version "0.12.5"
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
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.assertj:assertj-core:3.26.3")

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.3.202401111512-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.gpg.bc:5.13.3.202401111512-r")
    implementation("org.slf4j:slf4j-api:1.7.36")
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
