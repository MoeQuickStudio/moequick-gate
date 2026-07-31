package moe.div.moequickgate.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;

/**
 * 处理主界面的展示层事件。
 * Handles presentation-only events for the main view.
 */
public final class MainController {
    public static final String PROXY_CARD_RESOURCE = "/fxml/proxy_card.fxml";
    private static final String DEMO_PROXY_NAME = "Clash 本机监听";
    private static final String DEMO_PROXY_PROTOCOL = "HTTP";
    private static final String DEMO_PROXY_ENDPOINT = "127.0.0.1:7890";

    @FXML
    private Label currentProxyLabel;

    @FXML
    private FlowPane proxyList;

    @FXML
    private Label aptStatusLabel;

    @FXML
    private ToggleButton aptToggle;

    @FXML
    private Label npmStatusLabel;

    @FXML
    private ToggleButton npmToggle;

    private final List<ProxyCardController> proxyCards = new ArrayList<>();

    @FXML
    private void initialize() {
        ProxyCardController demoCard = addDemoProxyCard();
        selectProxyCard(demoCard);
        updateComponentStatus(aptToggle, aptStatusLabel);
        updateComponentStatus(npmToggle, npmStatusLabel);
    }

    @FXML
    private void handleAddProxy() {
        showPhase3Notice("新增代理");
    }

    @FXML
    private void handleAptToggle() {
        updateComponentStatus(aptToggle, aptStatusLabel);
    }

    @FXML
    private void handleNpmToggle() {
        updateComponentStatus(npmToggle, npmStatusLabel);
    }

    private ProxyCardController addDemoProxyCard() {
        URL resource = MainController.class.getResource(PROXY_CARD_RESOURCE);
        if (resource == null) {
            throw new IllegalStateException(
                    "无法加载代理卡片 / Unable to load proxy card: resource not found: "
                            + PROXY_CARD_RESOURCE);
        }

        FXMLLoader loader = new FXMLLoader(resource);
        try {
            Parent card = loader.load();
            ProxyCardController controller = loader.getController();
            controller.configure(DEMO_PROXY_NAME, DEMO_PROXY_PROTOCOL, DEMO_PROXY_ENDPOINT);
            controller.setSelectionHandler(this::selectProxyCard);
            controller.setEditHandler(() -> showPhase3Notice("编辑代理"));
            controller.setDeleteHandler(() -> showPhase3Notice("删除代理"));
            proxyCards.add(controller);
            proxyList.getChildren().add(card);
            return controller;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "无法加载代理卡片 / Unable to load proxy card: " + PROXY_CARD_RESOURCE,
                    exception);
        }
    }

    private void selectProxyCard(ProxyCardController selectedCard) {
        proxyCards.forEach(card -> card.setSelected(card == selectedCard));
        currentProxyLabel.setText("当前代理：" + selectedCard.getSummary());
    }

    private void updateComponentStatus(ToggleButton toggle, Label statusLabel) {
        boolean enabled = toggle.isSelected();
        toggle.setText(enabled ? "开启" : "关闭");
        statusLabel.setText(enabled ? "已开启" : "已关闭");
        setStyleClass(toggle, "component-toggle-active", enabled);
        setStyleClass(statusLabel, "component-status-enabled", enabled);
    }

    private void showPhase3Notice(String action) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MoeQuick Gate");
        alert.setHeaderText(action);
        alert.setContentText("该功能将在 Phase 3：代理配置管理中提供。");
        if (proxyList.getScene() != null) {
            alert.initOwner(proxyList.getScene().getWindow());
        }
        alert.showAndWait();
    }

    private static void setStyleClass(javafx.scene.Node node, String styleClass, boolean enabled) {
        if (enabled) {
            if (!node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
            }
        } else {
            node.getStyleClass().remove(styleClass);
        }
    }
}
