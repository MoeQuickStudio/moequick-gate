package moe.div.moequickgate.controller;

import java.util.Objects;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import moe.div.moequickgate.bean.MoeProxy;

/**
 * 处理单个代理卡片的展示和界面事件。
 * Handles presentation and UI events for one proxy card.
 */
public final class ProxyCardController {
    private static final String SELECTED_STYLE_CLASS = "proxy-card-selected";

    @FXML
    private VBox cardRoot;

    @FXML
    private Label nameLabel;

    @FXML
    private Label protocolLabel;

    @FXML
    private Label endpointLabel;

    @FXML
    private Label selectionLabel;

    private MoeProxy proxy;
    private Consumer<MoeProxy> selectionHandler;
    private Consumer<MoeProxy> editHandler;
    private Consumer<MoeProxy> deleteHandler;

    public void configure(MoeProxy proxy) {
        this.proxy = Objects.requireNonNull(proxy);
        nameLabel.setText(proxy.getName());
        protocolLabel.setText(proxy.getProtocol().name());
        endpointLabel.setText(proxy.getEndpoint());
    }

    public void setSelected(boolean selected) {
        selectionLabel.setManaged(selected);
        selectionLabel.setVisible(selected);
        if (selected) {
            if (!cardRoot.getStyleClass().contains(SELECTED_STYLE_CLASS)) {
                cardRoot.getStyleClass().add(SELECTED_STYLE_CLASS);
            }
        } else {
            cardRoot.getStyleClass().remove(SELECTED_STYLE_CLASS);
        }
    }

    public void setSelectionHandler(Consumer<MoeProxy> selectionHandler) {
        this.selectionHandler = Objects.requireNonNull(selectionHandler);
    }

    public void setEditHandler(Consumer<MoeProxy> editHandler) {
        this.editHandler = Objects.requireNonNull(editHandler);
    }

    public void setDeleteHandler(Consumer<MoeProxy> deleteHandler) {
        this.deleteHandler = Objects.requireNonNull(deleteHandler);
    }

    @FXML
    private void handleCardClicked(MouseEvent event) {
        if (selectionHandler != null) {
            selectionHandler.accept(proxy);
        }
    }

    @FXML
    private void handleEdit() {
        if (editHandler != null) {
            editHandler.accept(proxy);
        }
    }

    @FXML
    private void handleDelete() {
        if (deleteHandler != null) {
            deleteHandler.accept(proxy);
        }
    }
}
