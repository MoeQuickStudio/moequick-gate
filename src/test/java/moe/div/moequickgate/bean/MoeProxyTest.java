package moe.div.moequickgate.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MoeProxyTest {
    @Test
    void exposesJavaFxPropertiesAndFormatsIpv6Endpoint() {
        MoeProxy proxy = new MoeProxy(7, "Local", "::1", 7890, ProxyProtocol.SOCKS5);

        assertEquals(7, proxy.idProperty().get());
        assertEquals("Local", proxy.nameProperty().get());
        assertEquals("::1", proxy.hostProperty().get());
        assertEquals(7890, proxy.portProperty().get());
        assertEquals(ProxyProtocol.SOCKS5, proxy.protocolProperty().get());
        assertEquals("[::1]:7890", proxy.getEndpoint());
    }

    @Test
    void normalizesValidInputAndRejectsInvalidFields() {
        MoeProxy normalized = ProxyValidator.normalize(
                0, "  Clash  ", "  localhost  ", 7890, ProxyProtocol.HTTP);

        assertEquals("Clash", normalized.getName());
        assertEquals("localhost", normalized.getHost());
        assertThrows(ProxyValidationException.class,
                () -> ProxyValidator.normalize(0, "", "localhost", 7890, ProxyProtocol.HTTP));
        assertThrows(ProxyValidationException.class,
                () -> ProxyValidator.normalize(0, "Proxy", "http://localhost", 7890, ProxyProtocol.HTTP));
        assertThrows(ProxyValidationException.class,
                () -> ProxyValidator.normalize(0, "Proxy", "localhost", 65_536, ProxyProtocol.HTTP));
        assertThrows(ProxyValidationException.class,
                () -> ProxyValidator.normalize(0, "Proxy", "localhost", 7890, null));
    }
}
