plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Plain Kotlin/JVM, no Android: all the testable game logic lives here.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

// Clue bank: validates content/ before running the tests.
tasks.register<Exec>("validateClues") {
    group = "verification"
    description = "Validates content/clues/pt-PT. Fails the build."
    workingDir = rootProject.projectDir
    commandLine("python3", rootProject.file("tools/validate_clues.py").absolutePath)
}

tasks.withType<Test> {
    dependsOn("validateClues")
}
