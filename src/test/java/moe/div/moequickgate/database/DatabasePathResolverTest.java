package moe.div.moequickgate.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DatabasePathResolverTest {
    @Test
    void usesAbsoluteXdgDataHome() {
        Path resolved = DatabasePathResolver.resolve("/tmp/xdg-data", "/home/example");

        assertEquals(
                Path.of("/tmp/xdg-data/moequick-gate/moequick-gate.db"),
                resolved);
    }

    @Test
    void fallsBackToUserDataDirectoryForRelativeXdgValue() {
        Path resolved = DatabasePathResolver.resolve("relative/path", "/home/example");

        assertEquals(
                Path.of("/home/example/.local/share/moequick-gate/moequick-gate.db"),
                resolved);
    }
}
