package com.heartpredict.ml;

import java.util.*;

/** K-Nearest Neighbours (Euclidean distance, majority vote) implemented from scratch. */
public class KNN {
    private final int k;
    private double[][] X;
    private int[] y;

    public KNN(int k) { this.k = k; }

    public void fit(double[][] X, int[] y) { this.X = X; this.y = y; }

    /** Probability of class 1 = fraction of the k nearest neighbours that are positive. */
    public double proba(double[] q) {
        int n = X.length;
        double[] dist = new double[n];
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) {
            double s = 0;
            for (int j = 0; j < q.length; j++) { double t = X[i][j] - q[j]; s += t * t; }
            dist[i] = s; idx[i] = i;
        }
        Arrays.sort(idx, Comparator.comparingDouble(a -> dist[a]));
        int kk = Math.min(k, n), pos = 0;
        for (int i = 0; i < kk; i++) pos += y[idx[i]];
        return (double) pos / kk;
    }

    public int predict(double[] q) { return proba(q) >= 0.5 ? 1 : 0; }
}
