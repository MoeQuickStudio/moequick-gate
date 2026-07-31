package moe.div.moequickgate.controller;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.viewmodel.ProxyListViewModel;

/**
 * 处理主界面的展示层事件，并将数据操作委托给 ViewModel。
 * Handles main-view events and delegates data operations to the ViewModel.
 */
public final class MainController {
    public static final String PROXY_CARD_RESOURCE = "/fxml/proxy_card.fxml";
    public static final String PROXY_FORM_RESOURCE = "/fxml/proxy_form.fxml";

    private final ProxyListViewModel viewModel;
    private final Map<Long, ProxyCardController> proxyCards = new LinkedHashMap<>();

    @FXML
    private Label currentProxyLabel;

    @FXML
    private FlowPane proxyList;

    @FXML
    private HBox persistenceWarningBanner;

    @FXML
    private Label persistenceWarningLabel;

    @FXML
    private Label aptStatusLabel;

    @FXML
    private ToggleButton aptToggle;

    @FXML
    private Label npmStatusLabel;

    @FXML
    private ToggleButton npmToggle;

    public MainController(ProxyListViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    private void initialize() {
        viewModel.getProxies().addListener(
                (ListChangeListener<MoeProxy>) change -> refreshProxyCards());
        viewModel.selectedProxyProperty().addListener(
                (observable, previous, current) -> refreshSelection());
        showPersistenceState();
        refreshProxyCards();
        updateComponentStatus(aptToggle, aptStatusLabel);
        updateComponentStatus(npmToggle, npmStatusLabel);
    }

    @FXML
    private void handleAddProxy() {
        showProxyEditor(null);
    }

    @FXML
    private void handleAptToggle() {
        updateComponentStatus(aptToggle, aptStatusLabel);
    }

    @FXML
    private void handleNpmToggle() {
        updateComponentStatus(npmToggle, npmStatusLabel);
    }

    private void refreshProxyCards() {
        proxyList.getChildren().clear();
        proxyCards.clear();

        if (viewModel.getProxies().isEmpty()) {
            Label emptyState = new Label("暂无代理配置，点击“新增代理”开始。");
            emptyState.getStyleClass().add("empty-state");
            proxyList.getChildren().add(emptyState);
        } else {
            viewModel.getProxies().forEach(this::addProxyCard);
        }
        refreshSelection();
    }

    private void addProxyCard(MoeProxy proxy) {
        URL resource = requireResource(PROXY_CARD_RESOURCE, "代理卡片 / proxy card");
        FXMLLoader loader = new FXMLLoader(resource);
        try {
            Parent card = loader.load();
            ProxyCardController controller = loader.getController();
            controller.configure(proxy);
            controller.setSelectionHandler(selected -> runDataOperation(
                    "选择代理", () -> viewModel.selectProxy(selected.getId())));
            controller.setEditHandler(this::showProxyEditor);
            controller.setDeleteHandler(this::confirmDelete);
            proxyCards.put(proxy.getId(), controller);
            proxyList.getChildren().add(card);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "无法加载代理卡片 / Unable to load proxy card: " + PROXY_CARD_RESOURCE,
                    exception);
        }
    }

    private void refreshSelection() {
        MoeProxy selected = viewModel.getSelectedProxy();
        proxyCards.forEach((id, card) -> card.setSelected(selected != null && id == selected.getId()));
        currentProxyLabel.setText(selected == null
                ? "当前代理：未选择"
                : "当前代理：" + selected.getName() + " · " + selected.getProtocol()
                        + " · " + selected.getEndpoint());
    }

    private void showProxyEditor(MoeProxy existingProxy) {
        URL resource = requireResource(PROXY_FORM_RESOURCE, "代理表单 / proxy form");
        FXMLLoader loader = new FXMLLoader(resource);
        VBox content;
        ProxyFormController formController;
        try {
            content = loader.load();
            formController = loader.getController();
        } catch (IOException exception) {
            showError("打开代理表单失败", exception);
            return;
        }

        formController.configure(existingProxy);
        DialogResult dialogResult = createEditorDialog(existingProxy, content, formController);
        if (!dialogResult.saved()) {
            return;
        }

        MoeProxy input = formController.getValidatedProxy(existingProxy == null ? 0 : existingProxy.getId());
        if (existingProxy == null) {
            runDataOperation("新增代理", () -> viewModel.addProxy(
                    input.getName(), input.getHost(), input.getPort(), input.getProtocol()));
        } else {
            runDataOperation("编辑代理", () -> viewModel.updateProxy(
                    input.getId(),
                    input.getName(),
                    input.getHost(),
                    input.getPort(),
                    input.getProtocol()));
        }
    }

    private DialogResult createEditorDialog(
            MoeProxy existingProxy, VBox content, ProxyFormController formController) {
        javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(existingProxy == null ? "新增代理" : "编辑代理");
        dialog.setHeaderText(existingProxy == null ? "创建代理配置" : "修改代理配置");
        if (proxyList.getScene() != null) {
            dialog.initOwner(proxyList.getScene().getWindow());
        }
        dialog.getDialogPane().setContent(content);
        ButtonType saveType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);
        if (proxyList.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(proxyList.getScene().getStylesheets());
        }
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.disableProperty().bind(formController.validProperty().not());

        Optional<ButtonType> result = dialog.showAndWait();
        return new DialogResult(result.isPresent() && result.get() == saveType);
    }

    private void confirmDelete(MoeProxy proxy) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("删除代理");
        confirmation.setHeaderText("确定删除“" + proxy.getName() + "”吗？");
        confirmation.setContentText("此操作无法撤销。若删除当前代理，当前选择将被清空。");
        if (proxyList.getScene() != null) {
            confirmation.initOwner(proxyList.getScene().getWindow());
        }
        if (confirmation.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            runDataOperation("删除代理", () -> viewModel.deleteProxy(proxy.getId()));
        }
    }

    private void runDataOperation(String action, Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException exception) {
            showError(action + "失败", exception);
            try {
                viewModel.reload();
            } catch (RuntimeException reloadFailure) {
                showError("重新加载最后保存状态失败", reloadFailure);
            }
        }
    }

    private void showPersistenceState() {
        boolean showWarning = !viewModel.isPersistent();
        persistenceWarningBanner.setManaged(showWarning);
        persistenceWarningBanner.setVisible(showWarning);
        persistenceWarningLabel.setText(viewModel.getPersistenceWarning());
    }

    private void updateComponentStatus(ToggleButton toggle, Label statusLabel) {
        boolean enabled = toggle.isSelected();
        toggle.setText(enabled ? "开启" : "关闭");
        statusLabel.setText(enabled ? "已开启" : "已关闭");
        setStyleClass(toggle, "component-toggle-active", enabled);
        setStyleClass(statusLabel, "component-status-enabled", enabled);
    }

    private void showError(String action, Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("MoeQuick Gate");
        alert.setHeaderText(action);
        alert.setContentText("原因：" + rootMessage(throwable)
                + "\n建议：检查数据库目录权限和磁盘状态后重试。");
        if (proxyList.getScene() != null) {
            alert.initOwner(proxyList.getScene().getWindow());
        }
        alert.showAndWait();
    }

    private URL requireResource(String path, String description) {
        URL resource = MainController.class.getResource(path);
        if (resource == null) {
            throw new IllegalStateException(
                    "无法加载" + description + " / Unable to load " + description
                            + ": resource not found: " + path);
        }
        return resource;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
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

    private record DialogResult(boolean saved) {
    }
}
