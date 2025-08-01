package org.example.billing_software.utils;

import java.awt.Color;
import java.awt.print.PrinterException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.awt.print.PrinterJob;

import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.example.billing_software.models.InvoiceData;

public class PdfGenerator {
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");
    private static final String LOGO_PATH         = "/img/logo.png";
    private static final String COMPANY_GST_NO    = "29ABCDE1234F2Z5";
    private static final String BANK_NAME         = "HDFC BANK";
    private static final String BANK_ACC          = "50200045669341";
    private static final String BANK_BRANCH       = "HDFC0003789";
    private static final String SIGNATURE_TEXT    = "Authorized Signatory";
    private static final double CGST_RATE         = 0.09;
    private static final double SGST_RATE         = 0.09;

    public byte[] generatePdfDocument(InvoiceData data) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDRectangle media = page.getMediaBox();
            float w = media.getWidth();
            float h = media.getHeight();

            try (PDPageContentStream cs = new PDPageContentStream(document, page, AppendMode.OVERWRITE, true)) {
                // --- logo ---
                try (InputStream is = getClass().getResourceAsStream(LOGO_PATH)) {
                    if (is != null) {
                        byte[] imgBytes = is.readAllBytes();
                        PDImageXObject logo = PDImageXObject.createFromByteArray(document, imgBytes, "logo");
                        cs.drawImage(logo, 20, h - 70, w / 2.8f, 50);
                    }
                }

                // --- company address ---
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(20, h - 90);
                cs.showText("GF-10/11, Kasper Square, Opp. Gangotri Exotica,");
                cs.newLineAtOffset(0, -12);
                cs.showText("30 mtr Gotri-Laxmipura Road, Vadodara - 390021");
                cs.newLineAtOffset(0, -12);
                cs.showText("M:+91 99789 15902   Email: autocraft.gj06@gmail.com");
                cs.endText();

                // --- header (Bill No & Date) ---
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                float hdrX = w * 6 / 8;
                float y    = h - 90;
                cs.newLineAtOffset(hdrX, y);
                cs.showText("Bill No: " + data.invoiceNo);
                cs.newLineAtOffset(0, -14);
                cs.showText("Date: " + data.date);
                cs.endText();

                // --- client & car info ---
                y = h - 150;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(20, y);
                cs.showText("Client: " + data.client);
                cs.newLineAtOffset(w * 3 / 8 - 20, 0);
                cs.showText("Car Model: " + data.carMake + " " + data.carModel);
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(20, y - 16);
                cs.showText("GST No: " + (data.gst.isEmpty() ? "Not Applicable" : data.gst));
                cs.newLineAtOffset(w * 3 / 8 - 20, 0);
                cs.showText("License No: " + data.carLicense);
                cs.endText();

                // --- ITEMS TABLE WITH BORDERS ---
                y = h - 200;
                float[] colX = {
                        20,
                        w * 0.09f,   // particulars start
                        w * 0.40f,   // qty
                        w * 0.45f,   // gst
                        w * 0.50f,   // rate incl. tax
                        w * 0.66f,   // rate
                        w * 0.82f    // total
                };
                String[] head = {
                        "No.", "Particulars", "Qty", "GST", "Rate (incl. tax)", "Rate", "Total"
                };

                float tableX    = 20;
                float tableW    = w - 40;
                float headerH   = 20;
                float rowH      = 18;
                int   rowCount  = data.items.size() + 1;
                float tableTopY = y + headerH;
                float tableBotY = tableTopY - rowCount * rowH;

// 1) draw grid
                cs.setStrokingColor(Color.BLACK);
                cs.setLineWidth(0.5f);
// vertical lines
                for (float x : colX) {
                    cs.moveTo(x,       tableTopY);
                    cs.lineTo(x,       tableBotY);
                }
// rightmost border
                cs.moveTo(tableX + tableW, tableTopY);
                cs.lineTo(tableX + tableW, tableBotY);
                cs.stroke();

// 2) header background
                cs.setNonStrokingColor(new Color(200,200,255));
                cs.addRect(tableX, tableTopY - headerH, tableW, headerH);
                cs.fill();

// 3) header text
                cs.setNonStrokingColor(Color.BLACK);
                for (int i = 0; i < head.length; i++) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                    cs.newLineAtOffset(colX[i], tableTopY - headerH + 6);
                    cs.showText(head[i]);
                    cs.endText();
                }

// 4) data rows
                for (int r = 0; r < data.items.size(); r++) {
                    float rowY = tableTopY - headerH - (r+1) * rowH + 6;
                    var item = data.items.get(r);

                    // compute per‑item GST
                    double gstAmt = item.rate.get() * item.quantity.get() * (CGST_RATE + SGST_RATE);

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);

                    // 1) No.
                    cs.newLineAtOffset(colX[0]+ 2, rowY);
                    cs.showText(String.valueOf(r + 1));

