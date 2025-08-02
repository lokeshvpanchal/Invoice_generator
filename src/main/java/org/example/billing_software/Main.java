package org.example.billing_software;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.billing_software.services.AutocompleteRepository;
import org.example.billing_software.services.InvoiceRepository;
import org.example.billing_software.views.CreateInvoiceForm;
import org.example.billing_software.views.InvoiceListView;
import org.example.billing_software.views.SalesChartView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DashboardApp provides the main JavaFX window with navigation
 * and delegates the invoice form to a separate CreateInvoiceForm class.
 */
public class Main extends Application {
    private StackPane workspace;
    private Connection conn;

    @Override
    public void start(Stage primaryStage) {
        // Initialize SQLite connection and tables
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:invoices.db");
            AutocompleteRepository.createTables(conn);
            InvoiceRepository.createSchema(conn);
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
        Button chartBtn= new Button("Charts");
        salesBtn.setMaxWidth(Double.MAX_VALUE);
        createInvoiceBtn.setMaxWidth(Double.MAX_VALUE);
        chartBtn.setMaxWidth(Double.MAX_VALUE);

        salesBtn.setOnAction(e -> showSalesView());
        createInvoiceBtn.setOnAction(e -> showCreateInvoiceView());
        chartBtn.setOnAction(e -> showInventoryView());

        navBar.getChildren().addAll(salesBtn, chartBtn,createInvoiceBtn);

        // Workspace
        workspace = new StackPane();
        workspace.setStyle("-fx-background-color: #ECF0F1;");

        // Main layout
        BorderPane root = new BorderPane();
        root.setLeft(navBar);
        root.setCenter(workspace);

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(
                getClass().getResource("/styles/application.css").toExternalForm()
        );
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
        ScrollPane scroller = new ScrollPane(formView);
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(true);  // optional, if you want vertical fit too
        workspace.getChildren().setAll(scroller);
    }
    private void showSalesView() {
        Node invoiceList = InvoiceListView.create(conn);
        workspace.getChildren().setAll(invoiceList);
    }

    private void showInventoryView() {
        Node salesChart = SalesChartView.create(conn);
        workspace.getChildren().setAll(salesChart);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
