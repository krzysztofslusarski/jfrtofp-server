
group = "me.bechberger"
description = "Bundle of jfrtofp converter with a custom Firefox Profiler"

inner class ProjectInfo {
    val longName = "Bundle of the JFR to FirefoxProfiler converter with a custom Firefox Profiler"
    val website = "https://github.com/parttimenerd/jfrtofp-server"
    val scm = "git@github.com:parttimenerd/$name.git"
}

fun properties(key: String) = project.findProperty(key).toString()

configurations.all {
    resolutionStrategy.cacheDynamicVersionsFor(0, "hours")
    resolutionStrategy.cacheChangingModulesFor(0, "hours")
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"

    id("com.github.johnrengelman.shadow") version "7.1.2"

    id("io.gitlab.arturbosch.detekt") version "1.21.0"
    pmd

    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"

    id("maven-publish")

    id("java-library")
    id("signing")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

pmd {
    isConsoleOutput = true
    toolVersion = "6.21.0"
    rulesMinimumPriority.set(5)
    ruleSets = listOf("category/java/errorprone.xml", "category/java/bestpractices.xml")
}

apply { plugin("com.github.johnrengelman.shadow") }

detekt {
    buildUponDefaultConfig = true // preconfigure defaults
    config = files("$rootDir/config/detekt/detekt.yml")
    autoCorrect = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "1.8"
}
tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "1.8"
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // This dependency is used by the application.
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.4.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.21.0")
    implementation("info.picocli:picocli:4.6.3")
    implementation("io.javalin:javalin:4.6.4")
    implementation("org.slf4j:slf4j-simple:1.8.0-beta4")
    implementation("com.github.parttimenerd:jfrtofp:main-SNAPSHOT") {
        this.isChanging = true
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    // Define the main class for the application.
    mainClass.set("me.bechberger.jfrtofp.server.MainKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.register<Copy>("copyHooks") {
    from("bin/pre-commit")
    into(".git/hooks")
}

tasks.findByName("build")?.dependsOn(tasks.findByName("copyHooks"))

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("jfrtofp-server")
                packaging = "jar"
                description.set(project.description)
                inceptionYear.set("2022")
                url.set("https://github.com/parttimenerd/jfrtofp-server")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("parttimenerd")
                        name.set("Johannes Bechberger")
                        email.set("me@mostlynerdless.de")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/parttimenerd/jfrtofp-server")
                    developerConnection.set("scm:git:https://github.com/parttimenerd/jfrtofp-server")
                    url.set("https://github.com/parttimenerd/jfrtofp-server")
                }
            }
        }
    }
    repositories {
        maven {
            name = "Sonatype"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            credentials {
                username = properties("sonatypeUsername")
                password = properties("sonatypePassword")
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

repositories {
    maven {
        url = uri("https://jitpack.io")
    }
}
