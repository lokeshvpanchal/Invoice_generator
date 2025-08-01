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

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class InvoiceListView {

    public static Node create(Connection conn) {
        HBox root = new HBox(20);
        root.setPadding(new Insets(20));

        VBox leftPane = new VBox(10);
        VBox rightPane = new VBox(10);
        rightPane.setPadding(new Insets(0, 0, 0, 20));
        rightPane.setPrefWidth(300);

        Label title = new Label("All Invoices (sorted by date):");

        // Search
        TextField searchField = new TextField();
        searchField.setPromptText("Search by client name");

        // Date range and granularity filter
        LocalDate today = LocalDate.now();

        DatePicker startDatePicker = new DatePicker(today.minusDays(7));
        DatePicker endDatePicker = new DatePicker(today);
        ComboBox<String> granularityBox = new ComboBox<>();
        granularityBox.getItems().addAll("Daily", "Monthly", "Yearly");
        granularityBox.setValue("Monthly");

        Button applyFilters = new Button("Apply Filters");

        Label statsLabel = new Label("Sales Summary");

        TableView<InvoiceSummary> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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
            private final HBox actionBox = new HBox(5, viewBtn, deleteBtn);

            {
                viewBtn.setOnAction(e -> {
                    InvoiceSummary invoice = getTableView().getItems().get(getIndex());
                    showInvoiceDetails(invoice.getInvoiceNo(), conn);
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
                        getTableView().getItems().remove(invoice);
                        statsLabel.setText(getSalesStats(getTableView().getItems()));
                    }
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
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            List<InvoiceSummary> filtered = fetchInvoicesWithDateRange(conn, start, end);
            masterList.setAll(filtered);
            statsLabel.setText(getSalesStats(filteredData));
        });

        statsLabel.setText(getSalesStats(filteredData));

        rightPane.getChildren().addAll(
                new Label("Filter Invoices:"),
                new Label("Start Date:"), startDatePicker,
                new Label("End Date:"), endDatePicker,
                new Label("Granularity:"), granularityBox,
                applyFilters
        );

        leftPane.getChildren().addAll(searchField, statsLabel, title, table);
        root.getChildren().addAll(leftPane, rightPane);
        return root;
    }

    private static List<InvoiceSummary> fetchInvoices(Connection conn) {
        return fetchInvoicesWithDateRange(conn, null, null);
    }

    private static List<InvoiceSummary> fetchInvoicesWithDateRange(Connection conn, LocalDate start, LocalDate end) {
        List<InvoiceSummary> list = new ArrayList<>();
        String sql = "SELECT invoice_no, client, date, total FROM invoices";

        if (start != null && end != null) {
            sql += " WHERE date BETWEEN ? AND ?";
        } else {
            sql += " ORDER BY date DESC";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (start != null && end != null) {
                ps.setString(1, start.toString());
                ps.setString(2, end.toString());
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new InvoiceSummary(
                        rs.getString("invoice_no"),
                        rs.getString("client"),
                        rs.getString("date"),
                        rs.getDouble("total")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
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

    private static void showInvoiceDetails(String invoiceNo, Connection conn) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Invoice Details");
        alert.setHeaderText("Invoice #" + invoiceNo);

        StringBuilder details = new StringBuilder();

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM invoices WHERE invoice_no = ?")) {
            ps.setString(1, invoiceNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                details.append("Client: ").append(rs.getString("client")).append("\n")
                        .append("Date: ").append(rs.getString("date")).append("\n")
                        .append("Total: ₹").append(rs.getDouble("total")).append("\n")
                        .append("GST No: ").append(rs.getString("gst")).append("\n\n")
                        .append("Items:\n");
            }
        } catch (SQLException e) {
            details.append("Error fetching main invoice.\n");
            e.printStackTrace();
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM invoice_items WHERE invoice_no = ?")) {
            ps.setString(1, invoiceNo);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String item = rs.getString("particulars");
                int qty = rs.getInt("quantity");
                double rate = rs.getDouble("rate");
                double amount = rs.getDouble("amount");
                details.append(String.format("• %s | Qty: %d | Rate: ₹%.2f | Amount: ₹%.2f\n", item, qty, rate, amount));
            }
        } catch (SQLException e) {
            details.append("Error fetching items.");
            e.printStackTrace();
        }

        alert.setContentText(details.toString());
        alert.showAndWait();
    }

    private static void deleteInvoice(Connection conn, String invoiceNo) {
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement delItems = conn.prepareStatement("DELETE FROM invoice_items WHERE invoice_no = ?");
                 PreparedStatement delInvoice = conn.prepareStatement("DELETE FROM invoices WHERE invoice_no = ?")) {

                delItems.setString(1, invoiceNo);
                delItems.executeUpdate();

                delInvoice.setString(1, invoiceNo);
                delInvoice.executeUpdate();

                conn.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static class InvoiceSummary {
        private final String invoiceNo;
        private final String client;
        private final String date;
        private final double total;

        public InvoiceSummary(String invoiceNo, String client, String date, double total) {
            this.invoiceNo = invoiceNo;
            this.client = client;
            this.date = date;
            this.total = total;
        }

        public String getInvoiceNo() { return invoiceNo; }
        public String getClient() { return client; }
        public String getDate() { return date; }
        public double getTotal() { return total; }
    }
}
