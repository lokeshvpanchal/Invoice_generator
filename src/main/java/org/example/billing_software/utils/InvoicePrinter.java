// InvoicePrinter.java
package org.example.billing_software.utils;

import java.awt.print.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class InvoicePrinter {

    /** print to a printer dialog **/
    public static void printInvoice(InvoiceData data) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable((Graphics graphics, PageFormat pf, int pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            InvoiceRenderer.render(g2, pf, data);
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            job.print();
        }
    }

    /** generate a BufferedImage you can e-mail or embed **/
    public static BufferedImage createInvoiceImage(InvoiceData data, PageFormat pf) {
        int w = (int)pf.getImageableWidth();
        int h = (int)pf.getImageableHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        // white background
        g2.setPaint(Color.white);
        g2.fillRect(0,0,w,h);
        InvoiceRenderer.render(g2, pf, data);
        g2.dispose();
        return img;
    }
}
