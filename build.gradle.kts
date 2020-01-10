plugins {
    java
    maven
}

group = "io.jsq.smithy"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("software.amazon.smithy:smithy-build:0.9.5")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.4.0")
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Use Junit5's test runner.
tasks.withType<Test> {
    useJUnitPlatform()
}
