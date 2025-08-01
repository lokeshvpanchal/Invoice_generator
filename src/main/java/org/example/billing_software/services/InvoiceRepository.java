package org.example.billing_software.services;

import org.example.billing_software.models.InvoiceData;
import org.example.billing_software.models.LineItem;

import java.sql.*;
import java.util.List;

public class InvoiceRepository {

    private static final double CGST_RATE = 0.09;
    private static final double SGST_RATE = 0.09;

    public static void createSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("""
                CREATE TABLE IF NOT EXISTS invoices(
                  invoice_no TEXT PRIMARY KEY,
                  client TEXT, gst TEXT, date TEXT,
                  car_make TEXT, car_model TEXT, license_no TEXT,
                  cgst REAL, sgst REAL, total REAL
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS invoice_items(
                  invoice_no TEXT,
                  particulars TEXT,
                  quantity INTEGER,
                  amount REAL,
                  rate REAL,
                  FOREIGN KEY(invoice_no) REFERENCES invoices(invoice_no)
                )""");
        }
    }

    public static int fetchNextBill(Connection conn) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(CAST(invoice_no AS INTEGER)),0)+1 FROM invoices")) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return 1;
        }
    }

    public static boolean saveInvoice(Connection conn, InvoiceData data) {
        try {
            conn.setAutoCommit(false);
            String sqlInv = """
                INSERT INTO invoices(invoice_no, client, gst, date, car_make, car_model, license_no, cgst, sgst, total)
                VALUES(?,?,?,?,?,?,?,?,?,?)""";
            try (PreparedStatement ps = conn.prepareStatement(sqlInv)) {
                double sum = data.items.stream().mapToDouble(i -> i.amount.get()).sum();
                double subtotal = sum;
                double cgst = subtotal * CGST_RATE;
                double sgst = subtotal * SGST_RATE;
                double total =  subtotal + cgst + sgst;

                ps.setString(1, data.invoiceNo);
                ps.setString(2, data.client);
                ps.setString(3, data.gst);
                ps.setString(4, data.date);
                ps.setString(5, data.carMake);
                ps.setString(6, data.carModel);
                ps.setString(7, data.carLicense);
                ps.setDouble(8, cgst);
                ps.setDouble(9, sgst);
                ps.setDouble(10, total);
                ps.executeUpdate();
            }

            String sqlItem = """
            INSERT INTO invoice_items(invoice_no, particulars, quantity, amount, rate)
            VALUES(?,?,?,?,?)""";
            try (PreparedStatement ps = conn.prepareStatement(sqlItem)) {
                for (LineItem item : data.items) {
                    int qty = item.quantity.get();
                    double inputAmt = item.amount.get();
                    double netAmt = inputAmt;
                    double netRate = qty > 0 ? netAmt / qty : 0;

                    ps.setString(1, data.invoiceNo);
                    ps.setString(2, item.particulars.get());
                    ps.setInt(3, qty);
                    ps.setDouble(4, netAmt);
                    ps.setDouble(5, netRate);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException e) { e.printStackTrace(); }
            ex.printStackTrace();
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
