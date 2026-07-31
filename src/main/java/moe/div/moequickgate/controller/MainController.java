package moe.div.moequickgate.controller;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import moe.div.moequickgate.bean.ComponentState;
import moe.div.moequickgate.bean.ComponentStatus;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyComponent;
import moe.div.moequickgate.bean.ProxyProtocol;
import moe.div.moequickgate.proxy.ProxyOperationException;
import moe.div.moequickgate.viewmodel.ComponentStatusViewModel;
import moe.div.moequickgate.viewmodel.MainViewModel;
import moe.div.moequickgate.viewmodel.OperationException;
import moe.div.moequickgate.viewmodel.ProxyListViewModel;

/**
 * 处理主界面的展示层事件，并将操作委托给 ViewModel。
 * Handles main-view events and delegates operations to ViewModels.
 */
public final class MainController {
    public static final String PROXY_CARD_RESOURCE = "/fxml/proxy_card.fxml";
    public static final String PROXY_FORM_RESOURCE = "/fxml/proxy_form.fxml";

    private final MainViewModel mainViewModel;
    private final ProxyListViewModel proxyViewModel;
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
    private Label aptDetailLabel;

    @FXML
    private ToggleButton aptToggle;

    @FXML
    private Label npmStatusLabel;

    @FXML
    private Label npmDetailLabel;

    @FXML
    private ToggleButton npmToggle;

    public MainController(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        this.proxyViewModel = mainViewModel.getProxyListViewModel();
    }

    @FXML
    private void initialize() {
        proxyViewModel.getProxies().addListener(
                (ListChangeListener<MoeProxy>) change -> refreshProxyCards());
        proxyViewModel.selectedProxyProperty().addListener(
                (observable, previous, current) -> {
                    refreshSelection();
                    refreshComponentControls();
                });
        mainViewModel.operationInProgressProperty().addListener(
                (observable, previous, current) -> refreshComponentControls());

        bindComponent(ProxyComponent.APT, aptToggle, aptStatusLabel, aptDetailLabel);
        bindComponent(ProxyComponent.NPM, npmToggle, npmStatusLabel, npmDetailLabel);
        bindWindowLifecycle();
        showPersistenceState();
        refreshProxyCards();
        refreshComponentControls();
        mainViewModel.start();
    }

    @FXML
    private void handleAddProxy() {
        showProxyEditor(null);
    }

    @FXML
    private void handleAptToggle() {
        runOperation("更新 APT 代理", () -> mainViewModel.setComponentEnabled(
                ProxyComponent.APT, aptToggle.isSelected()));
    }

    @FXML
    private void handleNpmToggle() {
        runOperation("更新 NPM 代理", () -> mainViewModel.setComponentEnabled(
                ProxyComponent.NPM, npmToggle.isSelected()));
    }

    private void bindComponent(
            ProxyComponent component,
            ToggleButton toggle,
            Label statusLabel,
            Label detailLabel) {
        ComponentStatus status = mainViewModel.getComponent(component).getStatus();
        status.stateProperty().addListener(
                (observable, previous, current) -> renderComponent(
                        status, toggle, statusLabel, detailLabel));
        status.detailProperty().addListener(
                (observable, previous, current) -> renderComponent(
                        status, toggle, statusLabel, detailLabel));
        renderComponent(status, toggle, statusLabel, detailLabel);
    }

    private void renderComponent(
            ComponentStatus status,
            ToggleButton toggle,
            Label statusLabel,
            Label detailLabel) {
        ComponentState state = status.getState();
        boolean enabled = state == ComponentState.ENABLED_CURRENT
                || state == ComponentState.ENABLED_OTHER;
        toggle.setSelected(enabled);
        toggle.setText(enabled ? "关闭代理" : "开启代理");
        statusLabel.setText(switch (state) {
            case DISABLED -> "已关闭";
            case ENABLED_CURRENT -> "已开启";
            case ENABLED_OTHER -> "其他代理正在生效";
            case UNAVAILABLE -> "不可用";
            case ERROR -> "检测失败";
            case BUSY -> "处理中";
        });
        detailLabel.setText(status.getDetail());
        setStyleClass(statusLabel, "component-status-enabled",
                state == ComponentState.ENABLED_CURRENT);
        setStyleClass(statusLabel, "component-status-warning",
                state == ComponentState.ENABLED_OTHER);
        setStyleClass(statusLabel, "component-status-error",
                state == ComponentState.ERROR || state == ComponentState.UNAVAILABLE);
        refreshComponentControls();
    }

