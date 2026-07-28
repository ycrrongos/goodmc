dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.jar {
    archiveFileName.set("QQBridge-${project.version}.jar")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}