                    // 2) Particulars
                    cs.newLineAtOffset(colX[1] - colX[0]+ 2, 0);
                    cs.showText(item.particulars.get());

                    // 3) Qty
                    cs.newLineAtOffset(colX[2] - colX[1]+ 2, 0);
                    cs.showText(String.valueOf(item.quantity.get()));

                    // 4) GST
                    cs.newLineAtOffset(colX[3] - colX[2]+ 2, 0);
                    cs.showText("18%");

                    // 5) Rate (incl. tax)
                    cs.newLineAtOffset(colX[4] - colX[3]+ 2, 0);
                    cs.showText(String.format("%.2f", item.rate.get() * (1 + CGST_RATE + SGST_RATE)));

                    // 6) Rate
                    cs.newLineAtOffset(colX[5] - colX[4]+ 2, 0);
                    cs.showText(String.format("%.2f", item.rate.get()));

                    // 7) Total
                    cs.newLineAtOffset(colX[6] - colX[5]+ 2, 0);
                    cs.showText(String.format("%.2f", item.amount.get()));

                    cs.endText();
                }
                // horizontal lines
                    float hl = tableTopY - headerH - (data.items.size()) * rowH ;
                    cs.moveTo(tableX, hl);
                    cs.lineTo(tableX + tableW, hl);
                    cs.stroke();



                // --- TOTALS TABLE WITH BORDERS ---
                float tx0    = colX[5]+60;
                float tx1    = colX[6]+90;
                float tw     = tx1 - tx0;
                float ty0    = 177;
                String[] labels = {"Subtotal:", "CGST:", "SGST:", "Total:"};
                double[] vals   = {data.subtotal, data.cgst, data.sgst, data.total};
                int tRows      = labels.length;


                // draw labels & values
                for (int i = 0; i < tRows; i++) {
                    float yy = ty0 - i * 14 + 4; // vertical centering
                    String lbl = labels[i];
                    String txt = String.format("%.2f", vals[i]);
                    float textW = PDType1Font.HELVETICA_BOLD.getStringWidth(txt) / 1000 * 12;

                    // label (left cell)
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(tx0 + 2, yy);
                    cs.showText(lbl);
                    cs.endText();

                    // value (right cell, right-aligned)
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.newLineAtOffset(tx0 + tw - textW - 2, yy);
                    cs.showText(txt);
                    cs.endText();
                }

                // --- rupees in words ---
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(20, 154);
                cs.showText("Amount Chargeable (in words) ");
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);

                cs.newLineAtOffset(0, -16);
                String words = NumberToWordsConverter.convert((long) data.total) + " INR Only";
                cs.showText(words);
                cs.endText();

                // --- footer ---
                float footerY = 110;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(20, footerY);
                cs.showText("Company GST No.: " + COMPANY_GST_NO);
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(20, footerY - 16);
                cs.showText("Bank Name: " + BANK_NAME);
                cs.newLineAtOffset(0, -14);
                cs.showText("Bank Account Number: " + BANK_ACC);
                cs.newLineAtOffset(0, -14);
                cs.showText("Bank Branch IFSC: " + BANK_BRANCH);
                cs.newLineAtOffset(0, -34);
                cs.showText("E. & O.E.");
                cs.newLineAtOffset(0, -14);
                cs.showText("Subject to Vadodara Jurisdiction.");
                cs.endText();

                // --- signature ---
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                float sigWidth = PDType1Font.HELVETICA.getStringWidth(SIGNATURE_TEXT) / 1000 * 12;
                cs.newLineAtOffset(w - sigWidth - 20, footerY - 90);
                cs.showText(SIGNATURE_TEXT);
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    public void printPdf(InvoiceData data) throws IOException, PrinterException {
        byte[] pdf = generatePdfDocument(data);
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf))) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(doc));
            if (job.printDialog())
                job.print();
        }
    }

    private static class NumberToWordsConverter {
        private static final String[] units  = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
        private static final String[] teens  = {"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        private static final String[] tens   = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

        public static String convert(long number) {
            if (number == 0) return "Zero";
            String words = "";
            if (number / 10000000 > 0) {
                words += convert(number / 10000000) + " Crore ";
                number %= 10000000;
            }
            if (number / 100000 > 0) {
                words += convert(number / 100000) + " Lakh ";
                number %= 100000;
            }
            if (number / 1000 > 0) {
                words += convert(number / 1000) + " Thousand ";
                number %= 1000;
            }
            if (number / 100 > 0) {
                words += convert(number / 100) + " Hundred ";
                number %= 100;
            }
            if (number > 0) {
                if (!words.isEmpty()) words += "and ";
                if (number < 10) words += units[(int) number];
                else if (number < 20) words += teens[(int) number - 10];
                else {
                    words += tens[(int) (number / 10)];
                    if (number % 10 > 0) words += " " + units[(int) (number % 10)];
                }
            }
            return words.trim();
        }
    }
}
