plugins {
    // Use kotlin plugin from the root project
    kotlin("jvm")
}

group = "com.slothiesmooth"
version = "1.0.0"

repositories {
    mavenCentral()
}
dependencies {
    implementation("com.slothiesmooth:links-detektor:2.0.0")
}
