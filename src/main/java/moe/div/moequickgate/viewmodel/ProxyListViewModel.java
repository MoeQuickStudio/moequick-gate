package moe.div.moequickgate.viewmodel;

import java.util.OptionalLong;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyProtocol;
import moe.div.moequickgate.bean.ProxyValidator;
import moe.div.moequickgate.repository.ProxyRepository;

/**
 * 管理代理列表、当前选择和 CRUD 页面状态。
 * Manages proxy list, selection, and CRUD view state.
 */
public final class ProxyListViewModel {
    private final ProxyRepository repository;
    private final ObservableList<MoeProxy> mutableProxies = FXCollections.observableArrayList();
    private final ObservableList<MoeProxy> proxies =
            FXCollections.unmodifiableObservableList(mutableProxies);
    private final ReadOnlyObjectWrapper<MoeProxy> selectedProxy = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper persistent = new ReadOnlyBooleanWrapper();
    private final ReadOnlyStringWrapper persistenceWarning = new ReadOnlyStringWrapper("");

    public ProxyListViewModel(
            ProxyRepository repository, boolean persistent, String persistenceWarning) {
        this.repository = repository;
        this.persistent.set(persistent);
        this.persistenceWarning.set(persistenceWarning == null ? "" : persistenceWarning);
        reload();
    }

    public ObservableList<MoeProxy> getProxies() {
        return proxies;
    }

    public MoeProxy getSelectedProxy() {
        return selectedProxy.get();
    }

    public ReadOnlyObjectProperty<MoeProxy> selectedProxyProperty() {
        return selectedProxy.getReadOnlyProperty();
    }

    public boolean isPersistent() {
        return persistent.get();
    }

    public ReadOnlyBooleanProperty persistentProperty() {
        return persistent.getReadOnlyProperty();
    }

    public String getPersistenceWarning() {
        return persistenceWarning.get();
    }

    public ReadOnlyStringProperty persistenceWarningProperty() {
        return persistenceWarning.getReadOnlyProperty();
    }

    public void addProxy(String name, String host, int port, ProxyProtocol protocol) {
        repository.create(ProxyValidator.normalize(0, name, host, port, protocol));
        reload();
    }

    public void updateProxy(long id, String name, String host, int port, ProxyProtocol protocol) {
        repository.update(ProxyValidator.normalize(id, name, host, port, protocol));
        reload();
    }

    public void deleteProxy(long id) {
        repository.deleteById(id);
        reload();
    }

    public void selectProxy(long id) {
        repository.select(id);
        reload();
    }

    public void reload() {
        mutableProxies.setAll(repository.findAll());
        OptionalLong selectedId = repository.findSelectedId();
        selectedProxy.set(selectedId.isPresent()
                ? mutableProxies.stream()
                        .filter(proxy -> proxy.getId() == selectedId.getAsLong())
                        .findFirst()
                        .orElse(null)
                : null);
    }
}
