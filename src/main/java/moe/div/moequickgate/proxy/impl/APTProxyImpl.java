package moe.div.moequickgate.proxy.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyComponent;
import moe.div.moequickgate.proxy.IProxy;
import moe.div.moequickgate.proxy.ProxyFailureType;
import moe.div.moequickgate.proxy.ProxyOperationException;
import moe.div.moequickgate.proxy.ProxyRuntimeStatus;
import moe.div.moequickgate.proxy.ProxyUriFactory;
import moe.div.moequickgate.utils.CommandExecutionException;
import moe.div.moequickgate.utils.CommandExecutor;
import moe.div.moequickgate.utils.CommandResult;
import moe.div.moequickgate.utils.CommandUtil;

/**
 * 通过 APT 配置片段控制 APT 代理。
 * Controls APT proxy settings through a dedicated APT configuration fragment.
 */
public final class APTProxyImpl implements IProxy {
    public static final Path DEFAULT_CONFIG_PATH =
            Path.of("/etc/apt/apt.conf.d/99zz-moequick-gate");

    private static final Path DEFAULT_APT_CONFIG = Path.of("/usr/bin/apt-config");
    private static final Path DEFAULT_PKEXEC = Path.of("/usr/bin/pkexec");
    private static final Path DEFAULT_INSTALL = Path.of("/usr/bin/install");
    private static final Duration CHECK_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration AUTHORIZATION_TIMEOUT = Duration.ofSeconds(120);
    private static final Pattern PROXY_LINE = Pattern.compile(
            "(?im)^Acquire::(http|https)::Proxy\\s+\"([^\"]*)\";?\\s*$");

    private final Path configPath;
    private final Path aptConfigExecutable;
    private final Path pkexecExecutable;
    private final Path installExecutable;
    private final CommandExecutor commandExecutor;

    public APTProxyImpl() {
        this(new CommandUtil());
    }

    public APTProxyImpl(CommandExecutor commandExecutor) {
        this(DEFAULT_CONFIG_PATH, DEFAULT_APT_CONFIG, DEFAULT_PKEXEC, DEFAULT_INSTALL,
                commandExecutor);
    }

