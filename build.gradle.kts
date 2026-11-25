import java.util.Properties
import java.io.File

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

// ===== Path config (stored in tools/paths.properties) =====

val pathsFile = file("$projectDir/tools/paths.properties")

fun readPath(key: String): String? {
    if (!pathsFile.exists()) return null
    val props = Properties()
    pathsFile.inputStream().use { props.load(it) }
    return props.getProperty(key)?.takeIf { it.isNotBlank() }
}

fun requirePath(key: String): String =
    readPath(key) ?: error("$key is not configured. Run ./gradlew resetPaths then retry.")

val toolsRuntime = project(":tools")
    .extensions.getByType(org.gradle.api.tasks.SourceSetContainer::class.java)
    .getByName("main")
    .runtimeClasspath

// ===== TrenchBroom tasks =====

tasks.register("ensurePaths") {
    group = "trenchbroom"
    description = "Prompts for missing paths (textures.dir, trenchbroom.exe) via Swing dialog"
    dependsOn(":tools:classes")
    doLast {
        javaexec {
            classpath = toolsRuntime
            mainClass.set("lee.qore.tools.PathPrompter")
            args(
                "$projectDir/tools",
                "textures.dir",
                "trenchbroom.exe"
            )
        }
    }
}

tasks.register("launchTrenchBroom") {
    group = "trenchbroom"
    description = "Launches TrenchBroom (detached, returns immediately)"
    dependsOn("ensurePaths")
    doLast {
        val exe = requirePath("trenchbroom.exe")
        if (!file(exe).exists()) {
            throw GradleException(
                "TrenchBroom not found at: $exe\n" +
                        "Run ./gradlew resetPaths and re-run to pick a new path."
            )
        }
        logger.lifecycle("Launching TrenchBroom (detached): $exe")
        val proc = ProcessBuilder(exe)
            .directory(projectDir)
            .start()
        logger.lifecycle("TrenchBroom started, pid=${proc.pid()}")
    }
}

tasks.register("showPaths") {
    group = "trenchbroom"
    description = "Shows all configured paths from tools/paths.properties"
    doLast {
        if (!pathsFile.exists()) {
            logger.lifecycle("No paths configured yet. Run any trenchbroom task first.")
            return@doLast
        }
        val props = Properties()
        pathsFile.inputStream().use { props.load(it) }
        props.stringPropertyNames().sorted().forEach { key ->
            val value = props.getProperty(key)
            val status = if (File(value).exists()) "OK" else "MISSING"
            logger.lifecycle("$key = $value [$status]")
        }
    }
}

tasks.register("resetPaths") {
    group = "trenchbroom"
    description = "Deletes tools/paths.properties so the next run re-prompts"
    doLast {
        if (pathsFile.exists() && pathsFile.delete()) {
            logger.lifecycle("Deleted ${pathsFile.absolutePath}. Next trenchbroom task will re-prompt.")
        } else {
            logger.lifecycle("No paths file to delete.")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}