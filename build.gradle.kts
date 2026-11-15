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

/** Reads a path from paths.properties, or returns null if missing. */
fun readPath(key: String): String? {
    if (!pathsFile.exists()) return null
    val props = Properties()
    pathsFile.inputStream().use { props.load(it) }
    return props.getProperty(key)?.takeIf { it.isNotBlank() }
}

/** Reads a required path, failing the build with a clear message if unset. */
fun requirePath(key: String): String =
    readPath(key) ?: error("$key is not configured. Run ./gradlew resetPaths then retry.")

// Shared runtime classpath for :tools (contains PathPrompter, PngToWalConverter, PngValidator).
val toolsRuntime = project(":tools")
    .extensions.getByType(org.gradle.api.tasks.SourceSetContainer::class.java)
    .getByName("main")
    .runtimeClasspath

// ===== Tasks =====

/**
 * Prompts for missing paths on first run.
 * Re-prompts only for keys whose stored value no longer points to an existing file.
 */
tasks.register("ensurePaths") {
    group = "trenchbroom"
    description = "Prompts for missing paths (textures.dir, trenchbroom.exe, palette.lmp)"
    dependsOn(":tools:classes")
    doLast {
        javaexec {
            classpath = toolsRuntime
            mainClass.set("lee.qore.tools.PathPrompter")
            args(
                "$projectDir/tools",
                "textures.dir",
                "trenchbroom.exe",
                "palette.lmp"
            )
        }
    }
}

// Deletes all .wal files under the textures directory.
tasks.register("cleanWalTextures") {
    group = "trenchbroom"
    description = "Deletes all .wal files under the textures directory"
    dependsOn("ensurePaths")
    doLast {
        val texturesDir = file(requirePath("textures.dir"))
        if (!texturesDir.exists()) {
            logger.warn("Textures directory does not exist: ${texturesDir.absolutePath}")
            return@doLast
        }
        var deleted = 0
        texturesDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("wal", ignoreCase = true) }
            .forEach { wal ->
                logger.lifecycle("Deleting ${wal.absolutePath}")
                if (wal.delete()) deleted++
            }
        logger.lifecycle("Deleted $deleted WAL file(s) from ${texturesDir.absolutePath}")
    }
}

// Converts every PNG under the textures directory to a matching WAL file.
tasks.register("convertAllPngToWal") {
    group = "trenchbroom"
    description = "Converts all .png files under the textures directory to .wal"
    dependsOn("cleanWalTextures", ":tools:classes")
    doLast {
        val texturesDir = file(requirePath("textures.dir"))
        val palette    = requirePath("palette.lmp")
        if (!texturesDir.exists()) {
            logger.warn("Textures directory does not exist: ${texturesDir.absolutePath}")
            return@doLast
        }
        texturesDir.walkTopDown().filter { it.extension == "png" }.forEach { png ->
            val wal = png.resolveSibling(png.nameWithoutExtension + ".wal")
            logger.lifecycle("Converting ${png.absolutePath} -> ${wal.absolutePath}")
            javaexec {
                classpath = toolsRuntime
                mainClass.set("lee.qore.tools.PngToWalConverter")
                args(png.absolutePath, wal.absolutePath, palette)
            }
        }
    }
}

// Validates PNG files under the textures directory.
tasks.register("validateTextures") {
    group = "trenchbroom"
    description = "Validates PNG files under the textures directory"
    dependsOn("ensurePaths")
    doLast {
        val texturesDir = file(requirePath("textures.dir"))
        javaexec {
            classpath = toolsRuntime
            mainClass.set("lee.qore.tools.PngValidator")
            args(texturesDir.absolutePath)
        }
    }
}

// Launches TrenchBroom in a detached process, so Gradle returns immediately.
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

// Shows all configured paths and whether they currently exist.
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

// Deletes tools/paths.properties so the next trenchbroom task re-prompts for all paths.
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

// One-click pipeline: clean stale WALs, convert PNGs to WAL, then open TrenchBroom.
tasks.register("editWithTrenchBroom") {
    group = "trenchbroom"
    description = "Cleans WALs, converts PNGs, and launches TrenchBroom"
    dependsOn("convertAllPngToWal", "launchTrenchBroom")
}