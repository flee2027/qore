plugins {
    application
}

application {
    mainClass.set("jake2.fullgame.Jake2")
    applicationDefaultJvmArgs = listOf(
        "-Djava.library.path=${layout.buildDirectory.get()}/install/fullgame/lib"
    )
}

dependencies {
    implementation(project(":qcommon"))
    implementation(project(":client"))
    implementation(project(":server"))
    runtimeOnly(project(":game"))
    testImplementation(project(":game"))
}

tasks.register<Copy>("copyNatives") {
    val platforms = listOf("windows", "linux", "osx")
    duplicatesStrategy = DuplicatesStrategy.WARN

    platforms.forEach { pl ->
        val nativeArtifacts = configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts.filter {
            it.classifier == "natives-$pl"
        }
        nativeArtifacts.forEach { artifact ->
            from(zipTree(artifact.file))
        }
    }
    into(layout.buildDirectory.dir("natives"))
}

distributions {
    main {
        contents {
            from(layout.buildDirectory.dir("natives")) {
                into("lib")
            }
        }
    }
}

tasks.named<JavaExec>("run") {
    dependsOn("copyNatives", "installDist")
    val libPath = layout.buildDirectory.dir("install/fullgame/lib").get().asFile.absolutePath
    systemProperty("java.library.path", libPath)
}

tasks.register("buildQoreDist") {
    dependsOn("jar", "copyNatives", ":game:jar")
    val distDir = layout.buildDirectory.dir("qore-release").get().asFile

    doLast {
        distDir.mkdirs()
        val libDir = File(distDir, "lib").apply { mkdirs() }

        val mainJar = tasks.jar.get().archiveFile.get().asFile
        copy {
            from(mainJar)
            into(distDir)
            rename { "qore.jar" }
        }

        copy {
            from(configurations.runtimeClasspath)
            into(libDir)
            exclude("*natives-windows.jar", "*natives-linux.jar", "*natives-osx.jar")
        }

        copy {
            from(layout.buildDirectory.dir("natives").get().asFile)
            into(libDir)
        }

        val shScript = File(distDir, "qore.sh")
        shScript.writeText("""
            #!/bin/sh
            export GDK_BACKEND=x11
            export SDL_VIDEODRIVER=x11
            APP_HOME=${'$'}(cd "${'$'}(dirname "${'$'}0")" && pwd)
            
            if [ -n "${'$'}JAVA_HOME" ] && [ -x "${'$'}JAVA_HOME/bin/java" ]; then
                JAVACMD="${'$'}JAVA_HOME/bin/java"
            else
                JAVACMD="java"
            fi
            
            exec "${'$'}JAVACMD" -Djava.library.path="${'$'}APP_HOME/lib" -cp "qore.jar:lib/*" jake2.fullgame.Jake2 "${'$'}@"
        """.trimIndent())
        shScript.setExecutable(true)

        val batScript = File(distDir, "qore.bat")
        batScript.writeText("""
            @echo off
            setlocal
            set APP_HOME=%~dp0
            if defined JAVA_HOME (set JAVACMD="%JAVA_HOME%\bin\java.exe") else (set JAVACMD="java.exe")
            %JAVACMD% -Djava.library.path="%APP_HOME%lib" -cp "qore.jar;lib/*" jake2.fullgame.Jake2 %*
            endlocal
        """.trimIndent())

        println("Build successful. Output directory: ${distDir.absolutePath}")
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}