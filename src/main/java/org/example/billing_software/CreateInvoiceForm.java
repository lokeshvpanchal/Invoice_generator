package org.example.billing_software;
import javafx.application.Platform;
import org.example.billing_software.utils.EmailUtil;
import org.example.billing_software.utils.InvoiceData;

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
import org.example.billing_software.utils.InvoicePrinter;
import org.example.billing_software.utils.PdfGenerator;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.UnaryOperator;

public class CreateInvoiceForm {
    private static final double CGST_RATE = 0.09;
    private static final double SGST_RATE = 0.09;

    public static Node create(Connection conn) {
        try {
            createSchema(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR, "Database init failed: " + e.getMessage());
            error.showAndWait();
            return new Label("DB Error");
        }

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        // Header
        GridPane header = new GridPane();
        header.setHgap(10);
        header.setVgap(10);
        TextField billField = new TextField(String.valueOf(fetchNextBill(conn)));
        billField.setDisable(true);
        header.addRow(0,
                new Label("Bill No:"), billField
        );
        TextField nameField   = new TextField();
        TextField gstField    = new TextField(); gstField.setPromptText("Optional");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        CheckBox taxCheck     = new CheckBox();
        TextField carMakeField  = new TextField();
        TextField carModelField = new TextField();
        TextField carLicenseNo = new TextField();
        header.addRow(1, new Label("Client Name:"), nameField);
        header.addRow(2, new Label("GST No (opt):"), gstField);
        header.addRow(3, new Label("Date:"), datePicker);
        header.addRow(4, new Label("Car Make:"), carMakeField);
        header.addRow(5, new Label("Car Model:"), carModelField);
        header.addRow(5, new Label("Car License No. :"), carLicenseNo);
        header.addRow(6, new Label("Tax Included:"), taxCheck);

        // Items
        VBox itemsBox = new VBox(5);
        itemsBox.setPadding(new Insets(10, 0, 10, 0));
        ObservableList<LineItem> items = FXCollections.observableArrayList();

        // 1) In your `create(...)`, replace the header HBox with this:
        Label hdrPart   = new Label("Particulars");
        Label hdrQty    = new Label("Quantity"); hdrQty.setPrefWidth(60);
        Label hdrAmt    = new Label("Amount");   hdrAmt.setPrefWidth(80);
        Label hdrAction = new Label("Action");   hdrAction.setPrefWidth(80);

        HBox itemsHeader = new HBox(10, hdrPart, hdrQty, hdrAmt, hdrAction);
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
        totals.addRow(1, new Label("CGST:"),     cgstField);
        totals.addRow(2, new Label("SGST:"),     sgstField);
        totals.addRow(3, new Label("Total:"),    totField);

        // Row adder
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

        // Buttons
        Button saveBtn  = new Button("Save Invoice");
        Button printBtn = new Button("Print Invoice");
        Button emailBtn = new Button("Email Invoice");
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
                    carModelField.getText(),
                    carLicenseNo.getText()
            );
            billField.setText(String.valueOf(fetchNextBill(conn)));
        });

        printBtn.setOnAction(e -> {
            updateTotals(items, taxCheck.isSelected(), subField, cgstField, sgstField, totField);

            String invNo      = billField.getText();
            String clientName = nameField.getText();
            String gstNo      = gstField.getText();
            String date       = datePicker.getValue().toString();
            boolean taxIncl   = taxCheck.isSelected();
            String make       = carMakeField.getText();
            String model      = carModelField.getText();
            String license   =  carLicenseNo.getText();
            //grand totals
            double subtotalVal = Double.parseDouble(subField.getText());
            double cgstVal     = Double.parseDouble(cgstField.getText());
            double sgstVal     = Double.parseDouble(sgstField.getText());
            double totalVal    = Double.parseDouble(totField.getText());

            // build net-amount item list
            List<LineItem> printItems = new ArrayList<>();
            for (LineItem item : items) {
                LineItem pi = new LineItem();
                pi.particulars.set(item.particulars.get());

                int qty = item.quantity.get();
                pi.quantity.set(qty);

                double inputRate = item.rate.get();
                // net rate = strip tax if original was tax‑inclusive
                double netRate = taxIncl
                        ? inputRate / (1 + CGST_RATE + SGST_RATE)
                        : inputRate;
                pi.rate.set(netRate);

                // amount = rate × quantity
                double netAmt = netRate * qty;
                pi.amount.set(netAmt);

                printItems.add(pi);
            }

            // create InvoiceData and print
            InvoiceData data = new InvoiceData(
                    invNo, clientName, gstNo, date,
                    printItems, taxIncl,
                    make, model, license,
                    subtotalVal, cgstVal, sgstVal, totalVal
            );
            try {
                InvoicePrinter.printInvoice(data);
            } catch (PrinterException ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Print failed: " + ex.getMessage()).showAndWait();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        emailBtn.setOnAction(e -> {
            // 1) recalc totals
            updateTotals(items, taxCheck.isSelected(), subField, cgstField, sgstField, totField);

            // 2) prompt for recipient email
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Send Invoice");
            dialog.setHeaderText("Email Invoice PDF");
            dialog.setContentText("Enter recipient email:");
            Optional<String> emailResult = dialog.showAndWait();
            if (emailResult.isEmpty()) return; // cancelled

            String recipientEmail = emailResult.get().trim();
            if (recipientEmail.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "No email entered.").showAndWait();
                return;
            }

            // 3) gather invoice & customer info
            String invNo      = billField.getText();
            String clientName = nameField.getText();
            String gstNo      = gstField.getText();
            String date       = datePicker.getValue().toString();
            boolean taxIncl   = taxCheck.isSelected();
            String make       = carMakeField.getText();
            String model      = carModelField.getText();
            String licenseNo = carLicenseNo.getText();

            // 4) build net-amount line items
            // 4) build net-amount line items with rate preserved
            List<LineItem> emailItems = new ArrayList<>();
            for (LineItem item : items) {
                LineItem ei = new LineItem();
                ei.particulars.set(item.particulars.get());
                ei.quantity  .set(item.quantity.get());

                double inputRate = item.rate.get();
                double netRate   = taxIncl
                        ? inputRate  / (1 + CGST_RATE + SGST_RATE)
                        : inputRate;
                ei.rate.set(netRate);

                double rawAmt = item.amount.get();
                double netAmt = taxIncl
                        ? rawAmt      / (1 + CGST_RATE + SGST_RATE)
                        : rawAmt;
                ei.amount.set(netAmt);

                emailItems.add(ei);
            }


            InvoiceData data = new InvoiceData(
                    invNo, clientName, gstNo, date,
                    emailItems, taxIncl,
                    make, model, licenseNo,
                    Double.parseDouble(subField.getText()),
                    Double.parseDouble(cgstField.getText()),
                    Double.parseDouble(sgstField.getText()),
                    Double.parseDouble(totField.getText())
            );

            try {
                PdfGenerator gen = new PdfGenerator();
                byte[] pdfBytes = gen.generatePdfDocument(data);

                EmailUtil.sendEmailWithAttachmentBytes(
                        recipientEmail,
                        "Invoice #" + invNo + " PDF",
                        "Hello " + clientName + ",\n\nPlease find attached your invoice PDF.",
                        pdfBytes,
                        "invoice-" + invNo + ".pdf"
                );

                new Alert(Alert.AlertType.INFORMATION,
                        "Invoice PDF emailed to " + recipientEmail + "!").showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR,
                        "Failed to generate or send PDF: " + ex.getMessage())
                        .showAndWait();
            }
        });


        HBox actions = new HBox(10, saveBtn, printBtn, emailBtn);

        root.getChildren().addAll(header, itemsBox, totals, actions);
        return root;
    }

    private static void createSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("""
                CREATE TABLE IF NOT EXISTS invoices(
                  invoice_no TEXT PRIMARY KEY,
                  client TEXT, gst TEXT, date TEXT,
                  car_make TEXT, car_model TEXT, license_no TEXT,
                  cgst REAL, sgst REAL, total REAL
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS invoice_items(
                  invoice_no TEXT,
                  particulars TEXT,
                  quantity INTEGER,
                  amount REAL,
                  rate REAL,
                  FOREIGN KEY(invoice_no) REFERENCES invoices(invoice_no)
                )""");
        }
    }

    private static TextField makeReadonlyField() {
        TextField tf = new TextField();
        tf.setEditable(false);
        return tf;
    }

    private static HBox createRow(
            LineItem item,
            Runnable addRow,
            ObservableList<LineItem> items,
            CheckBox taxCheck,
            TextField subField,
            TextField cgstField,
            TextField sgstField,
            TextField totField
    ) {
        HBox row = new HBox(10);

        // filters for numeric input
        UnaryOperator<Change> intFilter = c ->
                c.getControlNewText().matches("\\d*") ? c : null;
        UnaryOperator<Change> decFilter = c ->
                c.getControlNewText().matches("\\d*(\\.\\d*)?") ? c : null;

        // particulars
        TextField partField = new TextField();
        partField.setPromptText("Particulars");
        partField.textProperty().addListener((obs, oldV, newV) ->
                item.particulars.set(newV)
        );

        // quantity
        TextField qtyField = new TextField("1");
        qtyField.setPrefWidth(60);
        qtyField.setTextFormatter(new TextFormatter<>(intFilter));

        // rate
        TextField rateField = new TextField("0.00");
        rateField.setPrefWidth(80);
        rateField.setTextFormatter(new TextFormatter<>(decFilter));

        // amount (read‑only, auto)
        TextField amtField = new TextField("0.00");
        amtField.setPrefWidth(80);
        amtField.setEditable(false);

        // when quantity changes, recalc amount
        qtyField.textProperty().addListener((obs, oldV, newV) -> {
            int q = newV.isEmpty() ? 0 : Integer.parseInt(newV);
            item.quantity.set(q);
            double r = item.rate.get();
            amtField.setText(String.format("%.2f", r * q));
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
            updateTotals(items, taxCheck.isSelected(), subField, cgstField, sgstField, totField);
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

        // delete button
        Button delBtn = new Button("Delete");
        delBtn.setOnAction(evt -> {
            items.remove(item);
            ((VBox) row.getParent()).getChildren().remove(row);
            updateTotals(items, taxCheck.isSelected(), subField, cgstField, sgstField, totField);
        });

        row.getChildren().addAll(partField, qtyField, rateField, amtField, delBtn);
        return row;
    }


    private static void updateTotals(ObservableList<LineItem> items,
                                     boolean taxIncluded,
                                     TextField subField,
                                     TextField cgstField,
                                     TextField sgstField,
                                     TextField totField) {
        double sum = items.stream().mapToDouble(i -> i.amount.get()).sum();
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
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(CAST(invoice_no AS INTEGER)),0)+1 FROM invoices")) {
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
                                    String carModel,
                                    String carLicense) {
        try {
            conn.setAutoCommit(false);
            String sqlInv = """
                INSERT INTO invoices(invoice_no, client, gst, date, car_make, car_model, license_no, cgst, sgst, total)
                VALUES(?,?,?,?,?,?,?,?,?,?)""";
            try (PreparedStatement ps = conn.prepareStatement(sqlInv)) {
                double sum = items.stream().mapToDouble(i -> i.amount.get()).sum();
                double subtotal = taxIncluded ? sum / (1 + CGST_RATE + SGST_RATE) : sum;
                double cgst = subtotal * CGST_RATE;
                double sgst = subtotal * SGST_RATE;
                double total = taxIncluded ? sum : subtotal + cgst + sgst;

                ps.setString(1, invoiceNo);
                ps.setString(2, client);
                ps.setString(3, gst);
                ps.setString(4, date);
                ps.setString(5, carMake);
                ps.setString(6, carModel);
                ps.setString(7, carLicense);
                ps.setDouble(8, cgst);
                ps.setDouble(9, sgst);
                ps.setDouble(10, total);
                ps.executeUpdate();
            }

            String sqlItem = """
            INSERT INTO invoice_items(invoice_no, particulars, quantity, amount, rate)
            VALUES(?,?,?,?,?)""";
            try (PreparedStatement ps = conn.prepareStatement(sqlItem)) {
                for (LineItem item : items) {
                    int qty        = item.quantity.get();
                    double inputAmt  = item.amount.get();
                    double inputRate = qty != 0 ? inputAmt / qty : 0;

                    // strip tax out if needed
                    double netAmt  = taxIncluded
                            ? inputAmt  / (1 + CGST_RATE + SGST_RATE)
                            : inputAmt;
                    double netRate = qty > 0
                            ? netAmt / qty
                            : 0;

                    ps.setString(1, invoiceNo);
                    ps.setString(2, item.particulars.get());
                    ps.setInt   (3, qty);
                    ps.setDouble(4, netAmt);
                    ps.setDouble(5, netRate);
                    ps.executeUpdate();
                }
            }


            conn.commit();
            new Alert(Alert.AlertType.INFORMATION, "Saved invoice " + invoiceNo).showAndWait();
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException e) { e.printStackTrace(); }
            ex.printStackTrace();
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // Stub: export to PDF (use iText, PDFBox, or platform PDF printer)
    private static String exportInvoiceToPdf(Node node, String invoiceNo) throws Exception {
        // TODO: implement PDF generation and return path, e.g. "/tmp/invoice-" + invoiceNo + ".pdf"
        throw new UnsupportedOperationException("PDF export not implemented yet");
    }

    // Send email with JavaMail
    private static void sendEmailWithAttachment(String to, String subject, String body, String attachmentPath) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.yourhost.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("you@domain.com","yourPassword");
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("you@domain.com"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject);

        Multipart mp = new MimeMultipart();
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body);
        mp.addBodyPart(textPart);

        MimeBodyPart filePart = new MimeBodyPart();
        DataSource src = new FileDataSource(new File(attachmentPath));
        filePart.setDataHandler(new DataHandler(src));
        filePart.setFileName(src.getName());
        mp.addBodyPart(filePart);

        msg.setContent(mp);
        Transport.send(msg);
    }

    public static class LineItem {
        public final StringProperty  particulars = new SimpleStringProperty();
        public final IntegerProperty quantity    = new SimpleIntegerProperty(1);
        public final DoubleProperty  rate        = new SimpleDoubleProperty(0.0);   // ← new
        public final DoubleProperty  amount      = new SimpleDoubleProperty(0.0);
    }

}
