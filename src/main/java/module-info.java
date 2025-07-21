module org.example.billing_software {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.sql;
    requires java.mail;
    requires activation;
    requires org.apache.pdfbox;
    requires java.desktop;

    opens org.example.billing_software to javafx.fxml;
    exports org.example.billing_software;
}