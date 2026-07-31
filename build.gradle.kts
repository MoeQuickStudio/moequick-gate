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
        imageName = "moequick-gate"
        installerName = "moequick-gate"
        installerType = "deb"
        appVersion = project.version.toString()
        vendor = "MoeQuickStudio"
        setResourceDir(layout.projectDirectory.dir("src/main/jpackage").asFile)
        icon = layout.projectDirectory.file(
            "src/main/resources/icon/moequick-gate.png"
        ).asFile.absolutePath
        installerOptions = listOf(
            "--linux-package-name", "moequick-gate",
            "--linux-deb-maintainer", "linmo456@hotmmail.com",
            "--linux-app-category", "net",
            "--linux-package-deps", "policykit-1",
            "--linux-shortcut",
            "--license-file", layout.projectDirectory.file("LICENSE").asFile.absolutePath,
            "--about-url", "https://github.com/MoeQuickStudio/moequick-gate",
            "--copyright", "Copyright (c) 2026 MoeQuickStudio",
            "--description", "MoeQuick Gate developer network proxy manager"
        )
    }
}
