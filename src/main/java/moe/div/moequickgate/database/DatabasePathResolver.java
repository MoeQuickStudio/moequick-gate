package moe.div.moequickgate.database;

import java.nio.file.Path;

/**
 * 按照 XDG 规范解析应用数据库路径。
 * Resolves the application database path according to XDG conventions.
 */
public final class DatabasePathResolver {
    private static final String APPLICATION_DIRECTORY = "moequick-gate";
    private static final String DATABASE_FILE = "moequick-gate.db";

    private DatabasePathResolver() {
    }

    public static Path resolveDefault() {
        return resolve(System.getenv("XDG_DATA_HOME"), System.getProperty("user.home"));
    }

    static Path resolve(String xdgDataHome, String userHome) {
        Path dataHome;
        if (xdgDataHome != null && !xdgDataHome.isBlank() && Path.of(xdgDataHome).isAbsolute()) {
            dataHome = Path.of(xdgDataHome);
        } else {
            dataHome = Path.of(userHome).toAbsolutePath().resolve(".local").resolve("share");
        }
        return dataHome.resolve(APPLICATION_DIRECTORY).resolve(DATABASE_FILE);
    }
}
