package moe.div.moequickgate.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProxyRepositoryFactoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void fallsBackToSeededMemoryRepositoryWhenDatabaseCannotBeCreated() throws IOException {
        Path regularFile = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(regularFile, "blocked");
        Path invalidDatabase = regularFile.resolve("moequick-gate.db");

        RepositoryContext context = ProxyRepositoryFactory.create(invalidDatabase);

        assertFalse(context.persistent());
        assertTrue(context.warning().contains("当前更改不会保存"));
        assertTrue(context.warning().contains(invalidDatabase.toString()));
        assertEquals(1, context.repository().findAll().size());
        assertTrue(context.repository().findSelectedId().isPresent());
    }
}
