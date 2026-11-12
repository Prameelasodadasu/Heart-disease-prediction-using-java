package com.heartpredict.ml;

import java.io.*;
import java.util.*;

/** Loads a CSV (last column = target) and performs cleaning: missing-value imputation + de-duplication. */
public class Dataset {
    public final String[] features;
    public final double[][] X;
    public final int[] y;

    public Dataset(String[] features, double[][] X, int[] y) {
        this.features = features; this.X = X; this.y = y;
    }

    public static Dataset loadCsv(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String[] header = br.readLine().trim().split(",");
        int d = header.length - 1;
        String[] feats = new String[d];
        for (int i = 0; i < d; i++) feats[i] = header[i].trim();

        Set<String> seen = new HashSet<>();
        List<double[]> rows = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            String[] p = line.split(",", -1);
            if (p.length != header.length) continue;
            double[] r = new double[d + 1];
            for (int i = 0; i <= d; i++) {
                try { r[i] = Double.parseDouble(p[i].trim()); } catch (Exception e) { r[i] = Double.NaN; }
            }
            if (Double.isNaN(r[d])) continue;               // no label -> drop
            if (!seen.add(Arrays.toString(r))) continue;     // duplicate -> drop
            rows.add(r);
        }
        // impute missing feature values with the column median
        for (int c = 0; c < d; c++) {
            final int col = c;
            double[] vals = rows.stream().mapToDouble(r -> r[col]).filter(v -> !Double.isNaN(v)).sorted().toArray();
            double med = vals[vals.length / 2];
            for (double[] r : rows) if (Double.isNaN(r[c])) r[c] = med;
        }
        double[][] X = new double[rows.size()][d];
        int[] y = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            System.arraycopy(rows.get(i), 0, X[i], 0, d);
            y[i] = rows.get(i)[d] > 0 ? 1 : 0;
        }
        return new Dataset(feats, X, y);
    }
}
