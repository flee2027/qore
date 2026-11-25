dependencies {
    compileOnly(project(":qcommon"))
    implementation("org.apache.commons:commons-csv:1.9.0")
}
tasks.jar {
    archiveFileName.set("baseq2.jar")
}