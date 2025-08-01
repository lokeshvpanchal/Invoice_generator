package org.example.billing_software.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.example.billing_software.models.InvoiceSummary;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.billing_software.services.InvoiceRepository.*;

public class InvoiceListView {

    public static Node create(Connection conn) {
        HBox root = new HBox(20);
        root.setPadding(new Insets(20));

        VBox leftPane = new VBox(10);
        VBox rightPane = new VBox(10);
        leftPane.setPrefWidth(600);
        rightPane.setPadding(new Insets(0, 0, 0, 20));
        rightPane.setPrefWidth(300);

        Label title = new Label("All Invoices (sorted by date):");

        // Search
        TextField searchField = new TextField();
        searchField.setPromptText("Search by client name");

        // Date range and granularity filter
        LocalDate today = LocalDate.now();

        ComboBox<Integer> yearBox = new ComboBox<>();
        ComboBox<Integer> monthBox = new ComboBox<>();
        int currentYear = today.getYear();
        List<Integer> years = new ArrayList<>();
        for (int i = currentYear; i >= currentYear - 10; i--) years.add(i);
        yearBox.setItems(FXCollections.observableArrayList(years));
        yearBox.setValue(currentYear);

        monthBox.setItems(FXCollections.observableArrayList(
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
        ));
        monthBox.setValue(today.getMonthValue());

        Button applyFilters = new Button("Apply Filters");

        Label statsLabel = new Label("Sales Summary");

        TableView<InvoiceSummary> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<InvoiceSummary, String> invoiceCol = new TableColumn<>("Invoice No");
        invoiceCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));

        TableColumn<InvoiceSummary, String> clientCol = new TableColumn<>("Client");
        clientCol.setCellValueFactory(new PropertyValueFactory<>("client"));

        TableColumn<InvoiceSummary, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<InvoiceSummary, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));

        TableColumn<InvoiceSummary, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button deleteBtn = new Button("Delete");
            private final Button editBtn = new Button("Edit");
            private final HBox actionBox = new HBox(5, viewBtn, editBtn, deleteBtn);

            {
                viewBtn.setOnAction(e -> {
                    InvoiceSummary invoice = getTableView().getItems().get(getIndex());
                    Stage detailStage = new Stage();
                    detailStage.setScene(new Scene(
                            (Parent) InvoiceDetailView.create(conn, invoice.getInvoiceNo()),
                            600, 500
                    ));
                    detailStage.setTitle("Invoice Details #" + invoice.getInvoiceNo());
                    detailStage.show();
                });


                deleteBtn.setOnAction(e -> {
                    InvoiceSummary invoice = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete Invoice #" + invoice.getInvoiceNo() + "?",
                            ButtonType.YES, ButtonType.NO);
                    confirm.setTitle("Delete Invoice");
                    confirm.setHeaderText("Confirm Deletion");
                    Optional<ButtonType> result = confirm.showAndWait();

                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        deleteInvoice(conn, invoice.getInvoiceNo());
                    }

                    ObservableList<InvoiceSummary> masterList = FXCollections.observableArrayList(fetchInvoices(conn));
                    FilteredList<InvoiceSummary> filteredData = new FilteredList<>(masterList, p -> true);
                    table.setItems(filteredData);
                });
                editBtn.setOnAction(e -> {
                    InvoiceSummary invoice = getTableView().getItems().get(getIndex());
                    Stage stage = new Stage();
                    stage.setScene(new Scene((Parent) EditInvoiceForm.create(conn, invoice.getInvoiceNo()), 700, 600));
                    stage.setTitle("Edit Invoice #" + invoice.getInvoiceNo());
                    stage.showAndWait();

                    ObservableList<InvoiceSummary> masterList = FXCollections.observableArrayList(fetchInvoices(conn));
                    FilteredList<InvoiceSummary> filteredData = new FilteredList<>(masterList, p -> true);
                    table.setItems(filteredData);
                });

            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });

        table.getColumns().addAll(invoiceCol, clientCol, dateCol, totalCol, actionCol);

        ObservableList<InvoiceSummary> masterList = FXCollections.observableArrayList(fetchInvoices(conn));
        FilteredList<InvoiceSummary> filteredData = new FilteredList<>(masterList, p -> true);
        table.setItems(filteredData);

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredData.setPredicate(invoice -> {
                if (newV == null || newV.isEmpty()) return true;
                return invoice.getClient().toLowerCase().contains(newV.toLowerCase());
            });
            statsLabel.setText(getSalesStats(filteredData));
        });

        applyFilters.setOnAction(e -> {
            int year = yearBox.getValue();
            int month = monthBox.getValue();
            List<InvoiceSummary> filtered = fetchInvoicesWithYearMonth(conn, year, month);
            masterList.setAll(filtered);
            statsLabel.setText(getSalesStats(filteredData));
        });

        statsLabel.setText(getSalesStats(filteredData));

        rightPane.getChildren().addAll(
                new Label("Filter Invoices:"),
                new Label("Year:"), yearBox,
                new Label("Month:"), monthBox,
                applyFilters
        );

        leftPane.getChildren().addAll(searchField, statsLabel, title, table);
        root.getChildren().addAll(leftPane, rightPane);
        return root;
    }


    private static List<InvoiceSummary> fetchInvoices(Connection conn) {
        return fetchInvoicesWithDateRange(conn, null, null);
    }


    private static String getSalesStats(List<InvoiceSummary> invoices) {
        if (invoices.isEmpty()) return "No data available.";

        double totalSales = invoices.stream().mapToDouble(InvoiceSummary::getTotal).sum();
        double avgPerClient = invoices.stream()
                .collect(Collectors.groupingBy(InvoiceSummary::getClient))
                .values()
                .stream()
                .mapToDouble(list -> list.stream().mapToDouble(InvoiceSummary::getTotal).sum())
                .average()
                .orElse(0);

        return String.format("Total Sales: ₹%.2f | Avg per Client: ₹%.2f", totalSales, avgPerClient);
    }

}
