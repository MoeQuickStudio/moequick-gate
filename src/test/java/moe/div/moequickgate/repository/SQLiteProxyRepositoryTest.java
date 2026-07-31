package moe.div.moequickgate.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyProtocol;
import moe.div.moequickgate.database.SQLiteHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SQLiteProxyRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsSeedOnceAndRestoresSelectionWhenReopened() {
        Path database = temporaryDirectory.resolve("seed.db");

        SQLiteProxyRepository first = new SQLiteProxyRepository(new SQLiteHelper(database));
        assertEquals(1, first.findAll().size());
        assertEquals("Clash 本机监听", first.findAll().getFirst().getName());
        assertEquals(first.findAll().getFirst().getId(), first.findSelectedId().orElseThrow());

        SQLiteProxyRepository reopened = new SQLiteProxyRepository(new SQLiteHelper(database));
        assertEquals(1, reopened.findAll().size());
        assertTrue(reopened.findSelectedId().isPresent());
    }

    @Test
    void supportsCrudDuplicateNamesAndPersistentSelection() {
        Path database = temporaryDirectory.resolve("crud.db");
        SQLiteProxyRepository repository = new SQLiteProxyRepository(new SQLiteHelper(database));
        MoeProxy seed = repository.findAll().getFirst();

        MoeProxy duplicate = repository.create(
                new MoeProxy(0, seed.getName(), "localhost", 8080, ProxyProtocol.HTTPS));
        assertEquals(2, repository.findAll().size());

        repository.update(new MoeProxy(
                duplicate.getId(), "Edited", "::1", 1080, ProxyProtocol.SOCKS5));
        MoeProxy updated = repository.findAll().stream()
                .filter(proxy -> proxy.getId() == duplicate.getId())
                .findFirst()
                .orElseThrow();
        assertEquals("Edited", updated.getName());
        assertEquals("::1", updated.getHost());

        repository.select(duplicate.getId());
        SQLiteProxyRepository reopened = new SQLiteProxyRepository(new SQLiteHelper(database));
        assertEquals(duplicate.getId(), reopened.findSelectedId().orElseThrow());

        reopened.deleteById(duplicate.getId());
        assertFalse(reopened.findSelectedId().isPresent());
        assertEquals(1, reopened.findAll().size());
    }

    @Test
    void selectsNewProfileOnlyWhenListWasEmpty() {
        Path database = temporaryDirectory.resolve("empty.db");
        SQLiteProxyRepository repository = new SQLiteProxyRepository(new SQLiteHelper(database));
        long seedId = repository.findAll().getFirst().getId();
        repository.deleteById(seedId);

        assertTrue(repository.findAll().isEmpty());
        assertFalse(repository.findSelectedId().isPresent());

        MoeProxy first = repository.create(
                new MoeProxy(0, "First", "127.0.0.1", 7890, ProxyProtocol.HTTP));
        assertEquals(first.getId(), repository.findSelectedId().orElseThrow());

        repository.create(new MoeProxy(0, "Second", "127.0.0.1", 7891, ProxyProtocol.HTTP));
        assertEquals(first.getId(), repository.findSelectedId().orElseThrow());
    }
}
