package org.example.billing_software;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DashboardApp provides the main JavaFX window with navigation
 * and delegates the invoice form to a separate CreateInvoiceForm class.
 */
public class DashboardApp extends Application {
    private StackPane workspace;
    private Connection conn;

    @Override
    public void start(Stage primaryStage) {
        // Initialize SQLite connection and tables
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:invoices.db");
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Navigation bar
        VBox navBar = new VBox(15);
        navBar.setPadding(new Insets(20));
        navBar.setStyle("-fx-background-color: #2C3E50;");

        Button salesBtn = new Button("Sales");
        Button createInvoiceBtn = new Button("Create Invoice");
        Button inventoryBtn = new Button("Inventory Items");
        salesBtn.setMaxWidth(Double.MAX_VALUE);
        createInvoiceBtn.setMaxWidth(Double.MAX_VALUE);
        inventoryBtn.setMaxWidth(Double.MAX_VALUE);

        salesBtn.setOnAction(e -> showSalesView());
        createInvoiceBtn.setOnAction(e -> showCreateInvoiceView());
        inventoryBtn.setOnAction(e -> showInventoryView());

        navBar.getChildren().addAll(salesBtn, createInvoiceBtn, inventoryBtn);

        // Workspace
        workspace = new StackPane();
        workspace.setStyle("-fx-background-color: #ECF0F1;");

        // Main layout
        BorderPane root = new BorderPane();
        root.setLeft(navBar);
        root.setCenter(workspace);

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("Invoice Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Default view
        showCreateInvoiceView();
    }

    /**
     * Shows the Create Invoice form from the modular form class.
     */
    private void showCreateInvoiceView() {
        Node formView = CreateInvoiceForm.create(conn);
        workspace.getChildren().setAll(formView);
    }

    private void showSalesView() {
        workspace.getChildren().setAll(new Button("[Sales view placeholder]"));
    }

    private void showInventoryView() {
        workspace.getChildren().setAll(new Button("[Inventory view placeholder]"));
    }

    private void createTables() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS sales(invoice_number TEXT PRIMARY KEY, client_name TEXT, gst_no TEXT, date TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS items(item_id INTEGER PRIMARY KEY AUTOINCREMENT, description TEXT UNIQUE, rate REAL)");
            st.execute("CREATE TABLE IF NOT EXISTS invoice_items(invoice_number TEXT, item_id INTEGER, quantity INTEGER, rate REAL, " +
                    "FOREIGN KEY(invoice_number) REFERENCES sales(invoice_number), " +
                    "FOREIGN KEY(item_id) REFERENCES items(item_id))");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
