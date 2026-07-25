plugins {
    java
    id("com.gradleup.shadow") version "9.6.0"
}

group = "net.glaced"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    implementation("fr.mrmicky:fastboard:2.2.0")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")

    // Folia API als normale compileOnly-Dependency
    compileOnly("dev.folia:folia-api:1.20.4-R0.1-SNAPSHOT")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("fr.mrmicky.fastboard", "net.glaced.desertjunglecore.lib.fastboard")
    }

    build {
        dependsOn(shadowJar)
    }

    assemble {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to project.version)
        filesMatching("plugin.yml") { expand(props) }
    }
}
