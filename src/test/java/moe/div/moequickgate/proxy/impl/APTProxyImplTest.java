package moe.div.moequickgate.proxy.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyProtocol;
import moe.div.moequickgate.proxy.ProxyOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class APTProxyImplTest {
    @TempDir
    Path temporaryDirectory;

    private Path target;
    private Path aptConfig;
    private Path pkexec;
    private Path install;
    private FakeRunner runner;
    private APTProxyImpl service;

    @BeforeEach
    void setUp() throws IOException {
        target = temporaryDirectory.resolve("99zz-moequick-gate");
        aptConfig = executable("apt-config");
        pkexec = executable("pkexec");
        install = executable("install");
        runner = new FakeRunner(aptConfig, pkexec, target);
        service = new APTProxyImpl(target, aptConfig, pkexec, install, runner);
    }

    @Test
    void detectsEffectiveAptProxy() {
        runner.aptConfigOutput = "Acquire::http::Proxy \"http://localhost:7890/\";\n"
                + "Acquire::https::Proxy \"DIRECT\";\n";

        var status = service.check();

        assertTrue(status.available());
        assertEquals("http://localhost:7890/", status.routes().get("http"));
        assertFalse(status.routes().containsKey("https"));
    }

    @Test
    void enablesAndDisablesThroughParameterizedPkexecInstall() throws IOException {
        service.enable(new MoeProxy(1, "Local", "::1", 8443, ProxyProtocol.HTTPS));

        String enabled = Files.readString(target);
        assertTrue(enabled.contains("Acquire::http::Proxy \"https://[::1]:8443/\";"));
        assertTrue(enabled.contains("Acquire::https::Proxy \"https://[::1]:8443/\";"));
        assertEquals(pkexec.toString(), runner.commands.get(0).get(0));
        assertEquals(install.toString(), runner.commands.get(0).get(1));
        assertFalse(runner.commands.get(0).contains("sh"));

        service.disable();

        String disabled = Files.readString(target);
        assertTrue(disabled.contains("Acquire::http::Proxy \"DIRECT\";"));
        assertTrue(disabled.contains("Acquire::https::Proxy \"DIRECT\";"));
    }

    @Test
    void reportsAuthorizationCancellationAndRejectsUnsafeHost() {
        runner.installExitCode = 126;
        ProxyOperationException cancelled = assertThrows(
                ProxyOperationException.class, service::disable);
        assertTrue(cancelled.getMessage().contains("授权已取消"));

        runner.installExitCode = 0;
        assertThrows(ProxyOperationException.class, () -> service.enable(
                new MoeProxy(1, "Unsafe", "host;Injected", 7890, ProxyProtocol.HTTP)));
    }

    @Test
    void marksAptUnavailableWhenAptConfigIsMissing() throws IOException {
        Files.delete(aptConfig);

        assertFalse(service.check().available());
    }

    @Test
    void reportsCheckTimeout() {
        runner.aptConfigTimedOut = true;

        ProxyOperationException failure = assertThrows(ProxyOperationException.class, service::check);

        assertTrue(failure.getMessage().contains("超时"));
    }

    private Path executable(String name) throws IOException {
        Path path = Files.createFile(temporaryDirectory.resolve(name));
        assertTrue(path.toFile().setExecutable(true));
        return path;
    }

    private static final class FakeRunner implements ProcessRunner {
        private final Path aptConfig;
        private final Path pkexec;
        private final Path target;
        private final List<List<String>> commands = new ArrayList<>();
        private String aptConfigOutput = "";
        private boolean aptConfigTimedOut;
        private int installExitCode;

        private FakeRunner(Path aptConfig, Path pkexec, Path target) {
            this.aptConfig = aptConfig;
            this.pkexec = pkexec;
            this.target = target;
        }

        @Override
        public ProcessResult run(List<String> command, Duration timeout) throws IOException {
            commands.add(List.copyOf(command));
            if (command.get(0).equals(aptConfig.toString())) {
                if (aptConfigTimedOut) {
                    return new ProcessResult(-1, "", true);
                }
                return new ProcessResult(0, aptConfigOutput, false);
            }
            if (command.get(0).equals(pkexec.toString()) && installExitCode == 0) {
                Files.copy(Path.of(command.get(command.size() - 2)), target,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return new ProcessResult(installExitCode, "authorization result", false);
        }
    }
}
