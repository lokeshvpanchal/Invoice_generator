package org.example.billing_software.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.example.billing_software.CreateInvoiceForm.LineItem;

public class InvoicePdfUtil {

    /**
     * Builds a PDF document in memory.
     */
    private static PDDocument buildDocument(
            String invoiceNo,
            String client,
            String gst,
            String date,
            List<LineItem> items,
            boolean taxIncluded,
            String carMake,
            String carModel,
            double subtotal,
            double cgst,
            double sgst,
            double total
    ) throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.LETTER);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // --- Header ---
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
            cs.newLineAtOffset(50, 750);
            cs.showText("INVOICE");
            cs.endText();

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
            cs.newLineAtOffset(400, 750);
            cs.showText("Invoice #: " + invoiceNo);
            cs.newLineAtOffset(0, -15);
            cs.showText("Date: " + date);
            cs.endText();

            // --- Client & Vehicle Info ---
            float y = 720;
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, y);
            cs.showText("Bill To: " + client);
            if (gst != null && !gst.isBlank()) {
                cs.newLineAtOffset(0, -15);
                cs.showText("GST No: " + gst);
            }
            cs.newLineAtOffset(0, -15);
            cs.showText("Car: " + carMake + " • " + carModel);
            cs.endText();

            // --- Table Header ---
            y -= 60;
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
            cs.newLineAtOffset(50, y);
            cs.showText("Particulars");
            cs.newLineAtOffset(200, 0);
            cs.showText("Qty");
            cs.newLineAtOffset(50, 0);
            cs.showText("Rate");
            cs.newLineAtOffset(50, 0);
            cs.showText("Amount");
            cs.endText();

            // --- Table Rows ---
            cs.setFont(PDType1Font.HELVETICA, 12);
            y -= 20;
            for (LineItem item : items) {
                double rate = item.quantity.get() != 0
                        ? item.amount.get() / item.quantity.get()
                        : 0.0;
                cs.beginText();
                cs.newLineAtOffset(50, y);
                cs.showText(item.particulars.get());
                cs.newLineAtOffset(200, 0);
                cs.showText(String.valueOf(item.quantity.get()));
                cs.newLineAtOffset(50, 0);
                cs.showText(String.format("%.2f", rate));
                cs.newLineAtOffset(50, 0);
                cs.showText(String.format("%.2f", item.amount.get()));
                cs.endText();
                y -= 20;
            }

            // --- Totals ---
            y -= 30;
            cs.beginText();
            cs.newLineAtOffset(350, y);
            cs.showText(String.format("Subtotal: %.2f", subtotal));
            cs.newLineAtOffset(0, -15);
            cs.showText(String.format("CGST: %.2f", cgst));
            cs.newLineAtOffset(0, -15);
            cs.showText(String.format("SGST: %.2f", sgst));
            cs.newLineAtOffset(0, -15);
            cs.showText(String.format("Total: %.2f", total));
            cs.endText();
        }
        return doc;
    }

    /**
     * Prints the invoice via the system's default PDF viewer print dialog (Ctrl+P).
     */
    public static void printWithSystemDialog(
            String invoiceNo,
            String client,
            String gst,
            String date,
            List<LineItem> items,
            boolean taxIncluded,
            String carMake,
            String carModel,
            double subtotal,
            double cgst,
            double sgst,
            double total
    ) throws IOException {
        // 1) build the PDF
        PDDocument doc = buildDocument(
                invoiceNo, client, gst, date,
                items, taxIncluded,
                carMake, carModel,
                subtotal, cgst, sgst, total
        );

        // 2) save to a temp file
        File tempFile = File.createTempFile("invoice-" + invoiceNo + "-", ".pdf");
        doc.save(tempFile);
        doc.close();

        // 3) hand off to the OS's default PDF handler print dialog
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.PRINT)) {
            desktop.print(tempFile);
        } else {
            throw new UnsupportedOperationException(
                    "Printing is not supported on this platform"
            );
        }
    }
}
