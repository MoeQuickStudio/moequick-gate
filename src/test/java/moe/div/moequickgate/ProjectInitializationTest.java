package moe.div.moequickgate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import moe.div.moequickgate.scene.MainScene;
import org.junit.jupiter.api.Test;

class ProjectInitializationTest {
    @Test
    void mainFxmlIsAvailableFromTheModulePath() {
        assertNotNull(MainScene.class.getResource(MainScene.FXML_RESOURCE));
    }

    @Test
    void compiledApplicationDeclaresTheExpectedNamedModule() {
        var applicationModule = ModuleFinder.of(Path.of("build/classes/java/main"))
                .find("moe.div.moequickgate");

        assertTrue(applicationModule.isPresent());
        assertEquals("moe.div.moequickgate", applicationModule.orElseThrow().descriptor().name());
    }
}
