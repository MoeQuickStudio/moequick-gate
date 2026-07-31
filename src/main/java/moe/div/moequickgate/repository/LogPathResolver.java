package moe.div.moequickgate.repository;

import java.nio.file.Path;

/**
 * 按照 XDG 规范解析操作日志路径。
 * Resolves the operation log path according to XDG conventions.
 */
public final class LogPathResolver {
    private static final String APPLICATION_DIRECTORY = "moequick-gate";
    private static final String LOG_FILE = "operations.log";

    private LogPathResolver() {
    }

    public static Path resolveDefault() {
        return resolve(System.getenv("XDG_STATE_HOME"), System.getProperty("user.home"));
    }

    static Path resolve(String xdgStateHome, String userHome) {
        Path stateHome;
        if (xdgStateHome != null
                && !xdgStateHome.isBlank()
                && Path.of(xdgStateHome).isAbsolute()) {
            stateHome = Path.of(xdgStateHome);
        } else {
            stateHome = Path.of(userHome).toAbsolutePath().resolve(".local").resolve("state");
        }
        return stateHome.resolve(APPLICATION_DIRECTORY).resolve(LOG_FILE);
    }
}
