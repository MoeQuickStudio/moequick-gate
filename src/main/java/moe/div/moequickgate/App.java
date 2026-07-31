package moe.div.moequickgate;

import java.net.URL;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import moe.div.moequickgate.scene.MainScene;

/**
 * MoeQuick Gate 应用入口。
 * MoeQuick Gate application entry point.
 */
public final class App extends Application {
    public static final String APPLICATION_TITLE = "MoeQuick Gate";
    private static final String APPLICATION_ICON_RESOURCE = "/icon/moequick-gate.png";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(APPLICATION_TITLE);
        primaryStage.getIcons().add(loadApplicationIcon());
        primaryStage.setScene(MainScene.create());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static Image loadApplicationIcon() {
        URL resource = App.class.getResource(APPLICATION_ICON_RESOURCE);
        if (resource == null) {
            throw new IllegalStateException(
                    "无法加载应用图标 / Unable to load application icon: resource not found: "
                            + APPLICATION_ICON_RESOURCE);
        }
        return new Image(resource.toExternalForm());
    }
}
