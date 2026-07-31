package moe.div.moequickgate.repository;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Objects;
import moe.div.moequickgate.bean.OperationLogEntry;

/**
 * 单文件、定长上限的 UTF-8 操作日志。
 * Single-file UTF-8 operation log with a strict size limit.
 */
public final class TextLogRepository implements LogRepository {
    public static final int MAX_LOG_BYTES = 200 * 1024;
    private static final int MAX_FIELD_CHARACTERS = 2048;
    private static final Object JVM_FILE_LOCK = new Object();

    private final Path logPath;

    public TextLogRepository(Path logPath) {
        this.logPath = Objects.requireNonNull(logPath).toAbsolutePath().normalize();
        synchronized (JVM_FILE_LOCK) {
            initialize();
        }
    }

    @Override
    public void append(OperationLogEntry entry) {
        byte[] newLine = format(entry).getBytes(StandardCharsets.UTF_8);
        synchronized (JVM_FILE_LOCK) {
            try (FileChannel channel = FileChannel.open(
                    logPath,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
                    var ignored = channel.lock()) {
                byte[] existing = readRecentCompleteLines(channel);
                byte[] retained = retainNewest(existing, newLine);
                channel.truncate(0);
                channel.position(0);
                writeFully(channel, retained);
                channel.force(true);
            } catch (IOException exception) {
                throw new LogRepositoryException("无法写入操作日志：" + logPath, exception);
            }
        }
    }

    @Override
    public Path getLogPath() {
        return logPath;
    }

    private void initialize() {
        try {
            Path directory = logPath.getParent();
            boolean directoryExisted = Files.exists(directory);
            Files.createDirectories(directory);
            if (!directoryExisted) {
                setPermissions(directory, EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE));
            }
            if (Files.notExists(logPath)) {
                Files.createFile(logPath);
                setPermissions(logPath, EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE));
            }
            if (!Files.isRegularFile(logPath)) {
                throw new IOException("日志路径不是普通文件");
            }
        } catch (IOException exception) {
            throw new LogRepositoryException("无法初始化操作日志：" + logPath, exception);
        }
    }

    private byte[] readRecentCompleteLines(FileChannel channel) throws IOException {
        long size = channel.size();
        long start = Math.max(0, size - MAX_LOG_BYTES);
        channel.position(start);
        ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(MAX_LOG_BYTES, size));
        while (buffer.hasRemaining() && channel.read(buffer) != -1) {
            // 持续读取最近的日志窗口。 / Keep reading the recent log window.
        }
        byte[] bytes = new byte[buffer.position()];
        buffer.flip();
        buffer.get(bytes);
        if (start == 0 || bytes.length == 0) {
            return bytes;
        }
        int firstNewline = indexOfNewline(bytes, 0);
        if (firstNewline < 0 || firstNewline + 1 >= bytes.length) {
            return new byte[0];
        }
        return java.util.Arrays.copyOfRange(bytes, firstNewline + 1, bytes.length);
    }

    private static byte[] retainNewest(byte[] existing, byte[] newLine) {
        if (newLine.length >= MAX_LOG_BYTES) {
            return java.util.Arrays.copyOfRange(
                    newLine, newLine.length - MAX_LOG_BYTES, newLine.length);
        }
        byte[] combined = new byte[existing.length + newLine.length];
        System.arraycopy(existing, 0, combined, 0, existing.length);
        System.arraycopy(newLine, 0, combined, existing.length, newLine.length);
        if (combined.length <= MAX_LOG_BYTES) {
            return combined;
        }
        int minimumStart = combined.length - MAX_LOG_BYTES;
        int newline = indexOfNewline(combined, minimumStart);
        int start = newline < 0 ? existing.length : newline + 1;
        return java.util.Arrays.copyOfRange(combined, start, combined.length);
    }

    private static void writeFully(FileChannel channel, byte[] content) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(content);
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private static int indexOfNewline(byte[] bytes, int start) {
        for (int index = Math.max(0, start); index < bytes.length; index++) {
            if (bytes[index] == '\n') {
                return index;
            }
        }
        return -1;
    }

    private static String format(OperationLogEntry entry) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(entry.timestamp())
                + " | component=" + clean(entry.component().name())
                + " | trigger=" + clean(entry.trigger().name())
                + " | action=" + clean(entry.action().name())
                + " | proxy=" + clean(proxySummary(entry))
                + " | result=" + clean(entry.result().name())
                + " | durationMs=" + Math.max(0, entry.durationMillis())
                + " | detail=" + clean(entry.detail())
                + '\n';
    }

    private static String proxySummary(OperationLogEntry entry) {
        if (entry.proxyName() == null || entry.proxyName().isBlank()) {
            return "none";
        }
        return entry.proxyName() + " (" + nullToEmpty(entry.proxyProtocol())
                + " " + nullToEmpty(entry.proxyEndpoint()) + ")";
    }

    private static String clean(String value) {
        StringBuilder cleaned = new StringBuilder();
        String source = nullToEmpty(value);
        source.codePoints().forEach(codePoint -> {
            if (cleaned.length() >= MAX_FIELD_CHARACTERS) {
                return;
            }
            if (codePoint == '\n' || codePoint == '\r' || codePoint == '\t'
                    || Character.isISOControl(codePoint)) {
                cleaned.append(' ');
            } else {
                cleaned.appendCodePoint(codePoint);
            }
        });
        return cleaned.toString().strip();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void setPermissions(Path path, EnumSet<PosixFilePermission> permissions) {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException ignored) {
            // 目标平台支持 POSIX；其他文件系统保留默认权限。
            // The target supports POSIX; other file systems retain default permissions.
        } catch (IOException exception) {
            throw new LogRepositoryException("无法设置日志权限：" + path, exception);
        }
    }
}
