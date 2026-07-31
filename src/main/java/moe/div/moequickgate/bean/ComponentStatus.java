package moe.div.moequickgate.bean;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * 可供 JavaFX 绑定的组件代理状态。
 * JavaFX-bindable component proxy status.
 */
public final class ComponentStatus {
    private final ProxyComponent component;
    private final ReadOnlyObjectWrapper<ComponentState> state =
            new ReadOnlyObjectWrapper<>(this, "state", ComponentState.BUSY);
    private final ReadOnlyStringWrapper detail = new ReadOnlyStringWrapper(this, "detail", "正在检测…");

    public ComponentStatus(ProxyComponent component) {
        this.component = component;
    }

    public ProxyComponent getComponent() {
        return component;
    }

    public ComponentState getState() {
        return state.get();
    }

    public ReadOnlyObjectProperty<ComponentState> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public String getDetail() {
        return detail.get();
    }

    public ReadOnlyStringProperty detailProperty() {
        return detail.getReadOnlyProperty();
    }

    public void update(ComponentState newState, String newDetail) {
        state.set(newState);
        detail.set(newDetail == null ? "" : newDetail);
    }
}
