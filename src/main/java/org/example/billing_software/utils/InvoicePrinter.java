package org.example.billing_software.utils;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import static java.awt.print.Printable.NO_SUCH_PAGE;
import static java.awt.print.Printable.PAGE_EXISTS;


import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.example.billing_software.CreateInvoiceForm.LineItem;

public class InvoicePrinter {

    // Company-specific constants
    private static final String LOGO_PATH = "/img/logo.png";
    private static final String COMPANY_GST_NO = "29ABCDE1234F2Z5";
    private static final String BANK_NAME = "HDFC BANK";
    private static final String BANK_ACC = "50200045669341";
    private static final String BANK_BRANCH = "HDFC0003789";
    private static final String SIGNATURE_TEXT = "Authorized Signatory";
    private static final double CGST_RATE = 0.09;
    private static final double SGST_RATE = 0.09;
    /**
     * Prints a full-page invoice with logo, table, amounts, and footer details.
     */
    public static void printInvoice(
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
    ) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
                if (pageIndex > 0) return NO_SUCH_PAGE;
                Graphics2D g = (Graphics2D) graphics;
                // translate to imageable area
                g.translate(pf.getImageableX(), pf.getImageableY());
                double width = pf.getImageableWidth();
                double height = pf.getImageableHeight();

                int y = 20;
                int x = 20;
                // Draw logo
                try (InputStream is = InvoicePrinter.class.getResourceAsStream(LOGO_PATH)) {
                    if (is == null) {
                        System.out.println("logo resource not found on classpath: " + LOGO_PATH);
                    } else {
                        Image logo = ImageIO.read(is);
                        int logoHeight = 50;
                        g.drawImage(logo, x,  y, (int)(width/2.8), logoHeight, null);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                y += 70;
                g.setFont(new Font("Arial", Font.PLAIN, 10));
                g.drawString("GF-10/11, Kasper Square, Opp. Gangotri Exotica,", 20, y);
                y += 13;
                g.drawString("30 mtr Gotri-Laxmipura Road, Vadodara - 390021", 20, y);
                y += 13;
                g.drawString("M:+91 99789 15902   Email: autocraft.gj06@gmail.com", 20, y);


                // Header: client and bill no
                y = 86;
                int pos = (int)width*6/8;
                g.setFont(new Font("Arial", Font.PLAIN, 10));
                y += 13;
                g.drawString("Bill No:", (int) pos , y);
                String billLabel =  invoiceNo;
                g.drawString(billLabel, (int)width*7/8, y);
                y += 13;
                g.drawString("Date:", (int) pos , y);
                String dateLabel = date;
                g.drawString(dateLabel,  (int)width*7/8, y);
                // client details


                y += 50;
                pos = (int)width/6;
                g.setFont(new Font("Arial", Font.PLAIN, 12));

                g.drawString("Client:" , 20 , y);
                String clientLabel =  client;
                g.drawString(clientLabel, pos , y);

                g.drawString("Car Make:" , (int)width*4/6 , y);
                String makeLabel =  carMake;
                g.drawString(makeLabel, (int)width*5/6 , y);

                y += 13;
                g.drawString( "GST No.:" , 20 , y);
                String gstLabel = gst;
                if (gst == ""){
                    gstLabel = "Not Applicable";
                }
                g.drawString(gstLabel, pos, y);

                g.drawString("Car Model:" , (int)width*4/6 , y);
                String makeModel =  carModel;
                g.drawString(makeModel, (int)width*5/6 , y);

                // Table header (100% width)
                y += 30;
                int tableX = 20;
                int qtyX   = (int)(width * 0.5);
                int rateX  = (int)(width * 0.7);
                int amtX   = (int)(width * 0.85);
                g.setFont(new Font("Arial", Font.BOLD, 13));
                g.drawString("No.", tableX, y);
                g.drawString("Particulars", tableX +30, y);
                g.drawString("Qty", qtyX, y);
                g.drawString("Rate", rateX, y);
                g.drawString("Amount", amtX, y);

                // Table rows

// Table rows
                g.setFont(new Font("Arial", Font.PLAIN, 12));
                y += 20;
                int i = 1;
                for (LineItem item : items) {
                    double rawAmt = item.amount.get();

                    // ← NEW: if tax was included, get the net amount (remove 18%)
                    double displayAmt = taxIncluded
                            ? rawAmt / (1 + CGST_RATE + SGST_RATE)
                            : rawAmt;

                    // compute unit rate on the net amount
                    double rate = item.quantity.get() != 0
                            ? displayAmt / item.quantity.get()
                            : 0.0;

                    g.drawString(String.valueOf(i++),        tableX,      y);
                    g.drawString(item.particulars.get(),     tableX + 30, y);
                    g.drawString(String.valueOf(item.quantity.get()), qtyX, y);
                    g.drawString(String.format("%.2f", rate),           rateX, y);
                    g.drawString(String.format("%.2f", displayAmt),     amtX,  y);
                    y += 15;
                }

                // Totals block under Amount column
                y += 15;
                int labelX = (int)(width * 0.7);
                g.drawString("Subtotal:", labelX, y);
                g.drawString(String.format("%.2f", subtotal), amtX, y);
                y += 15;
                g.drawString("CGST:", labelX, y);
                g.drawString(String.format("%.2f", cgst), amtX, y);
                y += 15;
                g.drawString("SGST:", labelX, y);
                g.drawString(String.format("%.2f", sgst), amtX, y);
                g.setFont(new Font("Arial", Font.BOLD, 12));
                g.drawString("Rupees in Words:", tableX, y);
                y += 15;
                g.drawString("Total:", labelX, y);
                g.drawString(String.format("%.2f", total), amtX, y);

                // Rupees in words (left side)
                g.setFont(new Font("Arial", Font.PLAIN, 12));
                String words = "   "+NumberToWordsConverter.convert((long) total) + " Rupees Only";
                g.drawString(words, tableX, y);

                // Footer: company GST, bank details
                y = (int) height - 120;
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("Company GST No.: " + COMPANY_GST_NO, tableX, y);
                g.setFont(new Font("Arial", Font.PLAIN, 12));

                y += 15;
                g.drawString("Bank Name: " + BANK_NAME, tableX, y);

                y += 15;
                g.drawString("Bank Account Number: " + BANK_ACC, tableX, y);

                y += 15;
                g.drawString("Bank Branch IFSC: " + BANK_BRANCH, tableX, y);

                y += 30;
                g.drawString("E. & O.E. ", tableX, y);
                y += 15;
                g.drawString("Subject to Vadodara Jurisdication.", tableX, y);

                // Signature at bottom-right
                int sigY = (int)height - 30;
                g.drawString(SIGNATURE_TEXT, (int)width - g.getFontMetrics().stringWidth(SIGNATURE_TEXT)-20, sigY);

                return PAGE_EXISTS;
            }
        });

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Utility to convert numbers into words (Indian numbering system).
     */
    private static class NumberToWordsConverter {
        private static final String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
        private static final String[] teens = {"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        private static final String[] tens  = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

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
                    words += tens[(int)(number / 10)];
                    if (number % 10 > 0) words += " " + units[(int)(number % 10)];
                }
            }
            return words.trim();
        }
    }
}
