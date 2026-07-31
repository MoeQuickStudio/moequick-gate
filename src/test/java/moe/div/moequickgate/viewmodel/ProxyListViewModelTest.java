package moe.div.moequickgate.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import moe.div.moequickgate.bean.ProxyProtocol;
import moe.div.moequickgate.bean.ProxyValidationException;
import moe.div.moequickgate.repository.InMemoryProxyRepository;
import org.junit.jupiter.api.Test;

class ProxyListViewModelTest {
    @Test
    void managesCrudSelectionAndDuplicateNames() {
        ProxyListViewModel viewModel =
                new ProxyListViewModel(new InMemoryProxyRepository(), true, "");

        viewModel.addProxy("  Same  ", "localhost", 7890, ProxyProtocol.HTTP);
        long firstId = viewModel.getSelectedProxy().getId();
        viewModel.addProxy("Same", "127.0.0.1", 7891, ProxyProtocol.HTTPS);

        assertEquals(2, viewModel.getProxies().size());
        assertEquals(firstId, viewModel.getSelectedProxy().getId());

        long secondId = viewModel.getProxies().get(1).getId();
        viewModel.selectProxy(secondId);
        assertEquals(secondId, viewModel.getSelectedProxy().getId());

        viewModel.updateProxy(secondId, "Edited", "::1", 1080, ProxyProtocol.SOCKS5);
        assertEquals("Edited", viewModel.getSelectedProxy().getName());
        assertEquals("[::1]:1080", viewModel.getSelectedProxy().getEndpoint());

        viewModel.deleteProxy(secondId);
        assertNull(viewModel.getSelectedProxy());
        assertEquals(1, viewModel.getProxies().size());
    }

    @Test
    void exposesFallbackStateAndRejectsInvalidInput() {
        ProxyListViewModel viewModel = new ProxyListViewModel(
                InMemoryProxyRepository.seeded(), false, "数据库不可用");

        assertFalse(viewModel.isPersistent());
        assertEquals("数据库不可用", viewModel.getPersistenceWarning());
        assertThrows(ProxyValidationException.class,
                () -> viewModel.addProxy("Proxy", "https://localhost", 7890, ProxyProtocol.HTTP));
    }
}
