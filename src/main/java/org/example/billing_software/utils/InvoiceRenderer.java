package org.example.billing_software.utils;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.print.PageFormat;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class InvoiceRenderer {
    // constants
    private static final String LOGO_PATH       = "/img/logo.png";
    private static final String COMPANY_GST_NO  = "29ABCDE1234F2Z5";
    private static final String BANK_NAME       = "HDFC BANK";
    private static final String BANK_ACC        = "50200045669341";
    private static final String BANK_BRANCH     = "HDFC0003789";
    private static final String SIGNATURE_TEXT  = "Authorized Signatory";
    private static final double CGST_RATE       = 0.09;
    private static final double SGST_RATE       = 0.09;

    public static void render(Graphics2D g, PageFormat pf, InvoiceData data) {
        // shift origin to imageable area
        g.translate(pf.getImageableX(), pf.getImageableY());
        double width  = pf.getImageableWidth();
        double height = pf.getImageableHeight();

        int x = 20, y = 20;

        // --- logo ---
        try (InputStream is = InvoiceRenderer.class.getResourceAsStream(LOGO_PATH)) {
            if (is != null) {
                Image logo = ImageIO.read(is);
                g.drawImage(logo, x, y, (int)(width/2.8), 50, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- company address below logo ---
        y += 70;
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString("GF-10/11, Kasper Square, Opp. Gangotri Exotica,", x, y);
        y += 13;
        g.drawString("30 mtr Gotri-Laxmipura Road, Vadodara - 390021", x, y);
        y += 13;
        g.drawString("M:+91 99789 15902   Email: autocraft.gj06@gmail.com", x, y);

        // --- header (Bill No, Date) ---
        int hdrX = (int)(width * 6/8);
        y = 86;
        g.drawString("Bill No:", hdrX,    y += 13);
        g.drawString(data.invoiceNo,      hdrX +  (int)(width/8), y);
        g.drawString("Date:",    hdrX,    y += 13);
        g.drawString(data.date,            hdrX +  (int)(width/8), y);

        // --- client & car info ---
        y += 50;
        int col2 = (int)(width/6);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Client:",  x, y);
        g.drawString(data.client, col2, y);
        g.drawString("Car Make:", (int)(width*4/6), y);
        g.drawString(data.carMake, (int)(width*5/6), y);

        y += 13;
        g.drawString("GST No.:", x, y);
        g.drawString(data.gst.isEmpty() ? "Not Applicable" : data.gst, col2, y);
        g.drawString("Car Model:", (int)(width*4/6), y);
        g.drawString(data.carModel, (int)(width*5/6), y);

        // --- table header ---
        y += 30;
        int tableX = x, qtyX = (int)(width*0.5), rateX = (int)(width*0.7), amtX = (int)(width*0.85);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.drawString("No.",          tableX,        y);
        g.drawString("Particulars",  tableX + 30,   y);
        g.drawString("Qty",          qtyX,          y);
        g.drawString("Rate",         rateX,         y);
        g.drawString("Amount",       amtX,          y);

        // --- table rows ---
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        y += 20;
        int idx = 1;
        for (var item : data.items) {
            double raw = item.amount.get();
            double displayAmt = data.taxIncluded
                    ? raw / (1 + CGST_RATE + SGST_RATE)
                    : raw;
            double rate = item.quantity.get() != 0
                    ? displayAmt / item.quantity.get()
                    : 0.0;
            g.drawString(String.valueOf(idx++),       tableX,        y);
            g.drawString(item.particulars.get(),      tableX + 30,   y);
            g.drawString(String.valueOf(item.quantity.get()), qtyX, y);
            g.drawString(String.format("%.2f", rate), rateX,       y);
            g.drawString(String.format("%.2f", displayAmt), amtX,   y);
            y += 15;
        }

        // --- totals & words ---
        y += 15;
        int labelX = rateX;
        g.drawString("Subtotal:", labelX, y);
        g.drawString(String.format("%.2f", data.subtotal), amtX, y);
        y += 15;
        g.drawString("CGST:",     labelX, y);
        g.drawString(String.format("%.2f", data.cgst),     amtX, y);
        y += 15;
        g.drawString("SGST:",     labelX, y);
        g.drawString(String.format("%.2f", data.sgst),     amtX, y);

        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Total:",    labelX, y += 15);
        g.drawString(String.format("%.2f", data.total),    amtX, y);

        g.setFont(new Font("Arial", Font.PLAIN, 12));
        String words = NumberToWordsConverter.convert((long)data.total) + " Rupees Only";
        g.drawString("Rupees in Words: " + words, tableX, y);

        // --- footer ---
        y = (int)height - 120;
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Company GST No.: " + COMPANY_GST_NO, tableX, y);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        for (String line : new String[]{
                "Bank Name: " + BANK_NAME,
                "Bank Account Number: " + BANK_ACC,
                "Bank Branch IFSC: " + BANK_BRANCH,
                "E. & O.E.",
                "Subject to Vadodara Jurisdiction."
        }) {
            y += 15;
            g.drawString(line, tableX, y);
        }

        // signature
        int sigY = (int)height - 30;
        g.drawString(SIGNATURE_TEXT,
                (int)width - g.getFontMetrics().stringWidth(SIGNATURE_TEXT) - 20,
                sigY);
    }

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
    }}
