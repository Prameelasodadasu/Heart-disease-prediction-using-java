package com.heartpredict.db;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Stores patient records in MySQL. If MySQL is unreachable it falls back to memory so the app still runs. */
public class PatientRepository {

    public static class Record {
        public long id; public String name; public double[] values;
        public double knn, lr, score; public String risk, created;
    }

    private final String[] feats;
    private final String url, user, pass;
    private boolean mysql = false;
    private final List<Record> mem = new ArrayList<>();
    private long seq = 1;

    public PatientRepository(String[] feats) {
        this.feats = feats;
        url = env("DB_URL", "jdbc:mysql://localhost:3306/heart_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        user = env("DB_USER", "root");
        pass = env("DB_PASS", "root");
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS patients (id BIGINT AUTO_INCREMENT PRIMARY KEY, patient_name VARCHAR(120) NOT NULL, ");
        for (String f : feats) sql.append(f).append(" DOUBLE, ");
        sql.append("knn_prob DOUBLE, lr_prob DOUBLE, risk_score DOUBLE, risk_level VARCHAR(20), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        try (Connection c = DriverManager.getConnection(url, user, pass); Statement s = c.createStatement()) {
            s.execute(sql.toString());
            mysql = true;
            System.out.println("[DB] Connected to MySQL.");
        } catch (Exception e) {
            System.out.println("[DB] MySQL unavailable (" + e.getMessage() + ") -> using in-memory storage.");
        }
    }

    private static String env(String k, String d) { String v = System.getenv(k); return v == null ? d : v; }

    public String mode() { return mysql ? "MySQL" : "In-memory (MySQL not connected)"; }

    public synchronized void save(Record r) {
        if (mysql) {
            StringBuilder cols = new StringBuilder("patient_name"), qs = new StringBuilder("?");
            for (String f : feats) { cols.append(",").append(f); qs.append(",?"); }
            cols.append(",knn_prob,lr_prob,risk_score,risk_level"); qs.append(",?,?,?,?");
            try (Connection c = DriverManager.getConnection(url, user, pass);
                 PreparedStatement ps = c.prepareStatement("INSERT INTO patients(" + cols + ") VALUES(" + qs + ")", Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                ps.setString(i++, r.name);
                for (double v : r.values) ps.setDouble(i++, v);
                ps.setDouble(i++, r.knn); ps.setDouble(i++, r.lr); ps.setDouble(i++, r.score); ps.setString(i, r.risk);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) r.id = k.getLong(1); }
                r.created = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                return;
            } catch (SQLException e) {
                System.out.println("[DB] Save failed, switching to memory: " + e.getMessage());
                mysql = false;
            }
        }
        r.id = seq++;
        r.created = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        mem.add(0, r);
    }

    public synchronized List<Record> list() {
        if (!mysql) return new ArrayList<>(mem);
        List<Record> out = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM patients ORDER BY id DESC LIMIT 200")) {
            while (rs.next()) {
                Record r = new Record();
                r.id = rs.getLong("id"); r.name = rs.getString("patient_name");
                r.values = new double[feats.length];
                for (int i = 0; i < feats.length; i++) r.values[i] = rs.getDouble(feats[i]);
                r.knn = rs.getDouble("knn_prob"); r.lr = rs.getDouble("lr_prob");
                r.score = rs.getDouble("risk_score"); r.risk = rs.getString("risk_level");
                r.created = rs.getTimestamp("created_at").toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                out.add(r);
            }
        } catch (SQLException e) { System.out.println("[DB] List failed: " + e.getMessage()); }
        return out;
    }

    public synchronized void delete(long id) {
        if (!mysql) { mem.removeIf(r -> r.id == id); return; }
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement("DELETE FROM patients WHERE id=?")) {
            ps.setLong(1, id); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] Delete failed: " + e.getMessage()); }
    }
}
