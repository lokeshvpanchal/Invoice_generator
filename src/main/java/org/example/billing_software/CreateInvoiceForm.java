package org.example.billing_software;

import javafx.beans.property.*;
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

import java.sql.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.UnaryOperator;

/**
 * Modular form for creating invoices with dynamic input rows (Particulars, Quantity, Amount),
 * with Subtotal, CGST, SGST, Total calculations, Car Make/Model inputs,
 * input filters for digits, and database persistence.
 */
public class CreateInvoiceForm {
    private static final double CGST_RATE = 0.09;
    private static final double SGST_RATE = 0.09;

    /**
     * Entry point for the invoice form. Ensures schema exists before operations.
     */
    public static Node create(Connection conn) {
        // Ensure tables are present
        try {
            createSchema(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR, "Database initialization failed: " + e.getMessage());
            error.showAndWait();
            return new Label("Error initializing database");
        }

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        // Header section
        GridPane header = new GridPane();
        header.setHgap(10);
        header.setVgap(10);

        Label billLbl = new Label("Bill No:");
        TextField billField = new TextField(String.valueOf(fetchNextBill(conn)));
        billField.setDisable(true);

        Label nameLbl = new Label("Client Name:");
        TextField nameField = new TextField();

        Label gstLbl = new Label("GST No (optional):");
        TextField gstField = new TextField();
        gstField.setPromptText("Optional");

        Label dateLbl = new Label("Date:");
        DatePicker datePicker = new DatePicker(LocalDate.now());

        Label taxLbl = new Label("Tax Included:");
        CheckBox taxCheck = new CheckBox();

        Label carMakeLbl = new Label("Car Make:");
        TextField carMakeField = new TextField();
        Label carModelLbl = new Label("Car Model:");
        TextField carModelField = new TextField();

        header.add(billLbl, 0, 0);
        header.add(billField, 1, 0);
        header.add(nameLbl, 0, 1);
        header.add(nameField, 1, 1);
        header.add(gstLbl, 0, 2);
        header.add(gstField, 1, 2);
        header.add(dateLbl, 0, 3);
        header.add(datePicker, 1, 3);
        header.add(taxLbl, 0, 4);
        header.add(taxCheck, 1, 4);
        header.add(carMakeLbl, 0, 5);
        header.add(carMakeField, 1, 5);
        header.add(carModelLbl, 0, 6);
        header.add(carModelField, 1, 6);

        // Line items container
        VBox itemsBox = new VBox(5);
        itemsBox.setPadding(new Insets(10, 0, 10, 0));
        ObservableList<LineItem> items = FXCollections.observableArrayList();

        // Items header labels
        HBox itemsHeader = new HBox(10,
                new Label("Particulars"),
                new Label("Quantity"),
                new Label("Amount")
        );
        itemsBox.getChildren().add(itemsHeader);

        // Totals section
        GridPane totals = new GridPane();
        totals.setHgap(10);
        totals.setVgap(10);
        TextField subField  = makeReadonlyField();
        TextField cgstField = makeReadonlyField();
        TextField sgstField = makeReadonlyField();
        TextField totField  = makeReadonlyField();
        totals.add(new Label("Subtotal:"), 0, 0);
        totals.add(subField,  1, 0);
        totals.add(new Label("CGST:"),    0, 1);
        totals.add(cgstField, 1, 1);
        totals.add(new Label("SGST:"),    0, 2);
        totals.add(sgstField, 1, 2);
        totals.add(new Label("Total:"),   0, 3);
        totals.add(totField,  1, 3);

        // Create initial item row
        Runnable addRow = new Runnable() {
            @Override
            public void run() {
                LineItem item = new LineItem();
                items.add(item);
                HBox row = createRow(item, this, items, taxCheck, subField, cgstField, sgstField, totField);
                itemsBox.getChildren().add(row);
                updateTotals(items, taxCheck.isSelected(), subField, cgstField, sgstField, totField);
            }
        };
        addRow.run();
        taxCheck.setOnAction(e -> updateTotals(items, taxCheck.isSelected(), subField, cgstField, sgstField, totField));

        // Save button
        Button saveBtn = new Button("Save Invoice");
        saveBtn.setOnAction(e -> {
            updateTotals(items, taxCheck.isSelected(), subField, cgstField, sgstField, totField);
            saveInvoice(conn,
                    billField.getText(),
                    nameField.getText(),
                    gstField.getText(),
                    datePicker.getValue().toString(),
                    items,
                    taxCheck.isSelected(),
                    carMakeField.getText(),
                    carModelField.getText()
            );
            // Refresh bill number
            billField.setText(String.valueOf(fetchNextBill(conn)));
        });

        root.getChildren().addAll(header, itemsBox, totals, saveBtn);
        return root;
    }

