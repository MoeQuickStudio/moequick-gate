package moe.div.moequickgate.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LogPathResolverTest {
    @Test
    void usesAbsoluteXdgStateHome() {
        assertEquals(
                Path.of("/tmp/state/moequick-gate/operations.log"),
                LogPathResolver.resolve("/tmp/state", "/home/tester"));
    }

    @Test
    void fallsBackForMissingOrRelativeXdgStateHome() {
        Path expected = Path.of("/home/tester/.local/state/moequick-gate/operations.log");

        assertEquals(expected, LogPathResolver.resolve(null, "/home/tester"));
        assertEquals(expected, LogPathResolver.resolve("relative/state", "/home/tester"));
    }
}
