module moe.div.moequickgate {
    requires java.sql;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.xerial.sqlitejdbc;

    exports moe.div.moequickgate;
    exports moe.div.moequickgate.scene;

    opens moe.div.moequickgate.controller to javafx.fxml;
    opens moe.div.moequickgate.scene to javafx.fxml;
}