    /**
     * Ensures the required tables exist in the SQLite database.
     */
    private static void createSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("CREATE TABLE IF NOT EXISTS invoices(" +
                    "invoice_no TEXT PRIMARY KEY, " +
                    "client TEXT, gst TEXT, date TEXT, " +
                    "car_make TEXT, car_model TEXT, " +
                    "cgst REAL, sgst REAL, total REAL)");
            st.execute("CREATE TABLE IF NOT EXISTS invoice_items(" +
                    "invoice_no TEXT, particulars TEXT, quantity INTEGER, amount REAL, rate REAL, " +
                    "FOREIGN KEY(invoice_no) REFERENCES invoices(invoice_no))");
        }
    }

    private static TextField makeReadonlyField() {
        TextField tf = new TextField();
        tf.setEditable(false);
        return tf;
    }

    private static HBox createRow(LineItem item,
                                  Runnable addRow,
                                  ObservableList<LineItem> items,
                                  CheckBox taxCheck,
                                  TextField subField,
                                  TextField cgstField,
                                  TextField sgstField,
                                  TextField totField) {
        HBox row = new HBox(10);
        UnaryOperator<Change> intFilter = change -> change.getControlNewText().matches("\\d*") ? change : null;
        UnaryOperator<Change> decimalFilter = change -> change.getControlNewText().matches("\\d*(\\\\.\\d*)?") ? change : null;

        TextField partField = new TextField();
        partField.setPromptText("Particulars");
        partField.textProperty().addListener((o,oldV,newV) -> item.particulars.set(newV));

        TextField qtyField = new TextField("1");
        qtyField.setPrefWidth(60);
        qtyField.setTextFormatter(new TextFormatter<>(intFilter));
        qtyField.textProperty().addListener((o,oldV,newV) -> {
            item.quantity.set(newV.isEmpty() ? 0 : Integer.parseInt(newV));
            updateTotals(items, taxCheck.isSelected(), subField, cgstField, sgstField, totField);
        });

        TextField amtField = new TextField("0.00");
        amtField.setPrefWidth(80);
        amtField.setTextFormatter(new TextFormatter<>(decimalFilter));
        amtField.textProperty().addListener((o,oldV,newV) -> {
            item.amount.set(newV.isEmpty() ? 0.0 : Double.parseDouble(newV));
            updateTotals(items, taxCheck.isSelected(), subField, cgstField, sgstField, totField);
        });

        amtField.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ENTER) {
                addRow.run();
                evt.consume();
            }
        });

        row.getChildren().addAll(partField, qtyField, amtField);
        return row;
    }

    private static void updateTotals(ObservableList<LineItem> items,
                                     boolean taxIncluded,
                                     TextField subField,
                                     TextField cgstField,
                                     TextField sgstField,
                                     TextField totField) {
        double[] amounts = items.stream().mapToDouble(i -> i.amount.get()).toArray();
        double sum = Arrays.stream(amounts).sum();
        double subtotal = taxIncluded ? sum / (1 + CGST_RATE + SGST_RATE) : sum;
        double cgst = subtotal * CGST_RATE;
        double sgst = subtotal * SGST_RATE;
        double total = taxIncluded ? sum : subtotal + cgst + sgst;

        subField.setText(String.format("%.2f", subtotal));
        cgstField.setText(String.format("%.2f", cgst));
        sgstField.setText(String.format("%.2f", sgst));
        totField.setText(String.format("%.2f", total));
    }

    private static int fetchNextBill(Connection conn) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(CAST(invoice_no AS INTEGER)), 0) + 1 FROM invoices")) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return 1;
        }
    }

    private static void saveInvoice(Connection conn,
                                    String invoiceNo,
                                    String client,
                                    String gst,
                                    String date,
                                    ObservableList<LineItem> items,
                                    boolean taxIncluded,
                                    String carMake,
                                    String carModel) {
        try {
            conn.setAutoCommit(false);
            String insertInvoice = "INSERT INTO invoices(invoice_no, client, gst, date, car_make, car_model, cgst, sgst, total) VALUES(?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement psInv = conn.prepareStatement(insertInvoice)) {
                double[] amounts = items.stream().mapToDouble(i -> i.amount.get()).toArray();
                double sum = Arrays.stream(amounts).sum();
                double subtotal = taxIncluded ? sum / (1 + CGST_RATE + SGST_RATE) : sum;
                double cgst = subtotal * CGST_RATE;
                double sgst = subtotal * SGST_RATE;
                double total = taxIncluded ? sum : subtotal + cgst + sgst;

                psInv.setString(1, invoiceNo);
                psInv.setString(2, client);
                psInv.setString(3, gst);
                psInv.setString(4, date);
                psInv.setString(5, carMake);
                psInv.setString(6, carModel);
                psInv.setDouble(7, cgst);
                psInv.setDouble(8, sgst);
                psInv.setDouble(9, total);
                psInv.executeUpdate();
            }

            String insertItem = "INSERT INTO invoice_items(invoice_no, particulars, quantity, amount, rate) VALUES(?,?,?,?,?)";
            try (PreparedStatement psItem = conn.prepareStatement(insertItem)) {
                for (LineItem item : items) {
                    int qty = item.quantity.get();
                    double amt = item.amount.get();
                    double rate = qty != 0 ? amt / qty : 0;

                    psItem.setString(1, invoiceNo);
                    psItem.setString(2, item.particulars.get());
                    psItem.setInt(3, qty);
                    psItem.setDouble(4, amt);
                    psItem.setDouble(5, rate);
                    psItem.executeUpdate();
                }
            }

            conn.commit();
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Saved invoice " + invoiceNo);
            ok.showAndWait();
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException e) { e.printStackTrace(); }
            ex.printStackTrace();
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /** Data model for a line item */
    public static class LineItem {
        final StringProperty particulars = new SimpleStringProperty();
        final IntegerProperty quantity    = new SimpleIntegerProperty(1);
        final DoubleProperty  amount      = new SimpleDoubleProperty(0.0);
    }
}
