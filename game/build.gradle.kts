dependencies {
    compileOnly(project(":qcommon"))
    implementation("org.apache.commons:commons-csv:1.9.0")
}
tasks.jar {
    archiveFileName.set("baseq2.jar")
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}