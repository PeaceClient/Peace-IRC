plugins {
    id("java")
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.peace"
version = "2.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.google.code.gson:gson:2.14.0")
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("org.apache.logging.log4j:log4j-core:2.25.4")
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "com.peace.Main")
    }
}

tasks.test {
    useJUnitPlatform()
}