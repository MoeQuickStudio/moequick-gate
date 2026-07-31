module moe.div.moequickgate {
    requires javafx.controls;
    requires javafx.fxml;

    exports moe.div.moequickgate;
    exports moe.div.moequickgate.scene;

    opens moe.div.moequickgate.controller to javafx.fxml;
    opens moe.div.moequickgate.scene to javafx.fxml;
}
