plugins {
    // Applies the right Loom variant for the Minecraft version being built.
    id("dev.kikugie.loom-back-compat")
}

// The jar is named sync-<mod version>+mc<compatible Minecraft versions>.jar,
// e.g. sync-6.0.0+mc1.20-1.20.1.jar
version = "${sc.properties["mod.version"]}+mc${sc.properties["mod.mc_label"]}"
base.archivesName = sc.properties["mod.id"]

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Mojang's names on obfuscated versions (26.1+ ships with them).
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${sc.properties["deps.fabric_loader"]}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties["deps.fabric_api"]}")

    // Team Reborn Energy is bundled inside the jar.
    modImplementation("teamreborn:energy:${sc.properties["deps.energy"]}")
    include("teamreborn:energy:${sc.properties["deps.energy"]}")

    // Optional integrations: compiled against, never required at runtime.
    modCompileOnly("maven.modrinth:cloth-config:${sc.properties["deps.cloth_config"]}")
    modCompileOnly("maven.modrinth:modmenu:${sc.properties["deps.modmenu"]}")
    compileOnly("org.apiguardian:apiguardian-api:1.1.2")
}

/**
 * The access widener for this Minecraft version: lines ending in "# if <predicate>" are kept only
 * when the predicate matches, and 26.1+ (unobfuscated) uses the "official" namespace.
 */
fun processAccessWidener(text: String): String {
    val conditional = Regex("""^(.*?)\s*#\s*if\s+(\S+)\s*$""")
    return text.lines().mapNotNull { line ->
        val match = conditional.find(line) ?: return@mapNotNull line
        val (entry, predicate) = match.destructured
        val (operator, version) = Regex("""^(>=|<=|>|<|=)?(.+)$""").find(predicate)!!.destructured
        val current = sc.current.parsed
        val keep = when (operator) {
            ">=" -> current >= version
            "<=" -> current <= version
            ">" -> current > version
            "<" -> current < version
            else -> current.matches(version)
        }
        if (keep) entry else null
    }.joinToString("\n").let {
        if (sc.current.parsed >= "26.1") it.replace("accessWidener v2 named", "accessWidener v2 official") else it
    }
}

val processedAccessWidener = layout.buildDirectory.file("processed.accesswidener").get().asFile.apply {
    parentFile.mkdirs()
    writeText(processAccessWidener(rootProject.file("src/main/resources/sync.accesswidener").readText()))
}

// The radial menu's MSAA framebuffer uses raw OpenGL, which newer render backends don't allow.
if (sc.current.parsed >= "1.21.2") {
    sourceSets.main { java.exclude("**/client/gl/**") }
}
// "Special" item models (used to draw machines as items) only exist since 1.21.4.
if (sc.current.parsed < "1.21.4") {
    sourceSets.main { java.exclude("**/client/render/item/**") }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    accessWidenerPath = processedAccessWidener

    runConfigs.all {
        preferGradleTask = true
        runDirectory = rootProject.file("run")
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        val props = mapOf(
            "version" to sc.properties["mod.version"],
            "minecraft" to sc.properties["mod.mc_compat"],
            "loader" to Regex("\\d+\\.\\d+").find(sc.properties["deps.fabric_loader"])!!.value,
            "java" to requiredJava.majorVersion,
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
        filesMatching("sync.accesswidener") { filter { processAccessWidener(it) } }

        val mixinJava = mapOf("java" to "JAVA_${requiredJava.majorVersion}")
        inputs.properties(mixinJava)
        filesMatching("*.mixins.json") { expand(mixinJava) }

        // The data files in src/ use the current (1.21+) format; older versions need the old layout.
        val mcVersion = sc.current.parsed
        val legacyFolderNames = mcVersion < "1.21"
        val legacyJson = mcVersion < "1.20.5"
        inputs.property("legacyFolderNames", legacyFolderNames)
        inputs.property("legacyJson", legacyJson)
        val noBuiltinEntityModel = mcVersion >= "1.21.4"
        inputs.property("noBuiltinEntityModel", noBuiltinEntityModel)
        if (noBuiltinEntityModel) {
            filesMatching("assets/sync/models/item/*.json") {
                filter { line -> if (line.contains("\"parent\": \"builtin/entity\"")) "" else line }
            }
        }

        val stringIngredients = mcVersion >= "1.21.2"
        inputs.property("stringIngredients", stringIngredients)
        doLast {
            val data = destinationDir.resolve("data")
            if (stringIngredients) {
                // Since 1.21.2 recipe ingredients are plain ids ("minecraft:stone") instead of {"item": ...}.
                val itemIngredient = Regex("\\{\\s*\"item\":\\s*(\"[^\"]+\")\\s*}")
                data.resolve("sync/recipe").listFiles()?.forEach { file ->
                    file.writeText(itemIngredient.replace(file.readText()) { it.groupValues[1] })
                }
            }
            if (legacyJson) {
                // Recipe results were {"item": ...} and silk-touch checks used the old item predicate.
                data.resolve("sync/recipe").listFiles()?.forEach { file ->
                    file.writeText(file.readText().replace("\"id\":", "\"item\":"))
                }
                val enchantmentPredicate = Regex("\"predicates\":\\s*\\{\\s*\"minecraft:enchantments\":\\s*(\\[.*?])\\s*}", RegexOption.DOT_MATCHES_ALL)
                data.resolve("sync/loot_table/blocks").listFiles()?.forEach { file ->
                    val text = enchantmentPredicate.replace(file.readText()) { match ->
                        "\"enchantments\": " + match.groupValues[1].replace("\"enchantments\":", "\"enchantment\":")
                    }
                    file.writeText(text)
                }
            }
            if (legacyFolderNames) {
                // Data pack folders were plural before 1.21.
                listOf("sync/recipe" to "sync/recipes", "sync/loot_table" to "sync/loot_tables", "minecraft/tags/block" to "minecraft/tags/blocks").forEach { (from, to) ->
                    val source = data.resolve(from)
                    if (source.exists()) {
                        val target = data.resolve(to)
                        target.deleteRecursively()
                        source.renameTo(target)
                    }
                }
            }
        }
    }

    withType<Jar> {
        from(rootProject.file("LICENSE.md")) { rename { "LICENSE_${sc.properties["mod.id"]}.md" } }
    }

    // Builds this version's jar and copies it to the root build/libs/<mod version>/ folder.
    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/<mod version>/"
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${sc.properties["mod.version"]}"))
    }
}
