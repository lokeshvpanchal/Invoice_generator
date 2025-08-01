package org.example.billing_software.services;

import org.example.billing_software.models.InvoiceData;
import org.example.billing_software.models.InvoiceSummary;
import org.example.billing_software.models.LineItem;
import org.example.billing_software.views.InvoiceListView;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

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
                double sum = data.getItems().stream().mapToDouble(i -> i.amount.get()).sum();
                double subtotal = sum;
                double cgst = subtotal * CGST_RATE;
                double sgst = subtotal * SGST_RATE;
                double total =  subtotal + cgst + sgst;

                ps.setString(1, data.getInvoiceNo());
                ps.setString(2, data.getClient());
                ps.setString(3, data.getGst());
                ps.setString(4, data.getDate());
                ps.setString(5, data.getCarMake());
                ps.setString(6, data.getCarModel());
                ps.setString(7, data.getCarLicense());
                ps.setDouble(8, cgst);
                ps.setDouble(9, sgst);
                ps.setDouble(10, total);
                ps.executeUpdate();
            }

            String sqlItem = """
            INSERT INTO invoice_items(invoice_no, particulars, quantity, amount, rate)
            VALUES(?,?,?,?,?)""";
            try (PreparedStatement ps = conn.prepareStatement(sqlItem)) {
                for (LineItem item : data.getItems()) {
                    int qty = item.quantity.get();
                    double inputAmt = item.amount.get();
                    double netAmt = inputAmt;
                    double netRate = qty > 0 ? netAmt / qty : 0;

                    ps.setString(1, data.getInvoiceNo());
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
    public static InvoiceData fetchInvoice(Connection conn, String invoiceNo) {
        try (PreparedStatement psInv = conn.prepareStatement(
                "SELECT client, gst, date, car_make, car_model, license_no, cgst, sgst, total " +
                        "FROM invoices WHERE invoice_no = ?")) {
            psInv.setString(1, invoiceNo);
            try (ResultSet rs = psInv.executeQuery()) {
                if (!rs.next()) return null;

                String client    = rs.getString("client");
                String gst       = rs.getString("gst");
                String date      = rs.getString("date");
                String carMake   = rs.getString("car_make");
                String carModel  = rs.getString("car_model");
                String licenseNo = rs.getString("license_no");
                double cgst      = rs.getDouble("cgst");
                double sgst      = rs.getDouble("sgst");
                double total     = rs.getDouble("total");
                double subtotal  = total - cgst - sgst;

                List<LineItem> items = new ArrayList<>();
                try (PreparedStatement psItems = conn.prepareStatement(
                        "SELECT particulars, quantity, rate, amount FROM invoice_items WHERE invoice_no = ?")) {
                    psItems.setString(1, invoiceNo);
                    try (ResultSet rsItems = psItems.executeQuery()) {
                        while (rsItems.next()) {
                            LineItem it = new LineItem();
                            it.particulars.set(rsItems.getString("particulars"));
                            it.quantity   .set(rsItems.getInt("quantity"));
                            it.rate       .set(rsItems.getDouble("rate"));
                            it.amount     .set(rsItems.getDouble("amount"));
                            items.add(it);
                        }
                    }
                }

                return new InvoiceData(
                        invoiceNo, client, gst, date,
                        items, carMake, carModel, licenseNo,
                        subtotal, cgst, sgst, total
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<InvoiceSummary> fetchInvoicesWithYearMonth(Connection conn, int year, int month) {
        List<InvoiceSummary> list = new ArrayList<>();
        String sql = "SELECT invoice_no, client, date, total FROM invoices " +
                "WHERE strftime('%Y', date) = ? AND strftime('%m', date) = ? ORDER BY date DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(year));
            ps.setString(2, String.format("%02d", month));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new InvoiceSummary(
                        rs.getString("invoice_no"),
                        rs.getString("client"),
                        rs.getString("date"),
                        rs.getDouble("total")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
    public static List<InvoiceSummary> fetchInvoicesWithDateRange(Connection conn, LocalDate start, LocalDate end) {
        List<InvoiceSummary> list = new ArrayList<>();
        String sql = "SELECT invoice_no, client, date, total FROM invoices";

        if (start != null && end != null) {
            sql += " WHERE date BETWEEN ? AND ?";
        } else {
            sql += " ORDER BY date DESC";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (start != null && end != null) {
                ps.setString(1, start.toString());
                ps.setString(2, end.toString());
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new InvoiceSummary(
                        rs.getString("invoice_no"),
                        rs.getString("client"),
                        rs.getString("date"),
                        rs.getDouble("total")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean updateInvoice(Connection conn, InvoiceData data) {
        final double CGST_RATE = 0.09;
        final double SGST_RATE = 0.09;

        String updInvoiceSql = """
        UPDATE invoices
           SET client    = ?,
               gst       = ?,
               date      = ?,
               car_make  = ?,
               car_model = ?,
               license_no= ?,
               cgst      = ?,
               sgst      = ?,
               total     = ?
         WHERE invoice_no = ?
    """;

        String delItemsSql = """
        DELETE FROM invoice_items
         WHERE invoice_no = ?
    """;

        String insItemSql = """
        INSERT INTO invoice_items(
           invoice_no, particulars, quantity, amount, rate
        ) VALUES (?,?,?,?,?)
    """;

        try {
            conn.setAutoCommit(false);

            // 1) Recompute totals
            double subtotal = data.getItems().stream()
                    .mapToDouble(i -> i.amount.get())
                    .sum();
            double cgst = subtotal * CGST_RATE;
            double sgst = subtotal * SGST_RATE;
            double total = subtotal + cgst + sgst;

            // 2) Update invoice header
            try (PreparedStatement ps = conn.prepareStatement(updInvoiceSql)) {
                ps.setString(1, data.getClient());
                ps.setString(2, data.getGst());
                ps.setString(3, data.getDate());
                ps.setString(4, data.getCarMake());
                ps.setString(5, data.getCarModel());
                ps.setString(6, data.getCarLicense());
                ps.setDouble(7, cgst);
                ps.setDouble(8, sgst);
                ps.setDouble(9, total);
                ps.setString(10, data.getInvoiceNo());
                ps.executeUpdate();
            }

            // 3) Remove existing items
            try (PreparedStatement ps = conn.prepareStatement(delItemsSql)) {
                ps.setString(1, data.getInvoiceNo());
                ps.executeUpdate();
            }

            // 4) Insert updated items
            try (PreparedStatement ps = conn.prepareStatement(insItemSql)) {
                for (LineItem item : data.getItems()) {
                    int qty    = item.quantity.get();
                    double amt = item.amount.get();
                    double rate= qty > 0 ? amt/qty : 0.0;

                    ps.setString(1, data.getInvoiceNo());
                    ps.setString(2, item.particulars.get());
                    ps.setInt   (3, qty);
                    ps.setDouble(4, amt);
                    ps.setDouble(5, rate);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return true;
        }
        catch (SQLException ex) {
            ex.printStackTrace();
            try { conn.rollback(); } catch (SQLException e) { e.printStackTrace(); }
            return false;
        }
        finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public static void deleteInvoice(Connection conn, String invoiceNo) {
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement delItems = conn.prepareStatement("DELETE FROM invoice_items WHERE invoice_no = ?");
                 PreparedStatement delInvoice = conn.prepareStatement("DELETE FROM invoices WHERE invoice_no = ?")) {

                delItems.setString(1, invoiceNo);
                delItems.executeUpdate();

                delInvoice.setString(1, invoiceNo);
                delInvoice.executeUpdate();

                conn.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    // In InvoiceRepository.java (add this method):
    public static Map<LocalDate, Double> fetchDailySales(Connection conn, int year, int month) {
        Map<LocalDate, Double> salesMap = new LinkedHashMap<>();
        String sql = "SELECT date, SUM(total) AS daily_total " +
                "FROM invoices " +
                "WHERE strftime('%Y', date)=? AND strftime('%m', date)=? " +
                "GROUP BY date ORDER BY date";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(year));
            ps.setString(2, String.format("%02d", month));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LocalDate d = LocalDate.parse(rs.getString("date"));
                double total = rs.getDouble("daily_total");
                salesMap.put(d, total);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return salesMap;
    }
    /**
     * Fetches total sales aggregated by month for a given year.
     *
     * @param conn  the database connection
     * @param year  the year to filter by (e.g. 2025)
     * @return a map from month number (1–12) to total sales for that month
     */
    public static Map<Integer, Double> fetchMonthlySales(Connection conn, int year) {
        Map<Integer, Double> monthlySales = new HashMap<>();
        String sql = """
        SELECT CAST(strftime('%m', date) AS INTEGER) AS month,
               SUM(total) AS sales
          FROM invoices
         WHERE strftime('%Y', date) = ?
         GROUP BY month
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(year));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int month = rs.getInt("month");
                    double sales = rs.getDouble("sales");
                    monthlySales.put(month, sales);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return monthlySales;
    }

    /**
     * Fetches total sales aggregated by year for a given range of years.
     *
     * @param conn       the database connection
     * @param startYear  the first year in the range (inclusive)
     * @param endYear    the last year in the range (inclusive)
     * @return a map from year (e.g. 2020) to total sales in that year
     */
    public static Map<Integer, Double> fetchYearlySales(Connection conn, int startYear, int endYear) {
        Map<Integer, Double> yearlySales = new HashMap<>();
        String sql = """
        SELECT
          CAST(strftime('%Y', date) AS INTEGER) AS year,
          SUM(total) AS sales
        FROM invoices
        WHERE CAST(strftime('%Y', date) AS INTEGER) BETWEEN ? AND ?
        GROUP BY year
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, startYear);
            ps.setInt(2, endYear);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    yearlySales.put(
                            rs.getInt("year"),
                            rs.getDouble("sales")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return yearlySales;
    }


}