    APTProxyImpl(
            Path configPath,
            Path aptConfigExecutable,
            Path pkexecExecutable,
            Path installExecutable,
            CommandExecutor commandExecutor) {
        this.configPath = configPath;
        this.aptConfigExecutable = aptConfigExecutable;
        this.pkexecExecutable = pkexecExecutable;
        this.installExecutable = installExecutable;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public ProxyComponent getComponent() {
        return ProxyComponent.APT;
    }

    @Override
    public ProxyRuntimeStatus check() {
        if (!Files.isExecutable(aptConfigExecutable)) {
            return ProxyRuntimeStatus.unavailable("未找到 apt-config：" + aptConfigExecutable);
        }
        CommandResult result = execute(
                List.of(aptConfigExecutable.toString(), "dump"),
                CHECK_TIMEOUT,
                "检测 APT 代理失败",
                "确认 apt-config 可执行后重试。");
        requireSuccess(result, "检测 APT 代理失败", "运行 apt-config dump 检查配置语法。");

        Map<String, String> routes = new LinkedHashMap<>();
        Matcher matcher = PROXY_LINE.matcher(result.stdout());
        while (matcher.find()) {
            String value = normalize(matcher.group(2));
            if (value != null) {
                routes.put(matcher.group(1).toLowerCase(Locale.ROOT), value);
            }
        }
        return ProxyRuntimeStatus.available(routes);
    }

    @Override
    public void enable(MoeProxy proxy) {
        String uri;
        try {
            uri = ProxyUriFactory.create(proxy);
        } catch (IllegalArgumentException exception) {
            throw failure(
                    ProxyFailureType.INVALID_CONFIGURATION,
                    "启用 APT 代理失败：" + exception.getMessage(),
                    "修正代理地址或选择 HTTP/HTTPS 协议后重试。",
                    "",
                    exception);
        }
        installConfiguration(configuration(uri));
    }

    @Override
    public void disable() {
        installConfiguration(configuration("DIRECT"));
    }

    private void installConfiguration(String content) {
        if (!Files.isExecutable(pkexecExecutable)) {
            throw failure(
                    ProxyFailureType.TOOL_MISSING,
                    "未找到 pkexec：" + pkexecExecutable,
                    "安装 policykit-1 并确认桌面授权代理正在运行。",
                    "",
                    null);
        }
        if (!Files.isExecutable(installExecutable)) {
            throw failure(
                    ProxyFailureType.TOOL_MISSING,
                    "未找到 install：" + installExecutable,
                    "安装 coreutils 后重试。",
                    "",
                    null);
        }

        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("moequick-gate-apt-", ".conf");
            Files.writeString(temporaryFile, content, StandardCharsets.UTF_8);
            setOwnerOnlyPermissions(temporaryFile);
            CommandResult result = execute(
                    List.of(
                            pkexecExecutable.toString(),
                            installExecutable.toString(),
                            "--mode=0644",
                            "--owner=root",
                            "--group=root",
                            temporaryFile.toString(),
                            configPath.toString()),
                    AUTHORIZATION_TIMEOUT,
                    "写入 APT 代理配置失败",
                    "重新授权并确认 /etc/apt/apt.conf.d 可写。");
            if (result.exitCode() == 126) {
                throw failure(
                        ProxyFailureType.AUTH_CANCELLED,
                        "APT 授权已取消。",
                        "重新操作并在授权窗口中确认。",
                        commandDetail(result),
                        null);
            }
            if (result.exitCode() == 127) {
                throw failure(
                        ProxyFailureType.PERMISSION_DENIED,
                        "APT 授权失败。",
                        "确认当前用户允许通过 PolicyKit 修改 APT 配置。",
                        commandDetail(result),
                        null);
            }
            requireSuccess(result, "写入 APT 代理配置失败",
                    "检查 PolicyKit 授权和 /etc/apt/apt.conf.d 目录权限。");
        } catch (IOException exception) {
            throw failure(
                    ProxyFailureType.IO_FAILURE,
                    "准备 APT 代理配置失败：" + exception.getMessage(),
                    "检查临时目录权限和磁盘空间。",
                    "",
                    exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // 临时文件清理由操作系统最终完成。 / The OS will eventually clean the temp file.
                }
            }
        }
    }

    private CommandResult execute(
            List<String> command, Duration timeout, String action, String suggestion) {
        try {
            return commandExecutor.execute(command, timeout);
        } catch (CommandExecutionException exception) {
            ProxyFailureType type = switch (exception.getFailureType()) {
                case TIMEOUT -> ProxyFailureType.TIMEOUT;
                case INTERRUPTED -> ProxyFailureType.INTERRUPTED;
                case START_FAILED -> ProxyFailureType.PROCESS_FAILED;
                case OUTPUT_READ_FAILED -> ProxyFailureType.IO_FAILURE;
            };
            throw failure(
                    type,
                    action + "：" + exception.getMessage(),
                    suggestion,
                    streamDetail(exception.getStdout(), exception.getStderr()),
                    exception);
        }
    }

    private void requireSuccess(
            CommandResult result, String action, String suggestion) {
        if (result.exitCode() != 0) {
            throw failure(
                    ProxyFailureType.PROCESS_FAILED,
                    action + "（退出码 " + result.exitCode() + "）",
                    suggestion,
                    commandDetail(result),
                    null);
        }
    }

    private ProxyOperationException failure(
            ProxyFailureType type,
            String message,
            String suggestion,
            String technicalDetail,
            Throwable cause) {
        return new ProxyOperationException(
                getComponent(), type, message, suggestion, technicalDetail, cause);
    }

    private static String commandDetail(CommandResult result) {
        return "退出码：" + result.exitCode() + "\n"
                + streamDetail(result.stdout(), result.stderr());
    }

    private static String streamDetail(String stdout, String stderr) {
        StringBuilder detail = new StringBuilder();
        if (stderr != null && !stderr.isBlank()) {
            detail.append("stderr:\n").append(limit(stderr));
        }
        if (stdout != null && !stdout.isBlank()) {
            if (!detail.isEmpty()) {
                detail.append('\n');
            }
            detail.append("stdout:\n").append(limit(stdout));
        }
        return detail.toString();
    }

    private static String limit(String value) {
        String normalized = value.strip();
        return normalized.length() <= 4096 ? normalized : normalized.substring(0, 4096) + "…";
    }

    private static String configuration(String value) {
        return "// 由 MoeQuick Gate 管理，请勿手动编辑。 / Managed by MoeQuick Gate.\n"
                + "Acquire::http::Proxy \"" + value + "\";\n"
                + "Acquire::https::Proxy \"" + value + "\";\n";
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isEmpty() || normalized.equalsIgnoreCase("DIRECT")
                ? null
                : normalized;
    }

    private static void setOwnerOnlyPermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Ubuntu 支持 POSIX 权限；其他文件系统保持 createTempFile 默认权限。
            // Ubuntu supports POSIX permissions; other file systems retain createTempFile defaults.
        }
    }
}
