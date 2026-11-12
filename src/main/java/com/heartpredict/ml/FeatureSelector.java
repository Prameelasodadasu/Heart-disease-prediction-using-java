package com.heartpredict.ml;

import java.util.*;

/** Filter-based feature selection using absolute Pearson correlation with the label. */
public class FeatureSelector {

    public static double[] correlations(double[][] X, int[] y) {
        int d = X[0].length, n = X.length;
        double my = Arrays.stream(y).average().orElse(0);
        double[] c = new double[d];
        for (int j = 0; j < d; j++) {
            double mx = 0; for (double[] r : X) mx += r[j]; mx /= n;
            double sxy = 0, sxx = 0, syy = 0;
            for (int i = 0; i < n; i++) {
                double dx = X[i][j] - mx, dy = y[i] - my;
                sxy += dx * dy; sxx += dx * dx; syy += dy * dy;
            }
            c[j] = (sxx == 0 || syy == 0) ? 0 : sxy / Math.sqrt(sxx * syy);
        }
        return c;
    }

    /** Keep features with |corr| >= threshold (but never fewer than minKeep). Indices returned in original order. */
    public static int[] select(double[] corr, double threshold, int minKeep) {
        Integer[] order = new Integer[corr.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(Math.abs(corr[b]), Math.abs(corr[a])));
        List<Integer> keep = new ArrayList<>();
        for (int i = 0; i < order.length; i++)
            if (Math.abs(corr[order[i]]) >= threshold || i < minKeep) keep.add(order[i]);
        Collections.sort(keep);
        return keep.stream().mapToInt(Integer::intValue).toArray();
    }

    public static double[][] cols(double[][] X, int[] idx) {
        double[][] o = new double[X.length][idx.length];
        for (int i = 0; i < X.length; i++) for (int j = 0; j < idx.length; j++) o[i][j] = X[i][idx[j]];
        return o;
    }

    public static double[] cols(double[] x, int[] idx) {
        double[] o = new double[idx.length];
        for (int j = 0; j < idx.length; j++) o[j] = x[idx[j]];
        return o;
    }
}
