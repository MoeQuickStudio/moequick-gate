plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "4.0.2"
}

group = "moe.div"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainModule = "moe.div.moequickgate"
    mainClass = "moe.div.moequickgate.App"
}

javafx {
    version = "21.0.12"
    modules("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.53.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

jlink {
    options = listOf("--strip-debug", "--no-header-files", "--no-man-pages")

    launcher {
        name = "moequick-gate"
    }

    jpackage {
        imageName = "MoeQuick Gate"
        installerName = "moequick-gate"
        installerType = "deb"
        appVersion = project.version.toString()
        vendor = "moe.div"
        installerOptions = listOf(
            "--linux-package-name", "moequick-gate",
            "--description", "MoeQuick Gate developer network assistant"
        )
    }
}
