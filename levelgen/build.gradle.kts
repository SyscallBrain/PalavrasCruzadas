plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

// CLI that pre-generates the levels on a computer (not shipped in the APK).
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
    implementation(project(":core"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

application {
    mainClass.set("app.palavrascruzadas.levelgen.MainKt")
    // Keep generation within 1 GB of heap.
    applicationDefaultJvmArgs = listOf("-Xmx1g")
}
