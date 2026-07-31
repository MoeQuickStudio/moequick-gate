package moe.div.moequickgate.proxy.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyProtocol;
import moe.div.moequickgate.proxy.ProxyOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NPMProxyImplTest {
    @TempDir
    Path temporaryDirectory;

    private Path npm;
    private FakeNpmRunner runner;

    @BeforeEach
    void setUp() throws IOException {
        npm = Files.createFile(temporaryDirectory.resolve("npm"));
        assertTrue(npm.toFile().setExecutable(true));
        runner = new FakeNpmRunner();
    }

    @Test
    void enablesAndForcesUserConfigToDirectOnDisable() {
        runner.values.put("registry", "https://registry.npmjs.org/");
        NPMProxyImpl service = new NPMProxyImpl(npm, runner, Map.of());

        service.enable(new MoeProxy(1, "Local", "localhost", 7890, ProxyProtocol.HTTP));
        assertEquals("http://localhost:7890/", runner.values.get("proxy"));
        assertEquals("http://localhost:7890/", runner.values.get("https-proxy"));

        service.disable();
        assertNull(runner.values.get("proxy"));
        assertNull(runner.values.get("https-proxy"));
        assertEquals("https://registry.npmjs.org/", runner.values.get("registry"));
    }

    @Test
    void reportsEnvironmentProxyAsEffectiveOtherConfiguration() {
        NPMProxyImpl service = new NPMProxyImpl(
                npm, runner, Map.of("HTTP_PROXY", "http://environment:8080"));

        var status = service.check();

        assertEquals("http://environment:8080", status.routes().get("http"));
        assertEquals("http://environment:8080", status.routes().get("https"));
        assertTrue(status.detail().contains("环境变量"));
    }

    @Test
    void restoresBothValuesWhenSecondWriteFails() {
        runner.values.put("proxy", "http://old:1/");
        runner.values.put("https-proxy", "https://old:2/");
        runner.failNextHttpsSet = true;
        NPMProxyImpl service = new NPMProxyImpl(npm, runner, Map.of());

        assertThrows(ProxyOperationException.class, () -> service.enable(
                new MoeProxy(1, "New", "new-host", 7890, ProxyProtocol.HTTP)));

        assertEquals("http://old:1/", runner.values.get("proxy"));
        assertEquals("https://old:2/", runner.values.get("https-proxy"));
    }

    @Test
    void marksNpmUnavailableWhenExecutableIsMissing() {
        NPMProxyImpl service = new NPMProxyImpl(
                temporaryDirectory.resolve("missing"), runner, Map.of());

        assertFalse(service.check().available());
        assertThrows(ProxyOperationException.class, service::disable);
    }

    private static final class FakeNpmRunner implements ProcessRunner {
        private final Map<String, String> values = new HashMap<>();
        private boolean failNextHttpsSet;

        @Override
        public ProcessResult run(List<String> command, Duration timeout) {
            String operation = command.get(2);
            String key = command.get(3);
            if (operation.equals("get")) {
                return new ProcessResult(0, values.getOrDefault(key, "null") + "\n", false);
            }
            if (operation.equals("set")) {
                if (key.equals("https-proxy") && failNextHttpsSet) {
                    failNextHttpsSet = false;
                    return new ProcessResult(1, "simulated failure", false);
                }
                String value = command.get(4);
                if (value.equals("null")) {
                    values.remove(key);
                } else {
                    values.put(key, value);
                }
                return new ProcessResult(0, "", false);
            }
            return new ProcessResult(1, "unexpected command", false);
        }
    }
}
