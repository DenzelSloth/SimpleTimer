plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    kotlin("jvm") version "2.4.10"
    `maven-publish`
}

val mod_version: String by project
val maven_group: String by project

version = mod_version
group = maven_group

repositories {
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://maven.shedaniel.me/") {
        name = "Shedaniel"
    }
    maven("https://maven.terraformersmc.com/releases/") {
        name = "Terraformers"
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("simpletimer") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

sourceSets {
    named("main") {
        java.srcDir("src/main/kotlin")
    }
    named("client") {
        java.srcDir("src/client/kotlin")
    }
}

val minecraft_version: String by project
val loader_version: String by project
val fabric_api_version: String by project
val fabric_language_kotlin_version: String by project
val modmenu_version: String by project
val cloth_config_version: String by project

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    implementation("net.fabricmc:fabric-loader:$loader_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_api_version")
    implementation("net.fabricmc:fabric-language-kotlin:$fabric_language_kotlin_version")

    compileOnly("maven.modrinth:modmenu:$modmenu_version")
    compileOnly("me.shedaniel.cloth:cloth-config-fabric:$cloth_config_version") {
        exclude(group = "net.fabricmc.fabric-api")
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

kotlin {
    jvmToolchain(25)
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)

    from("LICENSE") {
        rename { "${it}_$projectName" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
