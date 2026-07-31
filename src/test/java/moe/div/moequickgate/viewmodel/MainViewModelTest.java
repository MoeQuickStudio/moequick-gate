package moe.div.moequickgate.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyComponent;
import moe.div.moequickgate.bean.ProxyProtocol;
import moe.div.moequickgate.proxy.IProxy;
import moe.div.moequickgate.proxy.ProxyRuntimeStatus;
import moe.div.moequickgate.proxy.ProxyUriFactory;
import moe.div.moequickgate.repository.InMemoryProxyRepository;
import moe.div.moequickgate.repository.ProxyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MainViewModelTest {
    private ScheduledExecutorService worker;
    private ProxyListViewModel proxyList;
    private FakeProxy apt;
    private FakeProxy npm;
    private MainViewModel viewModel;

    @BeforeEach
    void setUp() {
        InMemoryProxyRepository repository = new InMemoryProxyRepository();
        proxyList = new ProxyListViewModel(repository, true, "");
        proxyList.addProxy("Old", "old-host", 7890, ProxyProtocol.HTTP);
        proxyList.addProxy("New", "new-host", 7891, ProxyProtocol.HTTPS);
        apt = new FakeProxy(ProxyComponent.APT);
        npm = new FakeProxy(ProxyComponent.NPM);
        worker = Executors.newSingleThreadScheduledExecutor();
        viewModel = new MainViewModel(proxyList, List.of(apt, npm), worker, Runnable::run);
    }

    @AfterEach
    void tearDown() {
        viewModel.close();
    }

    @Test
    void switchesEnabledComponentsBeforePersistingSelection() {
        MoeProxy old = proxyList.getSelectedProxy().copy();
        apt.enable(old);
        npm.enable(old);
        refreshStatuses();
        long newId = proxyList.getProxies().get(1).getId();

        viewModel.selectProxy(newId).join();

        assertEquals(newId, proxyList.getSelectedProxy().getId());
        String expected = ProxyUriFactory.create(proxyList.getSelectedProxy());
        assertEquals(expected, apt.currentUri);
        assertEquals(expected, npm.currentUri);
    }

    @Test
    void rollsBackChangedComponentWhenLaterComponentFails() {
        MoeProxy old = proxyList.getSelectedProxy().copy();
        apt.enable(old);
        npm.enable(old);
        refreshStatuses();
        npm.failNextEnable = true;
        long newId = proxyList.getProxies().get(1).getId();

        assertThrows(CompletionException.class, () -> viewModel.selectProxy(newId).join());

        assertEquals(old.getId(), proxyList.getSelectedProxy().getId());
        assertEquals(ProxyUriFactory.create(old), apt.currentUri);
        assertEquals(ProxyUriFactory.create(old), npm.currentUri);
    }

    @Test
    void reportsRollbackFailureTogetherWithOriginalFailure() {
        MoeProxy old = proxyList.getSelectedProxy().copy();
        apt.enable(old);
        npm.enable(old);
        refreshStatuses();
        apt.failOnUri = ProxyUriFactory.create(old);
        npm.failNextEnable = true;
        long newId = proxyList.getProxies().get(1).getId();

        CompletionException thrown = assertThrows(
                CompletionException.class, () -> viewModel.selectProxy(newId).join());

        assertTrue(thrown.getCause().getMessage().contains("回滚失败"));
        assertEquals(old.getId(), proxyList.getSelectedProxy().getId());
    }

    @Test
    void rollsBackComponentsWhenPersistenceFails() {
        viewModel.close();
        InMemoryProxyRepository backing = new InMemoryProxyRepository();
        backing.create(new MoeProxy(0, "Old", "old-host", 7890, ProxyProtocol.HTTP));
        MoeProxy second = backing.create(
                new MoeProxy(0, "New", "new-host", 7891, ProxyProtocol.HTTPS));
        ProxyRepository failingRepository = new DelegatingRepository(backing) {
            @Override
            public void select(long id) {
                throw new IllegalStateException("simulated database failure");
            }
        };
        proxyList = new ProxyListViewModel(failingRepository, true, "");
        apt = new FakeProxy(ProxyComponent.APT);
        npm = new FakeProxy(ProxyComponent.NPM);
        apt.enable(proxyList.getSelectedProxy());
        npm.enable(proxyList.getSelectedProxy());
        worker = Executors.newSingleThreadScheduledExecutor();
        viewModel = new MainViewModel(proxyList, List.of(apt, npm), worker, Runnable::run);
        refreshStatuses();
        MoeProxy old = proxyList.getSelectedProxy().copy();

        assertThrows(CompletionException.class, () -> viewModel.selectProxy(second.getId()).join());

        assertEquals(old.getId(), proxyList.getSelectedProxy().getId());
        assertEquals(ProxyUriFactory.create(old), apt.currentUri);
        assertEquals(ProxyUriFactory.create(old), npm.currentUri);
    }

    @Test
    void rejectsSocksSelectionWhileAComponentIsEnabled() {
        proxyList.addProxy("Socks", "localhost", 1080, ProxyProtocol.SOCKS5);
        long socksId = proxyList.getProxies().get(2).getId();
        apt.enable(proxyList.getSelectedProxy());
        refreshStatuses();

        OperationException failure = assertThrows(
                OperationException.class, () -> viewModel.selectProxy(socksId));

        assertTrue(failure.getMessage().contains("SOCKS5"));
        assertFalse(viewModel.isOperationInProgress());
    }

    @Test
    void disablesEnabledComponentsBeforeDeletingCurrentProxy() {
        long selectedId = proxyList.getSelectedProxy().getId();
        apt.enable(proxyList.getSelectedProxy());
        npm.enable(proxyList.getSelectedProxy());
        refreshStatuses();

        viewModel.deleteProxy(selectedId).join();

        assertNull(proxyList.getSelectedProxy());
        assertNull(apt.currentUri);
        assertNull(npm.currentUri);
        assertEquals(1, proxyList.getProxies().size());
    }

    @Test
    void refusesToEnableWithoutCurrentProxyAndForSocks() {
        proxyList.deleteProxy(proxyList.getSelectedProxy().getId());
        assertThrows(CompletionException.class, () -> viewModel
                .setComponentEnabled(ProxyComponent.APT, true).join());

        long remainingId = proxyList.getProxies().get(0).getId();
        proxyList.updateProxy(remainingId, "Socks", "localhost", 1080, ProxyProtocol.SOCKS5);
        proxyList.selectProxy(remainingId);
        assertThrows(CompletionException.class, () -> viewModel
                .setComponentEnabled(ProxyComponent.APT, true).join());
    }

    private void refreshStatuses() {
        MoeProxy selected = proxyList.getSelectedProxy();
        viewModel.getComponent(ProxyComponent.APT).refreshNow(selected);
        viewModel.getComponent(ProxyComponent.NPM).refreshNow(selected);
    }

    private static final class FakeProxy implements IProxy {
        private final ProxyComponent component;
        private String currentUri;
        private boolean failNextEnable;
        private String failOnUri;

        private FakeProxy(ProxyComponent component) {
            this.component = component;
        }

        @Override
        public ProxyComponent getComponent() {
            return component;
        }

        @Override
        public ProxyRuntimeStatus check() {
            return currentUri == null
                    ? ProxyRuntimeStatus.available(Map.of())
                    : ProxyRuntimeStatus.available(Map.of(
                            "http", currentUri, "https", currentUri));
        }

        @Override
        public void enable(MoeProxy proxy) {
            String targetUri = ProxyUriFactory.create(proxy);
            if (failNextEnable) {
                failNextEnable = false;
                throw new IllegalStateException("simulated enable failure");
            }
            if (targetUri.equals(failOnUri)) {
                throw new IllegalStateException("simulated rollback failure");
            }
            currentUri = targetUri;
        }

        @Override
        public void disable() {
            currentUri = null;
        }
    }

    private static class DelegatingRepository implements ProxyRepository {
        private final ProxyRepository delegate;

        private DelegatingRepository(ProxyRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<MoeProxy> findAll() {
            return delegate.findAll();
        }

        @Override
        public OptionalLong findSelectedId() {
            return delegate.findSelectedId();
        }

        @Override
        public MoeProxy create(MoeProxy proxy) {
            return delegate.create(proxy);
        }

        @Override
        public void update(MoeProxy proxy) {
            delegate.update(proxy);
        }

        @Override
        public void deleteById(long id) {
            delegate.deleteById(id);
        }

        @Override
        public void select(long id) {
            delegate.select(id);
        }
    }
}
