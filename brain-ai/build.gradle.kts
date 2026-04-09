plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.mgmt)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.ai.bom.get().toString())
    }
}

dependencies {
    implementation(project(":brain-core"))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.ai.anthropic.starter)

    testImplementation(libs.mockito.junit.jupiter)
}
