package org.example.billing_software.models;

/**
 * A simple model representing a summary view of an invoice.
 */
public class InvoiceSummary {
    private String invoiceNo;
    private String client;
    private String date;
    private double total;

    /**
     * Constructs a new InvoiceSummary.
     *
     * @param invoiceNo the invoice number
     * @param client    the client name
     * @param date      the date of the invoice (YYYY-MM-DD)
     * @param total     the total amount of the invoice
     */
    public InvoiceSummary(String invoiceNo, String client, String date, double total) {
        this.invoiceNo = invoiceNo;
        this.client = client;
        this.date = date;
        this.total = total;
    }

    // Getters
    public String getInvoiceNo() {
        return invoiceNo;
    }

    public String getClient() {
        return client;
    }

    public String getDate() {
        return date;
    }

    public double getTotal() {
        return total;
    }

    // Setters
    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return String.format("InvoiceSummary[invoiceNo=%s, client=%s, date=%s, total=%.2f]",
                invoiceNo, client, date, total);
    }
}
