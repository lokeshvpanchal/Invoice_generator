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
import java.util.List;

import org.example.billing_software.CreateInvoiceForm.LineItem;

public class InvoicePrinter {

    // Company-specific constants
    private static final String LOGO_PATH = "/path/to/logo.png";
    private static final String COMPANY_GST_NO = "29ABCDE1234F2Z5";
    private static final String BANK_DETAILS = "Bank XYZ, A/C: 123456789";
    private static final String SIGNATURE_TEXT = "Authorized Signatory";

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

                int y = 0;
                // Draw logo
                try {
                    Image logo = ImageIO.read(new File(LOGO_PATH));
                    int logoHeight = 50;
                    g.drawImage(logo, 0, y, (int)(width/3), logoHeight, null);
                } catch (IOException e) {
                    // ignore missing logo
                }

                // Header: client and bill no
                y += 100;
                g.setFont(new Font("Serif", Font.BOLD, 12));
                g.drawString("Client: " + client, 20, y);
                String billLabel = "Bill No: " + invoiceNo;
                g.drawString(billLabel, (int)width - 2* g.getFontMetrics().stringWidth(billLabel), y);

                // GST and date
                y += 15;
                g.setFont(new Font("Serif", Font.PLAIN, 10));
                g.drawString("GST No.: " + gst, 0, y);
                String dateLabel = "Date: " + date;
                g.drawString(dateLabel, (int)width - g.getFontMetrics().stringWidth(dateLabel), y);

                // Table header (100% width)
                y += 30;
                int tableX = 0;
                int qtyX   = (int)(width * 0.5);
                int rateX  = (int)(width * 0.7);
                int amtX   = (int)(width * 0.85);
                g.setFont(new Font("Serif", Font.BOLD, 10));
                g.drawString("Particulars", tableX, y);
                g.drawString("Qty", qtyX, y);
                g.drawString("Rate", rateX, y);
                g.drawString("Amount", amtX, y);

                // Table rows
                g.setFont(new Font("Serif", Font.PLAIN, 10));
                y += 15;
                for (LineItem item : items) {
                    double rate = item.quantity.get() != 0
                            ? item.amount.get() / item.quantity.get()
                            : 0.0;
                    g.drawString(item.particulars.get(), tableX, y);
                    g.drawString(String.valueOf(item.quantity.get()), qtyX, y);
                    g.drawString(String.format("%.2f", rate), rateX, y);
                    g.drawString(String.format("%.2f", item.amount.get()), amtX, y);
                    y += 15;
                }

                // Totals block under Amount column
                y += 15;
                g.setFont(new Font("Serif", Font.PLAIN, 10));
                int labelX = (int)(width * 0.6);
                g.drawString("Subtotal:", labelX, y);
                g.drawString(String.format("%.2f", subtotal), amtX, y);
                y += 15;
                g.drawString("CGST:", labelX, y);
                g.drawString(String.format("%.2f", cgst), amtX, y);
                y += 15;
                g.drawString("SGST:", labelX, y);
                g.drawString(String.format("%.2f", sgst), amtX, y);
                y += 15;
                g.setFont(new Font("Serif", Font.BOLD, 10));
                g.drawString("Total:", labelX, y);
                g.drawString(String.format("%.2f", total), amtX, y);

                // Rupees in words (left side)
                y += 30;
                g.setFont(new Font("Serif", Font.PLAIN, 10));
                String words = NumberToWordsConverter.convert((long) total) + " Rupees Only";
                g.drawString(words, tableX, y);

                // Footer: company GST, bank details
                y += 30;
                g.drawString("Company GST No.: " + COMPANY_GST_NO, tableX, y);
                y += 15;
                g.drawString("Bank Details: " + BANK_DETAILS, tableX, y);

                // Signature at bottom-right
                int sigY = (int)height - 30;
                g.drawString(SIGNATURE_TEXT, (int)width - g.getFontMetrics().stringWidth(SIGNATURE_TEXT), sigY);

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
