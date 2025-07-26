// InvoiceData.java
package org.example.billing_software.utils;

import org.example.billing_software.CreateInvoiceForm;

import java.util.List;

public class InvoiceData {
    public final String invoiceNo, client, gst, date, carMake, carModel, carLicense;
    public final List<CreateInvoiceForm.LineItem> items;
    public final boolean taxIncluded;
    public final double subtotal, cgst, sgst, total;

    public InvoiceData(String invoiceNo, String client, String gst, String date,
                       List<CreateInvoiceForm.LineItem> items, boolean taxIncluded,
                       String carMake, String carModel, String carLicense,
                       double subtotal, double cgst, double sgst, double total) {
        this.invoiceNo = invoiceNo;
        this.client    = client;
        this.gst       = gst;
        this.date      = date;
        this.items     = items;
        this.taxIncluded = taxIncluded;
        this.carMake   = carMake;
        this.carModel  = carModel;
        this.carLicense  = carLicense;
        this.subtotal  = subtotal;
        this.cgst      = cgst;
        this.sgst      = sgst;
        this.total     = total;
    }
}
