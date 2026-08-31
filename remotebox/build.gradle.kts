plugins {
    application
}

group = "net.tawkit"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("com.formdev:flatlaf:3.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
}

application {
    mainClass.set("net.tawkit.remotebox.App")
    applicationName = "remotebox"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named<JavaExec>("run") {
    // Let the app find a display / keep console output readable
    jvmArgs("-Dfile.encoding=UTF-8")
}
