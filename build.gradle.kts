plugins {
    java
    alias(libs.plugins.spring.boot)          apply false
    alias(libs.plugins.spring.dependency.mgmt) apply false
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        "testImplementation"(platform(rootProject.libs.junit.bom))
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testImplementation"(rootProject.libs.assertj)
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // integrationTest task: runs *IT tests with a real ANTHROPIC_API_KEY
    // Excluded from the standard build lifecycle (not wired into 'check' or 'build')
    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests (*IT) that require ANTHROPIC_API_KEY"
        group       = "verification"
        useJUnitPlatform()
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath       = sourceSets["test"].runtimeClasspath
        include("**/*IT.class")

        val apiKey = System.getenv("ANTHROPIC_API_KEY") ?: ""
        onlyIf { apiKey.isNotBlank() }
        doFirst {
            if (apiKey.isBlank()) {
                throw GradleException("ANTHROPIC_API_KEY is not set — skipping integrationTest")
            }
        }
        // Do NOT depend on test so it can run independently
        mustRunAfter(tasks.named("test"))
    }
}
