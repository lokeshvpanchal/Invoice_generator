package org.example.billing_software.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * InvoicePrinter uses PdfGenerator to render the invoice into a PDF
 * and then sends it to the printer as a vector PDF for high-quality output.
 */
public class InvoicePrinter {

    /**
     * Print the invoice PDF to a physical printer via a print dialog.
     * @param data the invoice data
     * @throws PrinterException if the print job fails
     * @throws IOException if PDF generation or loading fails
     */
    public static void printInvoice(InvoiceData data) throws PrinterException, IOException {
        // Generate PDF bytes in memory
        PdfGenerator generator = new PdfGenerator();
        byte[] pdfBytes = generator.generatePdfDocument(data);

        // Load into PDFBox PDDocument
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            // Hook up to Java PrinterJob using PDFPageable for vector output
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(document));

            // Show print dialog and print if confirmed
            if (job.printDialog()) {
                job.print();
            }
        }
    }
}
