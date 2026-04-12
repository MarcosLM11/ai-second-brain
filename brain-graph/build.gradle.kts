dependencies {
    implementation(project(":brain-core"))
    implementation(project(":brain-wiki"))
    implementation(libs.jgrapht.core)
    implementation(libs.sqlite.jdbc)
    implementation(libs.jackson.databind)
}
