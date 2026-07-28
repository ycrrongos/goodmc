plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

dependencies {
    paperweight.paperDevBundle("26.2.build.+")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks.jar {
    archiveFileName.set("Server-FakePlayer-${project.version}.jar")
}
