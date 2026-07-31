package moe.div.moequickgate.scene;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * 创建并加载主场景。
 * Creates and loads the main scene.
 */
public final class MainScene {
    public static final String FXML_RESOURCE = "/fxml/main.fxml";
    public static final String CSS_RESOURCE = "/css/style.css";
    private static final double DEFAULT_WIDTH = 900;
    private static final double DEFAULT_HEIGHT = 600;

    private MainScene() {
    }

    public static Scene create() {
        URL fxmlResource = requireResource(FXML_RESOURCE, "主界面 / main scene");
        URL cssResource = requireResource(CSS_RESOURCE, "界面样式 / stylesheet");

        try {
            Parent root = FXMLLoader.load(fxmlResource);
            Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            scene.getStylesheets().add(cssResource.toExternalForm());
            return scene;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "无法加载主界面 / Unable to load main scene: " + FXML_RESOURCE,
                    exception);
        }
    }

    private static URL requireResource(String path, String description) {
        URL resource = MainScene.class.getResource(path);
        if (resource == null) {
            throw new IllegalStateException(
                    "无法加载" + description + " / Unable to load " + description
                            + ": resource not found: " + path);
        }
        return resource;
    }
}
