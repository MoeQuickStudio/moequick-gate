package moe.div.moequickgate.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import moe.div.moequickgate.bean.ComponentState;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyComponent;
import moe.div.moequickgate.bean.ProxyProtocol;
import moe.div.moequickgate.proxy.IProxy;
import moe.div.moequickgate.proxy.ProxyRuntimeStatus;
import org.junit.jupiter.api.Test;

class ComponentStatusViewModelTest {
    private final MoeProxy selected =
            new MoeProxy(1, "Local", "localhost", 7890, ProxyProtocol.HTTP);

    @Test
    void presentsDisabledCurrentOtherUnavailableAndErrorStates() {
        FakeProxy service = new FakeProxy();
        ComponentStatusViewModel viewModel =
                new ComponentStatusViewModel(service, Runnable::run);

        service.runtime = ProxyRuntimeStatus.available(Map.of());
        viewModel.refreshNow(selected);
        assertEquals(ComponentState.DISABLED, viewModel.getStatus().getState());

        service.runtime = ProxyRuntimeStatus.available(Map.of(
                "http", "http://localhost:7890/",
                "https", "http://localhost:7890/"));
        viewModel.refreshNow(selected);
        assertEquals(ComponentState.ENABLED_CURRENT, viewModel.getStatus().getState());

        service.runtime = ProxyRuntimeStatus.available(Map.of(
                "http", "http://other:8080/",
                "https", "http://other:8080/"));
        viewModel.refreshNow(selected);
        assertEquals(ComponentState.ENABLED_OTHER, viewModel.getStatus().getState());

        service.runtime = ProxyRuntimeStatus.unavailable("missing");
        viewModel.refreshNow(selected);
        assertEquals(ComponentState.UNAVAILABLE, viewModel.getStatus().getState());

        service.failure = new IllegalStateException("broken configuration");
        viewModel.refreshNow(selected);
        assertEquals(ComponentState.ERROR, viewModel.getStatus().getState());
        assertEquals("broken configuration", viewModel.getStatus().getDetail());
    }

    private static final class FakeProxy implements IProxy {
        private ProxyRuntimeStatus runtime = ProxyRuntimeStatus.available(Map.of());
        private RuntimeException failure;

        @Override
        public ProxyComponent getComponent() {
            return ProxyComponent.APT;
        }

        @Override
        public ProxyRuntimeStatus check() {
            if (failure != null) {
                throw failure;
            }
            return runtime;
        }

        @Override
        public void enable(MoeProxy proxy) {
        }

        @Override
        public void disable() {
        }
    }
}
