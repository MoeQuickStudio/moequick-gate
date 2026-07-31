package moe.div.moequickgate.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import moe.div.moequickgate.bean.OperationLogEntry;
import moe.div.moequickgate.bean.OperationLogEntry.Action;
import moe.div.moequickgate.bean.OperationLogEntry.Result;
import moe.div.moequickgate.bean.OperationLogEntry.Trigger;
import moe.div.moequickgate.bean.ProxyComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TextLogRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void writesSingleSanitizedUtf8LineWithPrivatePermissions() throws IOException {
        Path logPath = temporaryDirectory.resolve("state/moequick-gate/operations.log");
        TextLogRepository repository = new TextLogRepository(logPath);

        repository.append(entry(1, "failed\nwith\tcontrols\u0000"));

        List<String> lines = Files.readAllLines(logPath);
        assertEquals(1, lines.size());
        assertTrue(lines.getFirst().contains("component=APT"));
        assertTrue(lines.getFirst().contains("trigger=TOGGLE"));
        assertTrue(lines.getFirst().contains("failed with controls"));
        assertFalse(lines.getFirst().contains("\t"));
        if (Files.getFileStore(logPath).supportsFileAttributeView("posix")) {
            assertEquals(
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                    Files.getPosixFilePermissions(logPath));
        }
    }

    @Test
    void trimsOldCompleteLinesAndNeverExceedsTwoHundredKilobytes() throws IOException {
        Path logPath = temporaryDirectory.resolve("operations.log");
        TextLogRepository repository = new TextLogRepository(logPath);
        String detail = "x".repeat(2000);

        for (int index = 0; index < 180; index++) {
            repository.append(entry(index, "entry-" + index + "-" + detail));
        }

        byte[] content = Files.readAllBytes(logPath);
        String text = Files.readString(logPath);
        assertTrue(content.length <= TextLogRepository.MAX_LOG_BYTES);
        assertTrue(text.contains("entry-179-"));
        assertFalse(text.contains("entry-0-"));
        assertTrue(text.endsWith("\n"));
        assertTrue(text.lines().allMatch(line -> line.startsWith("2026-01-01T00:00:")));
    }

    @Test
    void serializesConcurrentWritersWithoutLosingLines() throws Exception {
        Path logPath = temporaryDirectory.resolve("operations.log");
        List<Callable<Void>> writes = new ArrayList<>();
        for (int index = 0; index < 80; index++) {
            int sequence = index;
            writes.add(() -> {
                new TextLogRepository(logPath).append(entry(sequence, "concurrent-" + sequence));
                return null;
            });
        }

        try (var executor = Executors.newFixedThreadPool(8)) {
            executor.invokeAll(writes).forEach(future -> {
                try {
                    future.get();
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            });
        }

        String text = Files.readString(logPath);
        assertEquals(80, text.lines().count());
        for (int index = 0; index < 80; index++) {
            assertTrue(text.contains("concurrent-" + index));
        }
    }

    @Test
    void factoryFallsBackWhenParentPathIsAFile() throws IOException {
        Path regularFile = Files.writeString(temporaryDirectory.resolve("blocked"), "blocked");
        Path logPath = regularFile.resolve("operations.log");

        LogRepositoryContext context = LogRepositoryFactory.create(logPath);

        assertFalse(context.available());
        assertTrue(context.warning().contains("操作日志不可用"));
        context.repository().append(entry(1, "ignored"));
    }

    private static OperationLogEntry entry(int sequence, String detail) {
        return new OperationLogEntry(
                OffsetDateTime.of(2026, 1, 1, 0, 0, sequence % 60, 0, ZoneOffset.ofHours(8)),
                ProxyComponent.APT,
                Trigger.TOGGLE,
                Action.ENABLE,
                "Local",
                "HTTP",
                "127.0.0.1:7890",
                Result.SUCCESS,
                sequence,
                detail);
    }
}
