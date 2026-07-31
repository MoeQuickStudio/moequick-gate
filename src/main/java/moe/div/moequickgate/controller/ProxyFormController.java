package moe.div.moequickgate.controller;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyProtocol;
import moe.div.moequickgate.bean.ProxyValidationException;
import moe.div.moequickgate.bean.ProxyValidator;

/**
 * 管理新增和编辑代理所共用的表单状态。
 * Manages the shared add/edit proxy form state.
 */
public final class ProxyFormController {
    @FXML
    private TextField nameField;

    @FXML
    private TextField hostField;

    @FXML
    private TextField portField;

    @FXML
    private ComboBox<ProxyProtocol> protocolBox;

    @FXML
    private Label validationLabel;

    private final ReadOnlyBooleanWrapper valid = new ReadOnlyBooleanWrapper(false);
    private MoeProxy validatedProxy;

    @FXML
    private void initialize() {
        protocolBox.getItems().setAll(ProxyProtocol.values());
        protocolBox.getSelectionModel().select(ProxyProtocol.HTTP);
        nameField.textProperty().addListener((observable, previous, current) -> validate());
        hostField.textProperty().addListener((observable, previous, current) -> validate());
        portField.textProperty().addListener((observable, previous, current) -> validate());
        protocolBox.valueProperty().addListener((observable, previous, current) -> validate());
        validate();
    }

    public void configure(MoeProxy proxy) {
        if (proxy == null) {
            nameField.clear();
            hostField.clear();
            portField.clear();
            protocolBox.getSelectionModel().select(ProxyProtocol.HTTP);
        } else {
            nameField.setText(proxy.getName());
            hostField.setText(proxy.getHost());
            portField.setText(Integer.toString(proxy.getPort()));
            protocolBox.getSelectionModel().select(proxy.getProtocol());
        }
        validate();
    }

    public ReadOnlyBooleanProperty validProperty() {
        return valid.getReadOnlyProperty();
    }

    public MoeProxy getValidatedProxy(long id) {
        validate(id);
        if (!valid.get() || validatedProxy == null) {
            throw new ProxyValidationException(validationLabel.getText());
        }
        return validatedProxy;
    }

    private void validate() {
        validate(0);
    }

    private void validate(long id) {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            validatedProxy = ProxyValidator.normalize(
                    id,
                    nameField.getText(),
                    hostField.getText(),
                    port,
                    protocolBox.getValue());
            valid.set(true);
            validationLabel.setText("");
            validationLabel.setManaged(false);
            validationLabel.setVisible(false);
        } catch (NumberFormatException exception) {
            invalidate("端口必须为 1–65535 的整数。");
        } catch (ProxyValidationException exception) {
            invalidate(exception.getMessage());
        }
    }

    private void invalidate(String message) {
        validatedProxy = null;
        valid.set(false);
        validationLabel.setText(message);
        validationLabel.setManaged(true);
        validationLabel.setVisible(true);
    }
}
