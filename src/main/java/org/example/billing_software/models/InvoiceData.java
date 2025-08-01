package org.example.billing_software.models;

import java.util.List;

public class InvoiceData {
    private String invoiceNo;
    private String client;
    private String gst;
    private String date;
    private String carMake;
    private String carModel;
    private String carLicense;
    private List<LineItem> items;
    private double subtotal;
    private double cgst;
    private double sgst;
    private double total;

    public InvoiceData(String invoiceNo, String client, String gst, String date,
                       List<LineItem> items,
                       String carMake, String carModel, String carLicense,
                       double subtotal, double cgst, double sgst, double total) {
        this.invoiceNo = invoiceNo;
        this.client = client;
        this.gst = gst;
        this.date = date;
        this.items = items;
        this.carMake = carMake;
        this.carModel = carModel;
        this.carLicense = carLicense;
        this.subtotal = subtotal;
        this.cgst = cgst;
        this.sgst = sgst;
        this.total = total;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getGst() {
        return gst;
    }

    public void setGst(String gst) {
        this.gst = gst;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCarMake() {
        return carMake;
    }

    public void setCarMake(String carMake) {
        this.carMake = carMake;
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public String getCarLicense() {
        return carLicense;
    }

    public void setCarLicense(String carLicense) {
        this.carLicense = carLicense;
    }

    public List<LineItem> getItems() {
        return items;
    }

    public void setItems(List<LineItem> items) {
        this.items = items;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getCgst() {
        return cgst;
    }

    public void setCgst(double cgst) {
        this.cgst = cgst;
    }

    public double getSgst() {
        return sgst;
    }

    public void setSgst(double sgst) {
        this.sgst = sgst;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
