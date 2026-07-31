package moe.div.moequickgate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleFinder;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import moe.div.moequickgate.controller.MainController;
import moe.div.moequickgate.scene.MainScene;
import org.junit.jupiter.api.Test;

class ProjectInitializationTest {
    @Test
    void mainFxmlIsAvailableFromTheModulePath() {
        assertNotNull(App.class.getResource("/icon/moequick-gate.png"));
        assertNotNull(MainScene.class.getResource(MainScene.FXML_RESOURCE));
        assertNotNull(MainController.class.getResource(MainController.PROXY_CARD_RESOURCE));
        assertNotNull(MainController.class.getResource(MainController.PROXY_FORM_RESOURCE));
        assertNotNull(MainScene.class.getResource(MainScene.CSS_RESOURCE));
    }

    @Test
    void releasePackagingDeclaresDesktopIntegrationMetadata() throws Exception {
        String buildScript = Files.readString(Path.of("build.gradle.kts"));
        String desktopEntry = Files.readString(
                Path.of("src/main/jpackage/moequick-gate.desktop"));

        assertTrue(buildScript.contains("imageName = \"moequick-gate\""));
        assertTrue(buildScript.contains("--linux-shortcut"));
        assertTrue(buildScript.contains("--linux-deb-maintainer"));
        assertTrue(buildScript.contains("--linux-package-deps\", \"policykit-1"));
        assertTrue(buildScript.contains("--license-file"));
        assertTrue(buildScript.contains("src/main/resources/icon/moequick-gate.png"));
        assertTrue(desktopEntry.contains("Name=MoeQuick Gate"));
        assertTrue(desktopEntry.contains("Categories=Network;"));
    }

    @Test
    void applicationIconIsA512PixelRgbaPng() throws Exception {
        byte[] icon = Files.readAllBytes(
                Path.of("src/main/resources/icon/moequick-gate.png"));
        ByteBuffer header = ByteBuffer.wrap(icon);

        assertEquals(0x89504E470D0A1A0AL, header.getLong());
        assertEquals(512, header.getInt(16));
        assertEquals(512, header.getInt(20));
        assertEquals(6, Byte.toUnsignedInt(icon[25]));
    }

    @Test
    void compiledApplicationDeclaresTheExpectedNamedModule() {
        var applicationModule = ModuleFinder.of(Path.of("build/classes/java/main"))
                .find("moe.div.moequickgate");

        assertTrue(applicationModule.isPresent());
        assertEquals("moe.div.moequickgate", applicationModule.orElseThrow().descriptor().name());
        assertTrue(applicationModule.orElseThrow().descriptor().opens().stream()
                .anyMatch(open -> open.source().equals("moe.div.moequickgate.controller")
                        && open.targets().contains("javafx.fxml")));
    }
}
