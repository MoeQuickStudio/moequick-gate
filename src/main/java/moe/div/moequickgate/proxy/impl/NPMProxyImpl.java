package moe.div.moequickgate.proxy.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyComponent;
import moe.div.moequickgate.proxy.IProxy;
import moe.div.moequickgate.proxy.ProxyOperationException;
import moe.div.moequickgate.proxy.ProxyRuntimeStatus;
import moe.div.moequickgate.proxy.ProxyUriFactory;

/**
 * 使用 npm 官方用户级配置命令控制 NPM 代理。
 * Controls NPM proxy settings through npm's official user-level config command.
 */
public final class NPMProxyImpl implements IProxy {
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(10);

    private final Path npmExecutable;
    private final ProcessRunner processRunner;
    private final Map<String, String> environment;

    public NPMProxyImpl() {
        this(findNpmExecutable(System.getenv()), new DefaultProcessRunner(), System.getenv());
    }

    NPMProxyImpl(
            Path npmExecutable, ProcessRunner processRunner, Map<String, String> environment) {
        this.npmExecutable = npmExecutable;
        this.processRunner = processRunner;
        this.environment = Map.copyOf(environment);
    }

    @Override
    public ProxyComponent getComponent() {
        return ProxyComponent.NPM;
    }

    @Override
    public ProxyRuntimeStatus check() {
        if (npmExecutable == null || !Files.isExecutable(npmExecutable)) {
            return ProxyRuntimeStatus.unavailable("未找到 npm 可执行文件。");
        }

        String configuredHttp = getConfig("proxy");
        String configuredHttps = getConfig("https-proxy");
        String environmentHttp = firstEnvironment("HTTP_PROXY", "http_proxy");
        String environmentHttps = firstEnvironment("HTTPS_PROXY", "https_proxy");

        Map<String, String> routes = new LinkedHashMap<>();
        putIfConfigured(routes, "http", environmentHttp != null ? environmentHttp : configuredHttp);
        putIfConfigured(routes, "https", environmentHttps != null
                ? environmentHttps
                : environmentHttp != null ? environmentHttp : configuredHttps);

        String detail = environmentHttp != null || environmentHttps != null
                ? "环境变量中的代理优先于 NPM 用户配置。"
                : "";
        return new ProxyRuntimeStatus(true, routes, detail);
    }

    @Override
    public void enable(MoeProxy proxy) {
        requireAvailable();
        String uri;
        try {
            uri = ProxyUriFactory.create(proxy);
        } catch (IllegalArgumentException exception) {
            throw failure("启用 NPM 代理失败：" + exception.getMessage(),
                    "修正代理地址或选择 HTTP/HTTPS 协议后重试。", exception);
        }
        replaceBoth(uri, uri);
    }

    @Override
    public void disable() {
        requireAvailable();
        replaceBoth(null, null);
    }

    private void replaceBoth(String httpValue, String httpsValue) {
        String previousHttp = getConfig("proxy");
        String previousHttps = getConfig("https-proxy");
        boolean httpChanged = false;
        try {
            setConfig("proxy", httpValue);
            httpChanged = true;
            setConfig("https-proxy", httpsValue);
        } catch (ProxyOperationException failure) {
            if (httpChanged) {
                try {
                    setConfig("proxy", previousHttp);
                    setConfig("https-proxy", previousHttps);
                } catch (ProxyOperationException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
            throw failure;
        }
    }

    private String getConfig(String key) {
        ProcessResult result = run(List.of(
                npmExecutable.toString(), "config", "get", key, "--location=user"));
        requireSuccess(result, "读取 NPM " + key + " 配置失败");
        String value = result.output().lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .reduce((previous, current) -> current)
                .orElse("");
        return normalize(value);
    }

    private void setConfig(String key, String value) {
        String storedValue = value == null ? "null" : value;
        ProcessResult result = run(List.of(
                npmExecutable.toString(), "config", "set", key, storedValue, "--location=user"));
        requireSuccess(result, "写入 NPM " + key + " 配置失败");
    }

    private ProcessResult run(List<String> command) {
        try {
            return processRunner.run(command, COMMAND_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw failure("NPM 操作被中断。", "重新执行操作。", exception);
        } catch (IOException exception) {
            throw failure("无法启动 npm：" + exception.getMessage(),
                    "安装 npm 并确认可执行文件位于 PATH。", exception);
        }
    }

    private void requireSuccess(ProcessResult result, String action) {
        if (result.timedOut()) {
            throw failure(action + "：操作超时。", "检查 npm 配置文件和系统负载后重试。", null);
        }
        if (result.exitCode() != 0) {
            String output = result.output().strip();
            throw failure(action + "（退出码 " + result.exitCode() + "）"
                    + (output.isEmpty() ? "" : "：" + output),
                    "检查用户级 .npmrc 权限和配置语法。", null);
        }
    }

    private void requireAvailable() {
        if (npmExecutable == null || !Files.isExecutable(npmExecutable)) {
            throw failure("未找到 npm 可执行文件。", "安装 npm 并重新启动应用。", null);
        }
    }

    private String firstEnvironment(String... names) {
        for (String name : names) {
            String value = normalize(environment.get(name));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private ProxyOperationException failure(String message, String suggestion, Throwable cause) {
        return cause == null
                ? new ProxyOperationException(getComponent(), message, suggestion)
                : new ProxyOperationException(getComponent(), message, suggestion, cause);
    }

    private static Path findNpmExecutable(Map<String, String> environment) {
        Path systemNpm = Path.of("/usr/bin/npm");
        if (Files.isExecutable(systemNpm)) {
            return systemNpm;
        }
        String pathValue = environment.getOrDefault("PATH", "");
        for (String entry : pathValue.split(PatternHolder.PATH_SEPARATOR)) {
            if (!entry.isBlank()) {
                Path candidate = Path.of(entry).resolve("npm");
                if (Files.isExecutable(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static void putIfConfigured(Map<String, String> routes, String key, String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            routes.put(key, normalized);
        }
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isEmpty()
                        || normalized.equalsIgnoreCase("null")
                        || normalized.equalsIgnoreCase("false")
                ? null
                : normalized;
    }

    private static final class PatternHolder {
        private static final String PATH_SEPARATOR =
                java.util.regex.Pattern.quote(System.getProperty("path.separator"));

        private PatternHolder() {
        }
    }
}
