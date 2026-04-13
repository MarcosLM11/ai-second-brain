plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.mgmt)
    alias(libs.plugins.graalvm.native)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.ai.bom.get().toString())
    }
}

dependencies {
    implementation(project(":brain-core"))
    implementation(project(":brain-wiki"))
    implementation(project(":brain-graph"))
    implementation(project(":brain-search"))

    implementation(libs.jgrapht.io)
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.ai.mcp.server.starter)
    implementation(libs.picocli)
    implementation(libs.picocli.spring.boot.starter)
    implementation(libs.sqlite.jdbc)
    implementation(libs.pdfbox)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockito.junit.jupiter)
}

tasks.withType<Test> {
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("brain")
            mainClass.set("brain.server.BrainApplication")
            buildArgs.addAll(
                "--initialize-at-run-time=org.sqlite.NativeDB",
                "-H:+ReportExceptionStackTraces",
                "--no-fallback"
            )
        }
    }
    toolchainDetection.set(false)
}
