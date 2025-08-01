// src/main/java/org/example/billing_software/views/InvoiceDetailView.java
package org.example.billing_software.views;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.example.billing_software.models.InvoiceData;
import org.example.billing_software.models.LineItem;
import org.example.billing_software.services.InvoiceRepository;

import java.sql.Connection;

public class InvoiceDetailView {
    public static Node create(Connection conn, String invoiceNo) {
        InvoiceData data = InvoiceRepository.fetchInvoice(conn, invoiceNo);
        if (data == null) {
            return new Label("Invoice not found: " + invoiceNo);
        }

        // Header info
        Label hdr = new Label("Invoice #" + invoiceNo);
        Label client = new Label("Client:    " + data.getClient());
        Label date   = new Label("Date:      " + data.getDate());
        Label gstNo  = new Label("GST No:    " + data.getGst());
        Label car    = new Label("Car:       "
                + data.getCarMake() + " / "
                + data.getCarModel() + " ("
                + data.getCarLicense() + ")");

        // Items table
        TableView<LineItem> table = new TableView<>(
                FXCollections.observableArrayList(data.getItems())
        );
        TableColumn<LineItem, String> partCol = new TableColumn<>("Particulars");
        partCol.setCellValueFactory(cell -> cell.getValue().particulars);
        TableColumn<LineItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol .setCellValueFactory(cell -> cell.getValue().quantity   .asObject());
        TableColumn<LineItem, Double> rateCol = new TableColumn<>("Rate");
        rateCol.setCellValueFactory(cell -> cell.getValue().rate       .asObject());
        TableColumn<LineItem, Double> amtCol = new TableColumn<>("Amount");
        amtCol .setCellValueFactory(cell -> cell.getValue().amount     .asObject());
        table.getColumns().addAll(partCol, qtyCol, rateCol, amtCol);

        // Totals
        Label subtotal = new Label(String.format("Subtotal: ₹%.2f", data.getSubtotal()));
        Label cgst     = new Label(String.format("CGST:     ₹%.2f", data.getCgst()));
        Label sgst     = new Label(String.format("SGST:     ₹%.2f", data.getSgst()));
        Label total    = new Label(String.format("Total:    ₹%.2f", data.getTotal()));

        VBox root = new VBox(10, hdr, client, date, gstNo, car, table, subtotal, cgst, sgst, total);
        root.setPadding(new Insets(20));
        return root;
    }
}
