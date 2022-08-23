import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import net.kyori.indra.git.IndraGitExtension
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("com.github.johnrengelman.shadow")
    jacoco
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
    implementation(libs.spark.common)

    testImplementation(libs.bundles.junit)
    testImplementation(libs.junit.platform.runner)
    testImplementation(libs.mockk)
    testImplementation(libs.jimfs)
    testRuntimeOnly(libs.bytebuddy)
}

tasks {
    jar {
        manifest {
            attributes(
                "Main-Class" to "org.kryptonmc.krypton.KryptonKt",
                "Automatic-Module-Name" to "org.kryptonmc.server",
                "Implementation-Title" to "Krypton",
                "Implementation-Vendor" to "KryptonMC",
                "Implementation-Version" to project.version.toString(),
                "Multi-Release" to "true"
            )
            extensions.findByType<IndraGitExtension>()?.applyVcsInformationToManifest(this)
        }
    }
    withType<ShadowJar> {
        val buildNumber = System.getenv("BUILD_NUMBER")?.let { "-$it" }.orEmpty()
        archiveFileName.set("Krypton-${project.version}$buildNumber.jar")
        transform(Log4j2PluginsCacheFileTransformer::class.java)

        exclude("it/unimi/dsi/fastutil/booleans/**")
        exclude("it/unimi/dsi/fastutil/bytes/**")
        exclude("it/unimi/dsi/fastutil/chars/**")
        exclude("it/unimi/dsi/fastutil/floats/**")
        exclude("it/unimi/dsi/fastutil/io/**")
        exclude("it/unimi/dsi/fastutil/objects/*Reference*")
        exclude("it/unimi/dsi/fastutil/shorts/**")

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

tasks["build"].dependsOn(tasks["shadowJar"])

jacoco {
    toolVersion = global.versions.jacoco.get()
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

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
