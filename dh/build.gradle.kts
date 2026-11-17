plugins {
    id("java")
}

group = "lee.qore"
version = "1.1.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(project(":qcommon"))
    implementation(project(":game"))
    implementation(project(":server"))
    implementation(project(":client"))
}

tasks.test {
    useJUnitPlatform()
}