package moe.div.moequickgate.viewmodel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyComponent;
import moe.div.moequickgate.bean.ProxyProtocol;
import moe.div.moequickgate.bean.ProxyValidator;
import moe.div.moequickgate.proxy.IProxy;

/**
 * 协调代理列表与组件实时状态。
 * Coordinates the proxy list and live component states.
 */
public final class MainViewModel implements AutoCloseable {
    private static final long REFRESH_INTERVAL_SECONDS = 5;

    private final ProxyListViewModel proxyListViewModel;
    private final Map<ProxyComponent, ComponentStatusViewModel> components =
            new EnumMap<>(ProxyComponent.class);
    private final ScheduledExecutorService worker;
    private final Consumer<Runnable> uiDispatcher;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ReadOnlyBooleanWrapper operationInProgress = new ReadOnlyBooleanWrapper();

    public MainViewModel(ProxyListViewModel proxyListViewModel, List<IProxy> proxyServices) {
        this(
                proxyListViewModel,
                proxyServices,
                Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "moequick-gate-components");
                    thread.setDaemon(true);
                    return thread;
                }),
                MainViewModel::dispatchToJavaFx);
    }

    MainViewModel(
            ProxyListViewModel proxyListViewModel,
            List<IProxy> proxyServices,
            ScheduledExecutorService worker,
            Consumer<Runnable> uiDispatcher) {
        this.proxyListViewModel = Objects.requireNonNull(proxyListViewModel);
        this.worker = Objects.requireNonNull(worker);
        this.uiDispatcher = Objects.requireNonNull(uiDispatcher);
        for (IProxy proxyService : proxyServices) {
            components.put(proxyService.getComponent(),
                    new ComponentStatusViewModel(proxyService, uiDispatcher));
        }
    }

    public ProxyListViewModel getProxyListViewModel() {
        return proxyListViewModel;
    }

    public ComponentStatusViewModel getComponent(ProxyComponent component) {
        ComponentStatusViewModel viewModel = components.get(component);
        if (viewModel == null) {
            throw new IllegalArgumentException("未配置组件：" + component);
        }
        return viewModel;
    }

    public boolean isOperationInProgress() {
        return operationInProgress.get();
    }

    public ReadOnlyBooleanProperty operationInProgressProperty() {
        return operationInProgress.getReadOnlyProperty();
    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            worker.scheduleWithFixedDelay(
                    () -> refreshAllSafely(false),
                    0,
                    REFRESH_INTERVAL_SECONDS,
                    TimeUnit.SECONDS);
        }
    }

    public void refresh() {
        if (!closed.get()) {
            worker.execute(() -> refreshAllSafely(false));
        }
    }

    public CompletableFuture<Void> addProxy(
            String name, String host, int port, ProxyProtocol protocol) {
        MoeProxy target = ProxyValidator.normalize(0, name, host, port, protocol);
        List<ComponentStatusViewModel> enabled = enabledComponents();
        if (!proxyListViewModel.getProxies().isEmpty() || enabled.isEmpty()) {
            return submit("新增代理", List.of(), () -> runOnUiAndWait(() -> {
                proxyListViewModel.addProxy(name, host, port, protocol);
                return null;
            }));
        }
        return coordinatedMutation(
                "新增并选择代理",
                enabled,
                target,
                null,
                () -> proxyListViewModel.addProxy(name, host, port, protocol));
    }

    public CompletableFuture<Void> selectProxy(long id) {
        MoeProxy target = requireProxy(id).copy();
        MoeProxy previous = copy(proxyListViewModel.getSelectedProxy());
        List<ComponentStatusViewModel> enabled = enabledComponents();
        requireSupportedWhenEnabled(target, enabled);
        return coordinatedMutation(
                "选择代理",
                enabled,
                target,
                previous,
                () -> proxyListViewModel.selectProxy(id));
    }

    public CompletableFuture<Void> updateProxy(
            long id, String name, String host, int port, ProxyProtocol protocol) {
        MoeProxy target = ProxyValidator.normalize(id, name, host, port, protocol);
        MoeProxy previous = requireProxy(id).copy();
        boolean current = proxyListViewModel.getSelectedProxy() != null
                && proxyListViewModel.getSelectedProxy().getId() == id;
        List<ComponentStatusViewModel> enabled = current ? enabledComponents() : List.of();
        requireSupportedWhenEnabled(target, enabled);
        return coordinatedMutation(
                "编辑代理",
                enabled,
                target,
                current ? previous : null,
                () -> proxyListViewModel.updateProxy(id, name, host, port, protocol));
    }

    public CompletableFuture<Void> deleteProxy(long id) {
        MoeProxy target = requireProxy(id).copy();
        boolean current = proxyListViewModel.getSelectedProxy() != null
                && proxyListViewModel.getSelectedProxy().getId() == id;
        List<ComponentStatusViewModel> enabled = current ? enabledComponents() : List.of();
        return submit("删除代理", enabled, () -> {
            List<ComponentStatusViewModel> changed = new ArrayList<>();
            try {
                for (ComponentStatusViewModel component : enabled) {
                    component.disableNow();
                    changed.add(component);
                }
                runOnUiAndWait(() -> {
                    proxyListViewModel.deleteProxy(id);
                    return null;
                });
            } catch (RuntimeException exception) {
                throw coordinatedFailure("删除代理", exception, rollback(changed, target));
            }
        });
    }

    public CompletableFuture<Void> setComponentEnabled(
            ProxyComponent component, boolean enabled) {
        ComponentStatusViewModel componentViewModel = getComponent(component);
        if (enabled) {
            MoeProxy selected = copy(proxyListViewModel.getSelectedProxy());
            if (selected == null) {
                return failed("开启 " + component.getDisplayName() + " 代理失败",
                        "尚未选择代理配置。", "先选择一个 HTTP 或 HTTPS 代理。");
            }
            if (selected.getProtocol() == ProxyProtocol.SOCKS5) {
                return failed("开启 " + component.getDisplayName() + " 代理失败",
                        "APT 和 NPM 暂不支持 SOCKS5。", "选择 HTTP 或 HTTPS 代理后重试。");
            }
            return submit("开启 " + component.getDisplayName() + " 代理",
                    List.of(componentViewModel), () -> componentViewModel.enableNow(selected));
        }
        return submit("关闭 " + component.getDisplayName() + " 代理",
                List.of(componentViewModel), componentViewModel::disableNow);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            worker.shutdownNow();
        }
    }

    private CompletableFuture<Void> coordinatedMutation(
            String action,
            List<ComponentStatusViewModel> affected,
            MoeProxy target,
            MoeProxy rollbackProxy,
            Runnable persistenceChange) {
        return submit(action, affected, () -> {
            List<ComponentStatusViewModel> changed = new ArrayList<>();
            try {
                for (ComponentStatusViewModel component : affected) {
                    component.enableNow(target);
                    changed.add(component);
                }
                runOnUiAndWait(() -> {
                    persistenceChange.run();
                    return null;
                });
            } catch (RuntimeException exception) {
                throw coordinatedFailure(action, exception, rollback(changed, rollbackProxy));
            }
        });
    }

    private List<String> rollback(
            List<ComponentStatusViewModel> changed, MoeProxy rollbackProxy) {
        List<String> failures = new ArrayList<>();
        for (int index = changed.size() - 1; index >= 0; index--) {
            ComponentStatusViewModel component = changed.get(index);
            try {
                if (rollbackProxy == null) {
                    component.disableNow();
                } else {
                    component.enableNow(rollbackProxy);
                }
            } catch (RuntimeException rollbackFailure) {
                failures.add(component.getComponent().getDisplayName() + "："
                        + rootMessage(rollbackFailure));
            }
        }
        return failures;
    }

    private OperationException coordinatedFailure(
            String action, RuntimeException failure, List<String> rollbackFailures) {
        String message = action + "失败：" + rootMessage(failure);
        String suggestion = suggestion(failure);
        if (!rollbackFailures.isEmpty()) {
            message += "\n回滚失败：" + String.join("；", rollbackFailures);
            suggestion += " 刷新实际状态并逐项修复回滚失败的组件。";
        }
        return new OperationException(message, suggestion, failure);
    }

    private CompletableFuture<Void> submit(
            String action, List<ComponentStatusViewModel> affected, Runnable operation) {
        if (closed.get()) {
            return failed(action + "失败", "应用正在关闭。", "重新启动应用后重试。");
        }
        if (operationInProgress.get()) {
            return failed(action + "失败", "另一项操作正在进行。", "等待当前操作完成后重试。");
        }
        operationInProgress.set(true);
        affected.forEach(component -> component.markBusy(action + "中…"));
        CompletableFuture<Void> future = new CompletableFuture<>();
        worker.execute(() -> {
            RuntimeException failure = null;
            try {
                operation.run();
            } catch (RuntimeException exception) {
                failure = exception;
            } finally {
                refreshAllSafely(true);
                runOnUiAndWait(() -> {
                    operationInProgress.set(false);
                    return null;
                });
            }
            if (failure == null) {
                future.complete(null);
            } else {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    private CompletableFuture<Void> failed(
            String action, String reason, String suggestion) {
        return CompletableFuture.failedFuture(
                new OperationException(action + "：" + reason, suggestion, null));
    }

    private void refreshAllSafely(boolean force) {
        if (closed.get() || (!force && operationInProgress.get())) {
            return;
        }
        MoeProxy selected;
        try {
            selected = runOnUiAndWait(() -> copy(proxyListViewModel.getSelectedProxy()));
        } catch (RuntimeException exception) {
            return;
        }
        components.values().forEach(component -> component.refreshNow(selected));
    }

    private List<ComponentStatusViewModel> enabledComponents() {
        return components.values().stream()
                .filter(ComponentStatusViewModel::isEnabled)
                .toList();
    }

    private void requireSupportedWhenEnabled(
            MoeProxy proxy, List<ComponentStatusViewModel> enabled) {
        if (!enabled.isEmpty() && proxy.getProtocol() == ProxyProtocol.SOCKS5) {
            throw new OperationException(
                    "无法切换到 SOCKS5：APT 或 NPM 代理仍处于开启状态。",
                    "先关闭所有已开启组件，再选择或保存 SOCKS5 代理。",
                    null);
        }
    }

    private MoeProxy requireProxy(long id) {
        return proxyListViewModel.getProxies().stream()
                .filter(proxy -> proxy.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("代理配置不存在：" + id));
    }

    private <T> T runOnUiAndWait(Supplier<T> operation) {
        CompletableFuture<T> result = new CompletableFuture<>();
        uiDispatcher.accept(() -> {
            try {
                result.complete(operation.get());
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
            }
        });
        try {
            return result.join();
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }

    private static void dispatchToJavaFx(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private static MoeProxy copy(MoeProxy proxy) {
        return proxy == null ? null : proxy.copy();
    }

    private static String suggestion(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof moe.div.moequickgate.proxy.ProxyOperationException proxyFailure) {
                return proxyFailure.getSuggestion();
            }
            current = current.getCause();
        }
        return "检查数据库、组件配置和文件权限后重试。";
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while ((current.getMessage() == null || current.getMessage().isBlank())
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
