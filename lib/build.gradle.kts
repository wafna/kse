import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    `java-library`
    `maven-publish`
    signing
    id("io.github.ben-manes.versions") version "0.61.0"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    @Suppress("VulnerableLibrariesLocal", "RedundantSuppression")
    implementation("ch.qos.logback:logback-classic:1.6.3")
    implementation("org.apache.logging.log4j:log4j-core:3.0.0-beta3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0-0.6.x-compat")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    testImplementation("com.zaxxer:HikariCP:7.1.0")
    testImplementation("com.h2database:h2:2.5.250")
    testImplementation("org.postgresql:postgresql:42.7.13")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:2.0.2")
    testImplementation("org.testcontainers:postgresql:1.21.4")
}

testing {
    suites {
        // Configure the built-in test suite
        @Suppress("UnstableApiUsage", "Unused")
        val test = named<JvmTestSuite>("test") {
            useKotlinTest("2.4.20")
        }
    }
}

base {
    archivesName.set("kse")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "kse"
            from(components["java"])

            pom {
                name.set("kse")
                description.set("Kotlin SQL Engine - A lightweight, performant, and easy-to-use JDBC wrapper written in Kotlin")
                url.set("https://github.com/wafna/kse")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("wafna")
                        name.set("Jason Feingold")
                        email.set("jfeingold@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/wafna/kse.git")
                    developerConnection.set("scm:git:ssh://github.com:wafna/kse.git")
                    url.set("https://github.com/wafna/kse")
                }
            }
        }
    }
    repositories {
//        maven {
//            name = "OSSRH"
//            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
//            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
//            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
//            credentials {
//                username = System.getenv("OSSRH_USERNAME")
//                password = System.getenv("OSSRH_PASSWORD")
//            }
//        }
        maven {
            name = "SonatypeCentral"
            url = uri("https://central.sonatype.com/api/v1/publisher/deployments/upload")
            credentials {
                username = System.getenv("SONATYPE_USERNAME")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
}

signing {
    val rawKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    if (!rawKey.isNullOrBlank()) {
        val keyFile = sequenceOf(file(rawKey), rootProject.file(rawKey)).firstOrNull { it.isFile }
        val signingKey = keyFile?.readText() ?: rawKey
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    setRequired {
        (gradle.taskGraph.hasTask("publish") || gradle.taskGraph.hasTask("publishToSonatype")) &&
                !version.toString().endsWith("-SNAPSHOT") &&
                (!rawKey.isNullOrBlank() || project.hasProperty("signing.keyId") || project.hasProperty("signing.secretKeyRingFile"))
    }
    sign(publishing.publications["maven"])
}

tasks {
    withType<Test> {
        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }
    val dokkaTask = named("dokkaGeneratePublicationHtml")
    named("javadoc") {
        dependsOn(dokkaTask)
    }
    named<Jar>("javadocJar") {
        from(dokkaTask)
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcollection-literals")
        freeCompilerArgs.add("-Xintrinsic-const-evaluation")
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
    }
}

