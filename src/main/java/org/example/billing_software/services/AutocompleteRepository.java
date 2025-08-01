package org.example.billing_software.services;

import org.example.billing_software.utils.TrieAutocomplete;

import java.sql.*;

public class AutocompleteRepository {

    public static void createTables(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS car_makes (
                name TEXT PRIMARY KEY,
                last_used TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )""");
        st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS car_models (
                name TEXT PRIMARY KEY,
                last_used TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )""");
        st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS particulars (
                name TEXT PRIMARY KEY,
                last_used TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )""");
    }

    public static void insertOrUpdate(Connection conn, String table, String value) {
        if (value == null || value.isBlank()) return;
        String sql = "INSERT INTO " + table + " (name, last_used) VALUES (?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(name) DO UPDATE SET last_used = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value.trim());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void loadIntoTrie(Connection conn, String table, TrieAutocomplete trie) {
        String sql = "SELECT name FROM " + table + " ORDER BY last_used DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                trie.insert(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
