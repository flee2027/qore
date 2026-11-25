dependencies {
    implementation(project(":qcommon"))
    implementation("ch.qos.logback:logback-classic:1.2.11")
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}