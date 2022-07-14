import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(projects.api)
    implementation(projects.annotationProcessor)

    // Networking
    api(libs.bundles.netty)
    implementation(libs.netty.transport.native.epoll) {
        artifact {
            classifier = "linux-x86_64"
        }
    }
    implementation(libs.netty.transport.native.kqueue) {
        artifact {
            classifier = "osx-x86_64"
        }
    }
    implementation(libs.velocity.native)

    // Events
    implementation(libs.event)
    implementation(libs.asm)

    // Logging and console
    runtimeOnly(libs.log4j.core)
    implementation(libs.tca)
    runtimeOnly(libs.jline.jansi)

    // Data
    api(libs.dataConverter)
    api(libs.nbt)
    implementation(libs.articData)

    // Collections and caching
    implementation(libs.fastutil)
    implementation(libs.flare)
    implementation(libs.flare.fastutil)
    implementation(libs.caffeine)

    // Miscellaneous
    implementation(libs.clikt)
    implementation(libs.bstats)

    testImplementation(libs.bundles.junit)
    testImplementation(libs.junit.platform.runner)
    testImplementation(libs.mockk)
    testImplementation(libs.jimfs)
    testRuntimeOnly(libs.bytebuddy)
}

license {
    exclude(
        "**/*.properties",
        "**/*.conf",
        "**/*.json",
        // Velocity derivatives, with a special header
        "**/event/KryptonEventManager.kt",
        "**/plugin/KryptonPluginContainer.kt",
        "**/plugin/KryptonPluginManager.kt",
        "**/plugin/PluginClassLoader.kt",
        "**/plugin/dependencies.kt",
        "**/plugin/loader/LoadedPluginDescription.kt",
        "**/plugin/loader/LoadedPluginDescriptionCandidate.kt",
        "**/plugin/loader/PluginLoader.kt",
        "**/scheduling/KryptonScheduler.kt",
        "**/util/bytebufs.kt",
        // Sponge derivatives, with a special header
        "**/console/BrigadierCompleter.kt",
        "**/console/BrigadierHighlighter.kt"
    )
}

tasks["build"].dependsOn(tasks.shadowJar)

tasks {
    withType<ShadowJar> {
        val buildNumber = System.getenv("BUILD_NUMBER")?.let { "-$it" }.orEmpty()
        archiveFileName.set("Krypton-${project.version}$buildNumber.jar")
        transform<Log4j2PluginsCacheFileTransformer>()

        fastutilExclusions("booleans", "bytes", "chars", "floats", "io", "shorts")

        relocate("org.bstats", "org.kryptonmc.krypton.bstats")
    }
    withType<ProcessResources> {
        filter<ReplaceTokens>("tokens" to mapOf(
            "version" to project.version.toString(),
            "minecraft" to global.versions.minecraft.get(),
            "spark" to libs.versions.spark.get(),
            "data" to global.versions.minecraft.get().replace('.', '_')
        ))
    }
}

applyImplJarMetadata("org.kryptonmc.server", "Krypton") {
    put("Main-Class", "org.kryptonmc.krypton.KryptonKt")
    put("Multi-Release", "true")
}
