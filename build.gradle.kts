import org.gradle.configurationcache.extensions.capitalized
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
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.16.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.1"
}

allprojects {
    apply(plugin = "kotlin")

    repositories {
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }
}

sourceSets {
    main {
        java.srcDirs("src/gen")
    }
}

idea {
    module {
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

val compileKotlin = tasks.named("compileKotlin") {
    dependsOn(
        generateFuncParser, generateFuncLexer,
        generateTactParser, generateTactLexer,
        generateFiftParser, generateFiftLexer,
        generateTlbParser, generateTlbLexer,
    )
}

val compileJava = tasks.named("compileJava")

tasks {
    runIde { enabled = true }
    prepareSandbox { enabled = true }
    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("")
    }
    buildSearchableOptions {
        enabled = prop("enableBuildSearchableOptions").toBoolean()
    }
}

fun prop(name: String, default: (() -> String?)? = null) = extra.properties[name] as? String
    ?: default?.invoke() ?: error("Property `$name` is not defined in gradle.properties")

fun generateParser(language: String, suffix: String = "", config: GenerateParserTask.() -> Unit = {}) =
    task<GenerateParserTask>("generate${language.capitalized()}Parser${suffix.capitalized()}") {
    sourceFile.set(file("src/main/grammar/${language}Parser.bnf"))
    targetRoot.set("src/gen")
        pathToParser.set("/org/ton/intellij/${language.lowercase()}/parser/${language}Parser.java")
        pathToPsiRoot.set("/org/ton/intellij/${language.lowercase()}/psi")
    purgeOldFiles.set(true)
        config()
}

fun generateLexer(language: String) = task<GenerateLexerTask>("generate${language}Lexer") {
    sourceFile.set(file("src/main/grammar/${language}Lexer.flex"))
    targetDir.set("src/gen/org/ton/intellij/${language.lowercase()}/lexer")
    targetClass.set("_${language}Lexer")
    purgeOldFiles.set(true)
}
