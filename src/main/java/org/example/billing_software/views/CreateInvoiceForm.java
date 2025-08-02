package org.example.billing_software.views;
import javafx.application.Platform;
import javafx.geometry.Pos;
import org.example.billing_software.models.LineItem;
import org.example.billing_software.services.AutocompleteRepository;
import org.example.billing_software.services.EmailSender;
import org.example.billing_software.models.InvoiceData;
import org.example.billing_software.services.InvoiceRepository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.example.billing_software.utils.AutocompleteUtil;
import org.example.billing_software.utils.TrieAutocomplete;

import java.awt.print.PrinterException;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.function.UnaryOperator;

public class CreateInvoiceForm {
    private static final double CGST_RATE = 0.09;
    private static final double SGST_RATE = 0.09;

    public static Node create(Connection conn) {
        // Initialize Trie Autocomplete structures
        TrieAutocomplete carMakeTrie = new TrieAutocomplete();
        TrieAutocomplete carModelTrie = new TrieAutocomplete();
        TrieAutocomplete particularsTrie = new TrieAutocomplete();

        // Load autocomplete data from SQLite
        AutocompleteRepository.loadIntoTrie(conn, "car_makes", carMakeTrie);
        AutocompleteRepository.loadIntoTrie(conn, "car_models", carModelTrie);
        AutocompleteRepository.loadIntoTrie(conn, "particulars", particularsTrie);

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setFillWidth(false);


        // Header
        GridPane header = new GridPane();
        header.setHgap(70);
        header.setVgap(10);
        TextField billField = new TextField(String.valueOf(InvoiceRepository.fetchNextBill(conn)));
        billField.setDisable(true);
        header.addRow(0, new Label("Bill No:"), billField);

        TextField nameField = new TextField();
        TextField gstField = new TextField(); gstField.setPromptText("Optional");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField carMakeField = new TextField();
        TextField carModelField = new TextField();
        TextField carLicenseField = new TextField();

        AutocompleteUtil.addAutocomplete(carMakeField, carMakeTrie);
        AutocompleteUtil.addAutocomplete(carModelField, carModelTrie);


        header.addRow(0, new Label("Date:"), datePicker);
        header.addRow(2, new Label("Client Name:"), nameField);
        header.addRow(2, new Label("GST No (opt):"), gstField);
        header.addRow(4, new Label("Car Make:"), carMakeField);
        header.addRow(4, new Label("Car Model:"), carModelField);
        header.addRow(5, new Label("Car License No. :"), carLicenseField);

        header.addRow(7,new Label(""));
        // Items
        VBox itemsBox = new VBox(5);
        itemsBox.setPadding(new Insets(10, 0, 10, 0));
        ObservableList<LineItem> items = FXCollections.observableArrayList();

        Label hdrPart = new Label("Particulars"); hdrPart.setPrefWidth(160);
        Label hdrQty = new Label("Quantity"); hdrQty.setPrefWidth(100);
        Label hdrRwt = new Label("Rate incl. Tax"); hdrRwt.setPrefWidth(135);
        Label hdrRate = new Label("Rate"); hdrRate.setPrefWidth(135);
        Label hdrAmount = new Label("Amount"); hdrAmount.setPrefWidth(135);
        Label hdrAction = new Label("Action"); hdrAction.setPrefWidth(80);
        HBox itemsHeader = new HBox(10, hdrPart, hdrQty, hdrRwt,hdrRate, hdrAmount,hdrAction);
        itemsBox.getChildren().add(itemsHeader);

        // Totals
        GridPane totals = new GridPane();

        totals.setPadding(new Insets(50, 0, 0, 0));  // 200px top padding

        totals.setTranslateX(540);
        totals.setHgap(10);
        totals.setVgap(10);
        TextField subField = makeReadonlyField();
        TextField cgstField = makeReadonlyField();
        TextField sgstField = makeReadonlyField();
        TextField totField = makeReadonlyField();
        totals.addRow(0, new Label("Subtotal:"), subField);
        totals.addRow(1, new Label("CGST:"), cgstField);
        totals.addRow(2, new Label("SGST:"), sgstField);
        totals.addRow(3, new Label("Total:"), totField);

        // Row adder
        Runnable addRow = new Runnable() {
            @Override
            public void run() {
                LineItem item = new LineItem();
                items.add(item);
                HBox row = createRow(item, this, items, subField, cgstField, sgstField, totField, particularsTrie);
                itemsBox.getChildren().add(row);
                updateTotals(items, subField, cgstField, sgstField, totField);
            }
        };

        addRow.run(); // Add initial row

        Button addItem = new Button("Add Item");
        addItem.getStyleClass().add("add-button");


        addItem.setOnAction(e ->{
            addRow.run();
        });

        HBox h = new HBox(50);

        // Buttons
        Button saveBtn = new Button("Save Invoice");
        saveBtn.getStyleClass().add("save-button");
        saveBtn.setTranslateX(totals.getTranslateX());
        saveBtn.setOnAction(e -> {

            // validation

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
            InvoiceData data = new InvoiceData(
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

            boolean success = InvoiceRepository.saveInvoice(conn, data);

            if (success) {
                billField.setText(String.valueOf(InvoiceRepository.fetchNextBill(conn)));

                // Store autocomplete
                AutocompleteRepository.insertOrUpdate(conn, "car_makes", carMakeField.getText());
                AutocompleteRepository.insertOrUpdate(conn, "car_models", carModelField.getText());
                for (LineItem item : items) {
                    AutocompleteRepository.insertOrUpdate(conn, "particulars", item.particulars.get());
                }

                PostSaveOptions.show(data);

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
            }
        });

        HBox actions = new HBox(saveBtn);
        actions.setPadding(new Insets(20, 0, 0, 0));


        root.getChildren().addAll(header, h,itemsBox,addItem,totals, actions);
        return root;
    }


    private static TextField makeReadonlyField() {
        TextField tf = new TextField();
        tf.setEditable(false);
        return tf;
    }

    public static HBox createRow(
            LineItem item,
            Runnable addRow,
            ObservableList<LineItem> items,
            TextField subField,
            TextField cgstField,
            TextField sgstField,
            TextField totField,
            TrieAutocomplete particularsTrie

    ) {
        HBox row = new HBox(25);

        // filters for numeric input
        UnaryOperator<Change> intFilter = c ->
                c.getControlNewText().matches("\\d*") ? c : null;
        UnaryOperator<Change> decFilter = c ->
                c.getControlNewText().matches("\\d*(\\.\\d*)?") ? c : null;

        // particulars
        TextField partField = new TextField(item.particulars.get());
        AutocompleteUtil.addAutocomplete(partField, particularsTrie);
        partField.setPromptText("Particulars");
        partField.textProperty().addListener((obs, oldV, newV) ->
                item.particulars.set(newV)
        );

        // quantity
        TextField qtyField = new TextField(String.valueOf(item.quantity.get()));
        qtyField.setPrefWidth(80);
        qtyField.setTextFormatter(new TextFormatter<>(intFilter));

        //rate with tax
        TextField rateWithTaxField = new TextField(String.format("%.2f", item.rate.get()* 1.18 ));
        rateWithTaxField.setPrefWidth(120);
        rateWithTaxField.setTextFormatter(new TextFormatter<>(decFilter));
        // rate
        TextField rateField = new TextField(String.format("%.2f", item.rate.get()));
        rateField.setPrefWidth(120);
        rateField.setTextFormatter(new TextFormatter<>(decFilter));

        // amount (read‑only, auto)
        TextField amtField = new TextField("0.00");
        amtField.setPrefWidth(120);
        amtField.setEditable(false);

        // when quantity changes, recalc amount
        qtyField.textProperty().addListener((obs, oldV, newV) -> {
            int q = newV.isEmpty() ? 0 : Integer.parseInt(newV);
            item.quantity.set(q);
            double r = item.rate.get();
            amtField.setText(String.format("%.2f", r * q));
        });

        // When rateWithTax changes, update base rate
        rateWithTaxField.textProperty().addListener((obs, oldV, newV) -> {
            double rwt = newV.isEmpty() ? 0.0 : Double.parseDouble(newV);
            double baseRate = rwt / 1.18;
            rateField.setText(String.format("%.2f", baseRate));
            // rateField listener will take care of setting item.rate and amount
        });

        // when rate changes, recalc amount
        rateField.textProperty().addListener((obs, oldV, newV) -> {
            double r = newV.isEmpty() ? 0.0 : Double.parseDouble(newV);
            item.rate.set(r);
            int q = item.quantity.get();
            amtField.setText(String.format("%.2f", r * q));
        });


        // when amount changes, update backing property and totals
        amtField.textProperty().addListener((obs, oldV, newV) -> {
            item.amount.set(newV.isEmpty() ? 0.0 : Double.parseDouble(newV));
            updateTotals(items, subField, cgstField, sgstField, totField);
        });

        // ENTER in amount adds a new row
        rateField.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ENTER) {
                addRow.run();
                evt.consume();
                // now move focus to the particulars of that new row:
                Platform.runLater(() -> {
                    VBox container = (VBox) row.getParent();
                    // last child is the new HBox
                    HBox newRow = (HBox) container.getChildren().get(container.getChildren().size() - 1);
                    // first child of that HBox is the particulars TextField
                    TextField newPart = (TextField) newRow.getChildren().get(0);
                    newPart.requestFocus();
                });
            }
        });
        rateWithTaxField.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ENTER) {
                addRow.run();
                evt.consume();
                // now move focus to the particulars of that new row:
                Platform.runLater(() -> {
                    VBox container = (VBox) row.getParent();
                    // last child is the new HBox
                    HBox newRow = (HBox) container.getChildren().get(container.getChildren().size() - 1);
                    // first child of that HBox is the particulars TextField
                    TextField newPart = (TextField) newRow.getChildren().get(0);
                    newPart.requestFocus();
                });
            }
        });

        // delete button
        Button delBtn = new Button("Delete");
        delBtn.getStyleClass().add("delete-button");

        delBtn.setOnAction(evt -> {
            items.remove(item);
            ((VBox) row.getParent()).getChildren().remove(row);
            updateTotals(items, subField, cgstField, sgstField, totField);
        });

        row.getChildren().addAll(partField, qtyField,rateWithTaxField, rateField, amtField, delBtn);
        return row;
    }


    public static void updateTotals(ObservableList<LineItem> items,
                                     TextField subField,
                                     TextField cgstField,
                                     TextField sgstField,
                                     TextField totField) {
        double sum = items.stream().mapToDouble(i -> i.amount.get()).sum();
        double subtotal = sum;
        double cgst = subtotal * CGST_RATE;
        double sgst = subtotal * SGST_RATE;
        double total =subtotal + cgst + sgst;

        subField.setText(String.format("%.2f", subtotal));
        cgstField.setText(String.format("%.2f", cgst));
        sgstField.setText(String.format("%.2f", sgst));
        totField.setText(String.format("%.2f", total));
    }

}
