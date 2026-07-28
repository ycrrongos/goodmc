allprojects {
    group = "com.goodmc"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
    }

    if (name != "server-fakeplayer") {
        dependencies {
            "compileOnly"("io.papermc.paper:paper-api:26.2.build.+")
        }
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.named<ProcessResources>("processResources") {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    tasks.named<Jar>("jar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
