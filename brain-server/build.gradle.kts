plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.mgmt)
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
    implementation(project(":brain-ai"))
    implementation(project(":brain-search"))

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.ai.anthropic.starter)
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
