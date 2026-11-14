plugins {
//    kotlin("jvm") version "1.4.0" apply false
    id("org.jetbrains.kotlin.jvm") version "1.7.0"

}


allprojects {

    group = "org.bytonic"
    version = "1.1.1"

    apply(plugin = "org.jetbrains.kotlin.jvm")

//    apply {
//        plugin("org.jetbrains.kotlin.jvm")
//        plugin("java")
//    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        testImplementation(group = "junit", name = "junit", version = "4.12")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
        testImplementation("org.mockito:mockito-core:5.+")
    }
}

