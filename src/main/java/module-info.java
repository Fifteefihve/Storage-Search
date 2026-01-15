module org.example.storagesearch {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.storagesearch to javafx.fxml;
    exports org.example.storagesearch;
}