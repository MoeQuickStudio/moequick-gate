package moe.div.moequickgate.viewmodel;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import moe.div.moequickgate.bean.ComponentState;
import moe.div.moequickgate.bean.ComponentStatus;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyComponent;
import moe.div.moequickgate.proxy.IProxy;
import moe.div.moequickgate.proxy.ProxyRuntimeStatus;
import moe.div.moequickgate.proxy.ProxyUriFactory;

/**
 * 检测并维护单个组件的实时代理状态。
 * Detects and maintains the live proxy state of one component.
 */
public final class ComponentStatusViewModel {
    private final IProxy proxyService;
    private final ComponentStatus status;
    private final Consumer<Runnable> uiDispatcher;

    public ComponentStatusViewModel(IProxy proxyService, Consumer<Runnable> uiDispatcher) {
        this.proxyService = Objects.requireNonNull(proxyService);
        this.status = new ComponentStatus(proxyService.getComponent());
        this.uiDispatcher = Objects.requireNonNull(uiDispatcher);
    }

    public ProxyComponent getComponent() {
        return proxyService.getComponent();
    }

    public ComponentStatus getStatus() {
        return status;
    }

    public boolean isEnabled() {
        return status.getState() == ComponentState.ENABLED_CURRENT
                || status.getState() == ComponentState.ENABLED_OTHER;
    }

    public boolean isAvailable() {
        return status.getState() != ComponentState.UNAVAILABLE;
    }

    public void markBusy(String detail) {
        uiDispatcher.accept(() -> status.update(ComponentState.BUSY, detail));
    }

    public void refreshNow(MoeProxy selectedProxy) {
        try {
            ProxyRuntimeStatus runtimeStatus = proxyService.check();
            Presentation presentation = present(runtimeStatus, selectedProxy);
            uiDispatcher.accept(() -> status.update(presentation.state(), presentation.detail()));
        } catch (RuntimeException exception) {
            uiDispatcher.accept(() -> status.update(ComponentState.ERROR, rootMessage(exception)));
        }
    }

    public void enableNow(MoeProxy proxy) {
        proxyService.enable(proxy);
    }

    public void disableNow() {
        proxyService.disable();
    }

    private Presentation present(ProxyRuntimeStatus runtime, MoeProxy selectedProxy) {
        if (!runtime.available()) {
            return new Presentation(ComponentState.UNAVAILABLE, runtime.detail());
        }
        if (runtime.routes().isEmpty()) {
            return new Presentation(ComponentState.DISABLED,
                    runtime.detail().isBlank() ? "未配置代理" : runtime.detail());
        }

        String expected = expectedUri(selectedProxy);
        boolean matchesCurrent = expected != null
                && runtime.routes().containsKey("http")
                && runtime.routes().containsKey("https")
                && runtime.routes().values().stream().allMatch(value -> sameUri(value, expected));
        if (matchesCurrent) {
            return new Presentation(ComponentState.ENABLED_CURRENT,
                    "正在使用 " + expected);
        }

        String actual = runtime.routes().entrySet().stream()
                .map(entry -> entry.getKey().toUpperCase() + "=" + entry.getValue())
                .reduce((left, right) -> left + "；" + right)
                .orElse("未知代理");
        String detail = runtime.detail().isBlank()
                ? actual
                : runtime.detail() + " " + actual;
        return new Presentation(ComponentState.ENABLED_OTHER, detail);
    }

    private static String expectedUri(MoeProxy proxy) {
        if (proxy == null) {
            return null;
        }
        try {
            return ProxyUriFactory.create(proxy);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean sameUri(String actual, String expected) {
        return trimTrailingSlash(actual).equalsIgnoreCase(trimTrailingSlash(expected));
    }

    private static String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.strip();
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while ((current.getMessage() == null || current.getMessage().isBlank())
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private record Presentation(ComponentState state, String detail) {
    }
}
