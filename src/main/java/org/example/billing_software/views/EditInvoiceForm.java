package org.example.billing_software.views;

import javafx.stage.Stage;
import org.example.billing_software.models.LineItem;
import org.example.billing_software.models.InvoiceData;
import org.example.billing_software.services.AutocompleteRepository;

import org.example.billing_software.services.InvoiceRepository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.example.billing_software.utils.AutocompleteUtil;
import org.example.billing_software.utils.TrieAutocomplete;


import java.sql.Connection;
import java.time.LocalDate;

import static org.example.billing_software.views.CreateInvoiceForm.updateTotals;

public class EditInvoiceForm {
    private static final double CGST_RATE = 0.09;
    private static final double SGST_RATE = 0.09;

    public static Node create(Connection conn, String invoiceNo) {
        // Load existing invoice data
        InvoiceData data = InvoiceRepository.fetchInvoice(conn, invoiceNo);

        // Autocomplete tries
        TrieAutocomplete carMakeTrie = new TrieAutocomplete();
        TrieAutocomplete carModelTrie = new TrieAutocomplete();
        TrieAutocomplete particularsTrie = new TrieAutocomplete();
        AutocompleteRepository.loadIntoTrie(conn, "car_makes", carMakeTrie);
        AutocompleteRepository.loadIntoTrie(conn, "car_models", carModelTrie);
        AutocompleteRepository.loadIntoTrie(conn, "particulars", particularsTrie);

        // Root layout
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        // Header form
        GridPane header = new GridPane();
        header.setHgap(10);
        header.setVgap(10);

        TextField billField = new TextField(data.getInvoiceNo());
        billField.setDisable(true);
        TextField nameField = new TextField(data.getClient());
        TextField gstField = new TextField(data.getGst());
        DatePicker datePicker = new DatePicker(LocalDate.parse(data.getDate()));
        TextField carMakeField = new TextField(data.getCarMake());
        TextField carModelField = new TextField(data.getCarModel());
        TextField carLicenseField = new TextField(data.getCarLicense());

        AutocompleteUtil.addAutocomplete(carMakeField, carMakeTrie);
        AutocompleteUtil.addAutocomplete(carModelField, carModelTrie);

        header.addRow(0, new Label("Bill No:"), billField);
        header.addRow(1, new Label("Client Name:"), nameField);
        header.addRow(2, new Label("GST No (opt):"), gstField);
        header.addRow(3, new Label("Date:"), datePicker);
        header.addRow(4, new Label("Car Make:"), carMakeField);
        header.addRow(5, new Label("Car Model:"), carModelField);
        header.addRow(6, new Label("Car License No.:"), carLicenseField);

        // Items section
        VBox itemsBox = new VBox(5);
        itemsBox.setPadding(new Insets(10, 0, 10, 0));
        ObservableList<LineItem> items = FXCollections.observableArrayList(data.getItems());

        HBox itemsHeader = new HBox(10,
                new Label("Particulars"),
                new Label("Quantity") {{ setPrefWidth(60); }},
                new Label("Rate with tax")   {{ setPrefWidth(80); }},
                new Label("Rate")   {{ setPrefWidth(80); }},
                new Label("Action")   {{ setPrefWidth(80); }}
        );
        itemsBox.getChildren().add(itemsHeader);

        // Totals
        GridPane totals = new GridPane();
        totals.setHgap(10);
        totals.setVgap(10);
        TextField subField  = makeReadonlyField();
        TextField cgstField = makeReadonlyField();
        TextField sgstField = makeReadonlyField();
        TextField totField  = makeReadonlyField();
        totals.addRow(0, new Label("Subtotal:"), subField);
        totals.addRow(1, new Label("CGST:"), cgstField);
        totals.addRow(2, new Label("SGST:"), sgstField);
        totals.addRow(3, new Label("Total:"), totField);

        // Runnable for adding rows
        Runnable addRow = new Runnable() {
            @Override
            public void run() {
                LineItem item = new LineItem();
                items.add(item);
                HBox row = CreateInvoiceForm.createRow(
                        item, this, items,
                        subField, cgstField, sgstField, totField,
                        particularsTrie
                );
                itemsBox.getChildren().add(row);
                updateTotals(items, subField, cgstField, sgstField, totField);
            }
        };


        // Prepopulate rows
        for (LineItem existing : data.getItems()) {
            HBox row = CreateInvoiceForm.createRow(
                    existing, addRow, items,
                    subField, cgstField, sgstField, totField,
                    particularsTrie
            );
            itemsBox.getChildren().add(row);
        }

        Button addItem = new Button("Add Item");
        addItem.setOnAction(e -> addRow.run());

        // Control buttons
        Button saveBtn  = new Button("Update Invoice");

        HBox actions = new HBox(10, saveBtn);

        // Initial totals
        updateTotals(items, subField, cgstField, sgstField, totField);

        // Save action
        saveBtn.setOnAction(e -> {
            if (nameField.getText().trim().isEmpty() ||
                    datePicker.getValue() == null ||
                    carMakeField.getText().trim().isEmpty() ||
                    carModelField.getText().trim().isEmpty() ||
                    carLicenseField.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Please fill in all required fields (Client, Date, Car details)." ).showAndWait();
                return;
            }
            if (items.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Please add at least one item.").showAndWait();
                return;
            }
            for (LineItem item : items) {
                if (item.particulars.get().trim().isEmpty() || item.quantity.get() < 0 || item.rate.get() < 0) {
                    new Alert(Alert.AlertType.ERROR, "Each item must have valid particulars, non-negative quantity and rate.").showAndWait();
                    return;
                }
            }

            updateTotals(items, subField, cgstField, sgstField, totField);

            InvoiceData updated = new InvoiceData(
                    billField.getText(),
                    nameField.getText(),
                    gstField.getText(),
                    datePicker.getValue().toString(),
                    items,
                    carMakeField.getText(),
                    carModelField.getText(),
                    carLicenseField.getText(),
                    Double.parseDouble(subField.getText()),
                    Double.parseDouble(cgstField.getText()),
                    Double.parseDouble(sgstField.getText()),
                    Double.parseDouble(totField.getText())
            );
            boolean ok = InvoiceRepository.updateInvoice(conn, updated);
            if (ok) {
                // Store autocomplete
                AutocompleteRepository.insertOrUpdate(conn, "car_makes", carMakeField.getText());
                AutocompleteRepository.insertOrUpdate(conn, "car_models", carModelField.getText());
                for (LineItem item : items) {
                    AutocompleteRepository.insertOrUpdate(conn, "particulars", item.particulars.get());
                }
                PostSaveOptions.show(updated);
                Stage stage = (Stage) saveBtn.getScene().getWindow();
                stage.close();
                // Reset fields
                nameField.clear();
                gstField.clear();
                datePicker.setValue(LocalDate.now());
                carMakeField.clear();
                carModelField.clear();
                carLicenseField.clear();
                items.clear();
                itemsBox.getChildren().clear();
                itemsBox.getChildren().add(itemsHeader); // re-add header

                // Add a fresh row
                addRow.run();

                // Reset totals
                subField.clear();
                cgstField.clear();
                sgstField.clear();
                totField.clear();
            } else {
                new Alert(Alert.AlertType.ERROR, "Failed to save invoice.").showAndWait();
            }        });


        root.getChildren().addAll(
                new Label("Edit Invoice #" + invoiceNo),
                header,
                new Label("Items:"), itemsBox,
                addItem,
                totals,
                actions
        );
        return root;
    }

    private static TextField makeReadonlyField() {
        TextField tf = new TextField();
        tf.setEditable(false);
        return tf;
    }

}
