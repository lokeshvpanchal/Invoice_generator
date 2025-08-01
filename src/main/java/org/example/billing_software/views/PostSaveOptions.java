package org.example.billing_software.views;

import javafx.scene.control.*;
import org.example.billing_software.models.InvoiceData;
import org.example.billing_software.services.EmailSender;
import org.example.billing_software.services.InvoicePrinter;
import org.example.billing_software.utils.PdfGenerator;

import java.util.Optional;

public class PostSaveOptions {

    public static void show(InvoiceData data) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Next Action");
        alert.setHeaderText("Invoice saved successfully.");
        alert.setContentText("What would you like to do next?");

        ButtonType printBtn = new ButtonType("Print Invoice");
        ButtonType emailBtn = new ButtonType("Email Invoice");
        ButtonType bothBtn = new ButtonType("Print and Email");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(printBtn, emailBtn, bothBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            try {
                if (result.get() == printBtn || result.get() == bothBtn) {
                    InvoicePrinter.printInvoice(data);
                }

                if (result.get() == emailBtn || result.get() == bothBtn) {
                    byte[] pdfBytes = new PdfGenerator().generatePdfDocument(data);
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Send Invoice");
                    dialog.setHeaderText("Enter the recipient's email address:");
                    dialog.setContentText("To:");

                    dialog.showAndWait().ifPresent(to -> {
                        try {
                            String subject = "Invoice from Autocraft - " + data.invoiceNo;
                            String body = "Dear " + data.client + ",\n\nPlease find attached your invoice.\n\nRegards,\nAutocraft";
                            EmailSender.sendEmailWithAttachmentBytes(to, subject, body, pdfBytes, "Invoice-" + data.invoiceNo + ".pdf");
                            new Alert(Alert.AlertType.INFORMATION, "Invoice sent successfully to " + to).showAndWait();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            new Alert(Alert.AlertType.ERROR, "Failed to send email.").showAndWait();
                        }
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Action failed.").showAndWait();
            }
        }
    }
}
