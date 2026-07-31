package moe.div.moequickgate;

import javafx.application.Application;
import javafx.stage.Stage;
import moe.div.moequickgate.scene.MainScene;

/**
 * MoeQuick Gate 应用入口。
 * MoeQuick Gate application entry point.
 */
public final class App extends Application {
    public static final String APPLICATION_TITLE = "MoeQuick Gate";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(APPLICATION_TITLE);
        primaryStage.setScene(MainScene.create());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

