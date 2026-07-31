package moe.div.moequickgate.bean;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * 用户创建的代理配置模型。
 * User-created proxy profile model.
 */
public final class MoeProxy {
    private final LongProperty id = new SimpleLongProperty(this, "id");
    private final StringProperty name = new SimpleStringProperty(this, "name");
    private final StringProperty host = new SimpleStringProperty(this, "host");
    private final IntegerProperty port = new SimpleIntegerProperty(this, "port");
    private final ObjectProperty<ProxyProtocol> protocol =
            new SimpleObjectProperty<>(this, "protocol");

    public MoeProxy(long id, String name, String host, int port, ProxyProtocol protocol) {
        setId(id);
        setName(name);
        setHost(host);
        setPort(port);
        setProtocol(protocol);
    }

    public long getId() {
        return id.get();
    }

    public void setId(long value) {
        id.set(value);
    }

    public LongProperty idProperty() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getHost() {
        return host.get();
    }

    public void setHost(String value) {
        host.set(value);
    }

    public StringProperty hostProperty() {
        return host;
    }

    public int getPort() {
        return port.get();
    }

    public void setPort(int value) {
        port.set(value);
    }

    public IntegerProperty portProperty() {
        return port;
    }

    public ProxyProtocol getProtocol() {
        return protocol.get();
    }

    public void setProtocol(ProxyProtocol value) {
        protocol.set(value);
    }

    public ObjectProperty<ProxyProtocol> protocolProperty() {
        return protocol;
    }

    public String getEndpoint() {
        String displayHost = getHost().contains(":") ? "[" + getHost() + "]" : getHost();
        return displayHost + ":" + getPort();
    }

    public MoeProxy copy() {
        return new MoeProxy(getId(), getName(), getHost(), getPort(), getProtocol());
    }
}
