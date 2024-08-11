module org.example.tankoyunu {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.tankoyunu to javafx.fxml;
    exports org.example.tankoyunu;
}