    private void refreshComponentControls() {
        updateToggleAvailability(ProxyComponent.APT, aptToggle);
        updateToggleAvailability(ProxyComponent.NPM, npmToggle);
    }

    private void updateToggleAvailability(ProxyComponent component, ToggleButton toggle) {
        ComponentStatusViewModel componentViewModel = mainViewModel.getComponent(component);
        ComponentState state = componentViewModel.getStatus().getState();
        boolean enabled = componentViewModel.isEnabled();
        MoeProxy selected = proxyViewModel.getSelectedProxy();
        boolean supportedSelection = selected != null
                && selected.getProtocol() != ProxyProtocol.SOCKS5;
        toggle.setDisable(mainViewModel.isOperationInProgress()
                || state == ComponentState.BUSY
                || state == ComponentState.UNAVAILABLE
                || (!enabled && !supportedSelection));
    }

    private void refreshProxyCards() {
        proxyList.getChildren().clear();
        proxyCards.clear();

        if (proxyViewModel.getProxies().isEmpty()) {
            Label emptyState = new Label("暂无代理配置，点击“新增代理”开始。");
            emptyState.getStyleClass().add("empty-state");
            proxyList.getChildren().add(emptyState);
        } else {
            proxyViewModel.getProxies().forEach(this::addProxyCard);
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
            controller.setSelectionHandler(selected -> runOperation(
                    "选择代理", () -> mainViewModel.selectProxy(selected.getId())));
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
        MoeProxy selected = proxyViewModel.getSelectedProxy();
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
            runOperation("新增代理", () -> mainViewModel.addProxy(
                    input.getName(), input.getHost(), input.getPort(), input.getProtocol()));
        } else {
            runOperation("编辑代理", () -> mainViewModel.updateProxy(
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
        confirmation.setContentText("此操作无法撤销。删除当前代理前会先关闭已开启组件。");
        if (proxyList.getScene() != null) {
            confirmation.initOwner(proxyList.getScene().getWindow());
        }
        if (confirmation.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            runOperation("删除代理", () -> mainViewModel.deleteProxy(proxy.getId()));
        }
    }

    private void runOperation(
            String action, Supplier<CompletableFuture<Void>> operation) {
        try {
            operation.get().whenComplete((ignored, failure) -> {
                if (failure != null) {
                    Platform.runLater(() -> showError(action + "失败", unwrap(failure)));
                }
            });
        } catch (RuntimeException exception) {
            showError(action + "失败", exception);
        }
    }

    private void showPersistenceState() {
        boolean showWarning = !proxyViewModel.isPersistent();
        persistenceWarningBanner.setManaged(showWarning);
        persistenceWarningBanner.setVisible(showWarning);
        persistenceWarningLabel.setText(proxyViewModel.getPersistenceWarning());
    }

    private void bindWindowLifecycle() {
        proxyList.sceneProperty().addListener((sceneObservable, oldScene, scene) -> {
            if (scene == null) {
                return;
            }
            scene.windowProperty().addListener((windowObservable, oldWindow, window) -> {
                if (window != null) {
                    attachWindow(window);
                }
            });
            if (scene.getWindow() != null) {
                attachWindow(scene.getWindow());
            }
        });
    }

    private void attachWindow(Window window) {
        window.focusedProperty().addListener((observable, previous, focused) -> {
            if (focused) {
                mainViewModel.refresh();
            }
        });
        window.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> mainViewModel.close());
    }

    private void showError(String action, Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("MoeQuick Gate");
        alert.setHeaderText(action);
        alert.setContentText("原因：" + displayMessage(throwable)
                + "\n建议：" + suggestion(throwable));
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

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String displayMessage(Throwable throwable) {
        Throwable current = unwrap(throwable);
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private static String suggestion(Throwable throwable) {
        Throwable current = unwrap(throwable);
        while (current != null) {
            if (current instanceof OperationException operationFailure) {
                return operationFailure.getSuggestion();
            }
            if (current instanceof ProxyOperationException proxyFailure) {
                return proxyFailure.getSuggestion();
            }
            current = current.getCause();
        }
        return "检查数据库目录、组件配置和文件权限后重试。";
    }

    private static void setStyleClass(Node node, String styleClass, boolean enabled) {
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
