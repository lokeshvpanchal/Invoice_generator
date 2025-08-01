// InvoiceData.java
package org.example.billing_software.models;


import java.util.List;

public class InvoiceData {
    public final String invoiceNo, client, gst, date, carMake, carModel, carLicense;
    public final List<LineItem> items;
    public final double subtotal, cgst, sgst, total;

    public InvoiceData(String invoiceNo, String client, String gst, String date,
                       List<LineItem> items,
                       String carMake, String carModel, String carLicense,
                       double subtotal, double cgst, double sgst, double total) {
        this.invoiceNo = invoiceNo;
        this.client    = client;
        this.gst       = gst;
        this.date      = date;
        this.items     = items;
        this.carMake   = carMake;
        this.carModel  = carModel;
        this.carLicense  = carLicense;
        this.subtotal  = subtotal;
        this.cgst      = cgst;
        this.sgst      = sgst;
        this.total     = total;
    }
}
