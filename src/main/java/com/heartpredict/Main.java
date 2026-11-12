package com.heartpredict;

import com.heartpredict.db.PatientRepository;
import com.heartpredict.db.PatientRepository.Record;
import com.heartpredict.ml.*;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class Main {
    static final Map<String, double[]> RANGES = new HashMap<>();
    static {
        RANGES.put("age", new double[]{1, 120}); RANGES.put("sex", new double[]{0, 1}); RANGES.put("cp", new double[]{0, 3});
        RANGES.put("trestbps", new double[]{50, 300}); RANGES.put("chol", new double[]{50, 700}); RANGES.put("fbs", new double[]{0, 1});
        RANGES.put("restecg", new double[]{0, 2}); RANGES.put("thalach", new double[]{40, 250}); RANGES.put("exang", new double[]{0, 1});
        RANGES.put("oldpeak", new double[]{0, 10}); RANGES.put("slope", new double[]{0, 2}); RANGES.put("ca", new double[]{0, 4});
        RANGES.put("thal", new double[]{1, 3});
    }
    static ModelService model;
    static PatientRepository repo;

    public static void main(String[] args) throws Exception {
        String file = System.getenv("DATA_FILE");
        Dataset ds;
        try (InputStream in = file != null ? new FileInputStream(file) : Main.class.getResourceAsStream("/data/heart.csv")) {
            ds = Dataset.loadCsv(in);
        }
        System.out.println("[ML] Loaded " + ds.X.length + " clean records, " + ds.features.length + " features.");
        model = new ModelService(ds);
        System.out.printf("[ML] Selected features: %s%n", Arrays.toString(model.selectedNames));
        System.out.printf("[ML] KNN (k=%d) accuracy: %.1f%% | Logistic Regression accuracy: %.1f%%%n",
                model.bestK, model.knnMetrics.accuracy * 100, model.lrMetrics.accuracy * 100);
        repo = new PatientRepository(model.allFeatures);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", safe(Main::staticPage));
        server.createContext("/api/model", safe(Main::modelInfo));
        server.createContext("/api/predict", safe(Main::predict));
        server.createContext("/api/patients", safe(Main::patients));
        server.createContext("/api/admin/login", safe(Main::login));
        server.createContext("/api/admin/logout", safe(Main::logout));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("\n  Patient page : http://localhost:" + port + "/");
        System.out.println("  Admin page   : http://localhost:" + port + "/admin   (password: " + ADMIN_PASS + ")\n");
    }

    // ---------- handlers ----------
    static void staticPage(HttpExchange ex) throws IOException {
        String p = ex.getRequestURI().getPath();
        String file;
        if (p.equals("/") || p.equals("/index.html")) file = "/web/index.html";
        else if (p.equals("/admin") || p.equals("/admin/")) file = "/web/admin.html";
        else { send(ex, 404, "text/plain", "Not found"); return; }
        try (InputStream in = Main.class.getResourceAsStream(file)) {
            send(ex, 200, "text/html; charset=utf-8", new String(readAll(in), StandardCharsets.UTF_8));
        }
    }

    // ---------- admin authentication ----------
    static final Set<String> sessions = java.util.concurrent.ConcurrentHashMap.newKeySet();
    static final String ADMIN_PASS = System.getenv("ADMIN_PASS") == null ? "admin123" : System.getenv("ADMIN_PASS");

    static String token(HttpExchange ex) {
        String c = ex.getRequestHeaders().getFirst("Cookie");
        if (c == null) return null;
        for (String part : c.split(";")) { part = part.trim(); if (part.startsWith("adm=")) return part.substring(4); }
        return null;
    }

    static boolean authed(HttpExchange ex) { String t = token(ex); return t != null && sessions.contains(t); }

    static void login(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { send(ex, 405, "application/json", err("POST required")); return; }
        Map<String, String> form = parseForm(body(ex));
        if (ADMIN_PASS.equals(form.get("password"))) {
            String t = UUID.randomUUID().toString(); sessions.add(t);
            ex.getResponseHeaders().add("Set-Cookie", "adm=" + t + "; Path=/; HttpOnly; SameSite=Strict");
            send(ex, 200, "application/json", "{\"ok\":true}");
        } else {
            try { Thread.sleep(800); } catch (InterruptedException ignored) { }
            send(ex, 401, "application/json", err("Wrong password"));
        }
    }

    static void logout(HttpExchange ex) throws IOException {
        String t = token(ex); if (t != null) sessions.remove(t);
        ex.getResponseHeaders().add("Set-Cookie", "adm=; Path=/; Max-Age=0");
        send(ex, 200, "application/json", "{\"ok\":true}");
    }

    static void modelInfo(HttpExchange ex) throws IOException {
        if (!authed(ex)) { send(ex, 401, "application/json", err("Admin login required")); return; }
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"features\":").append(arr(model.allFeatures)).append(",");
        sb.append("\"selected\":").append(arr(model.selectedNames)).append(",");
        sb.append("\"corr\":[");
        for (int i = 0; i < model.corr.length; i++) sb.append(i > 0 ? "," : "").append(num(model.corr[i]));
        sb.append("],\"k\":").append(model.bestK).append(",\"trainSize\":").append(model.trainSize)
          .append(",\"testSize\":").append(model.testSize)
          .append(",\"knn\":").append(metrics(model.knnMetrics))
          .append(",\"lr\":").append(metrics(model.lrMetrics))
          .append(",\"ensemble\":").append(metrics(model.ensembleMetrics))
          .append(",\"storage\":").append(q(repo.mode())).append("}");
        send(ex, 200, "application/json", sb.toString());
    }

    static void predict(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { send(ex, 405, "application/json", err("POST required")); return; }
        Map<String, String> form = parseForm(body(ex));
        Map<String, Double> in = new HashMap<>();
        double[] vals = new double[model.allFeatures.length];
        for (int i = 0; i < vals.length; i++) {
            String f = model.allFeatures[i], s = form.get(f);
            try { vals[i] = Double.parseDouble(s); } catch (Exception e) { send(ex, 400, "application/json", err("Invalid or missing value: " + f)); return; }
            double[] rg = RANGES.get(f);
            if (rg != null && (vals[i] < rg[0] || vals[i] > rg[1])) { send(ex, 400, "application/json", err("Please check the value for " + f)); return; }
            in.put(f, vals[i]);
        }
        double[] p = model.predict(in);
        Record r = new Record();
        r.name = form.getOrDefault("patient_name", "").trim().isEmpty() ? "Anonymous" : form.get("patient_name").trim();
        if (r.name.length() > 120) r.name = r.name.substring(0, 120);
        r.values = vals; r.knn = p[0]; r.lr = p[1]; r.score = p[2]; r.risk = ModelService.riskLevel(p[2]);
        repo.save(r);
        send(ex, 200, "application/json", "{\"id\":" + r.id + ",\"name\":" + q(r.name) + ",\"risk\":" + q(r.risk) + "}");
    }

    static void patients(HttpExchange ex) throws IOException {
        if (!authed(ex)) { send(ex, 401, "application/json", err("Admin login required")); return; }
        if (ex.getRequestMethod().equals("DELETE")) {
            Map<String, String> qs = parseForm(Optional.ofNullable(ex.getRequestURI().getRawQuery()).orElse(""));
            repo.delete(Long.parseLong(qs.getOrDefault("id", "-1")));
            send(ex, 200, "application/json", "{\"ok\":true}");
            return;
        }
        StringBuilder sb = new StringBuilder("{\"storage\":" + q(repo.mode()) + ",\"patients\":[");
        boolean first = true;
        for (Record r : repo.list()) {
            if (!first) sb.append(","); first = false;
            sb.append("{\"id\":").append(r.id).append(",\"name\":").append(q(r.name)).append(",\"created\":").append(q(r.created))
              .append(",\"knn\":").append(num(r.knn)).append(",\"lr\":").append(num(r.lr)).append(",\"score\":").append(num(r.score))
              .append(",\"risk\":").append(q(r.risk)).append(",\"values\":{");
            for (int i = 0; i < model.allFeatures.length; i++)
                sb.append(i > 0 ? "," : "").append(q(model.allFeatures[i])).append(":").append(num(r.values[i]));
            sb.append("}}");
        }
        send(ex, 200, "application/json", sb.append("]}").toString());
    }

    // ---------- helpers ----------
    static HttpHandler safe(HttpHandler h) {
        return ex -> {
            try { h.handle(ex); }
            catch (Exception e) { e.printStackTrace(); send(ex, 500, "application/json", err(String.valueOf(e.getMessage()))); }
        };
    }

    static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    static String body(HttpExchange ex) throws IOException { return new String(readAll(ex.getRequestBody()), StandardCharsets.UTF_8); }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096]; int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toByteArray();
    }

    static String dec(String s) {
        try { return URLDecoder.decode(s, "UTF-8"); } catch (UnsupportedEncodingException e) { return s; }
    }

    static Map<String, String> parseForm(String s) {
        Map<String, String> m = new HashMap<>();
        for (String kv : s.split("&")) {
            if (kv.isEmpty()) continue;
            int i = kv.indexOf('=');
            String k = dec(i < 0 ? kv : kv.substring(0, i));
            String v = i < 0 ? "" : dec(kv.substring(i + 1));
            m.put(k, v);
        }
        return m;
    }

    static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break;
                case '\t': sb.append(' '); break;
                default: sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }

    static String num(double d) { return String.valueOf(Math.round(d * 10000) / 10000.0); }
    static String err(String m) { return "{\"error\":" + q(m) + "}"; }
    static String arr(String[] a) { StringBuilder sb = new StringBuilder("["); for (int i = 0; i < a.length; i++) sb.append(i > 0 ? "," : "").append(q(a[i])); return sb.append("]").toString(); }
    static String metrics(Metrics m) {
        return "{\"accuracy\":" + num(m.accuracy) + ",\"precision\":" + num(m.precision) + ",\"recall\":" + num(m.recall) + ",\"f1\":" + num(m.f1)
                + ",\"tp\":" + m.tp + ",\"tn\":" + m.tn + ",\"fp\":" + m.fp + ",\"fn\":" + m.fn + "}";
    }
}
