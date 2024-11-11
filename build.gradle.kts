import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.changelog.Changelog
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Clock
import java.time.Instant

val publishChannel = prop("publishChannel")
val ideaVersion = prop("ideaVersion")
val pluginVersion = prop("pluginVersion").let { pluginVersion ->
    if (publishChannel != "release" && publishChannel != "stable") {
        val buildSuffix = prop("buildNumber") {
            Instant.now(Clock.systemUTC()).toString().substring(2, 16).replace("[-T:]".toRegex(), "")
        }
        "$pluginVersion-${publishChannel.uppercase()}+$buildSuffix"
    } else {
        pluginVersion
    }
}
version = pluginVersion
println("pluginVersion=$version")

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.3"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
    id("org.jetbrains.changelog") version "2.2.0"
    idea
}

allprojects {
    apply(plugin = "kotlin")

    repositories {
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        mavenCentral()
        maven(url = "https://jitpack.io")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
    }

    dependencies {
        implementation("me.alllex.parsus:parsus-jvm:0.6.1")
        implementation("com.github.andreypfau.tlb:tlb-jvm:54070d9405")
    }
}

sourceSets {
    main {
        java.srcDirs("src/gen")
    }
}

idea {
    module {
        isDownloadSources = true
        generatedSourceDirs.add(file("src/gen"))
    }
}

intellij {
    version.set(ideaVersion)
    type.set("IU")
    plugins.set(
        listOf(
            "JavaScript",
            "com.google.ide-perf:1.3.1",
            "izhangzhihao.rainbow.brackets:2023.3.2"
        )
    )
}

val generateFuncLexer = generateLexer("Func")
val generateFuncParser = generateParser("Func")

val generateTactLexer = generateLexer("Tact")
val generateTactParser = generateParser("Tact")

val generateFiftLexer = generateLexer("Fift")
val generateFiftParser = generateParser("Fift")

val generateTlbLexer = generateLexer("Tlb")
val generateTlbParser = generateParser("Tlb")

val generateAsmLexer = generateLexer("Asm")
val generateAsmParser = generateParser("Asm")

val generateTolkLexer = generateLexer("Tolk")
val generateTolkParser = generateParser("Tolk")

val compileKotlin = tasks.named("compileKotlin") {
    dependsOn(
        generateFuncParser, generateFuncLexer,
        generateTactParser, generateTactLexer,
        generateFiftParser, generateFiftLexer,
        generateTlbParser, generateTlbLexer,
        generateAsmParser, generateAsmLexer,
        generateTolkParser, generateTolkLexer,
    )
}

val compileJava = tasks.named("compileJava")

changelog {
    version.set(version)
    path.set("${project.projectDir}/CHANGELOG.md")
    header.set(provider { "[${version.get()}]" })
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
}

tasks {
    runIde { enabled = true }
    prepareSandbox { enabled = true }
    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("")
        changeNotes.set(provider {
            changelog.renderItem(changelog.run {
                getLatest()
            }, Changelog.OutputType.HTML)
        })
    }
    buildSearchableOptions {
        enabled = prop("enableBuildSearchableOptions").toBoolean()
    }
    configurations.runtimeClasspath.get().forEach {
        println(it)
    }
    jar {
        from({
            configurations.runtimeClasspath.get().filter { file ->
                !file.nameWithoutExtension.startsWith("kotlin-stdlib") &&
                        !file.nameWithoutExtension.startsWith("annotations")
            }.map {
                if (it.isDirectory) it
                else zipTree(it)
            }
        })
    }
}

fun prop(name: String, default: (() -> String?)? = null) = extra.properties[name] as? String
    ?: default?.invoke() ?: error("Property `$name` is not defined in gradle.properties")

fun generateParser(language: String, suffix: String = "", config: GenerateParserTask.() -> Unit = {}) =
    task<GenerateParserTask>("generate${language.capitalized()}Parser${suffix.capitalized()}") {
        sourceFile.set(file("src/main/grammar/${language}Parser.bnf"))
        targetRootOutputDir.set(file("src/gen"))
        pathToParser.set("/org/ton/intellij/${language.lowercase()}/parser/${language}Parser.java")
        pathToPsiRoot.set("/org/ton/intellij/${language.lowercase()}/psi")
        purgeOldFiles.set(true)
        config()
    }

fun generateLexer(language: String) = task<GenerateLexerTask>("generate${language}Lexer") {
    sourceFile.set(file("src/main/grammar/${language}Lexer.flex"))
    targetOutputDir.set(file("src/gen/org/ton/intellij/${language.lowercase()}/lexer"))
    purgeOldFiles.set(true)
}
