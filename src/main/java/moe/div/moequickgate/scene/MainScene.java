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
    private static final double DEFAULT_WIDTH = 900;
    private static final double DEFAULT_HEIGHT = 600;

    private MainScene() {
    }

    public static Scene create() {
        URL resource = MainScene.class.getResource(FXML_RESOURCE);
        if (resource == null) {
            throw new IllegalStateException(
                    "无法加载主界面 / Unable to load main scene: resource not found: " + FXML_RESOURCE);
        }

        try {
            Parent root = FXMLLoader.load(resource);
            return new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "无法加载主界面 / Unable to load main scene: " + FXML_RESOURCE,
                    exception);
        }
    }
}

