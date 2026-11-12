package com.heartpredict.ml;

/** Z-score normalisation (fit on training data only). */
public class Scaler {
    private double[] mean, std;

    public void fit(double[][] X) {
        int d = X[0].length; mean = new double[d]; std = new double[d];
        for (double[] r : X) for (int j = 0; j < d; j++) mean[j] += r[j];
        for (int j = 0; j < d; j++) mean[j] /= X.length;
        for (double[] r : X) for (int j = 0; j < d; j++) std[j] += Math.pow(r[j] - mean[j], 2);
        for (int j = 0; j < d; j++) { std[j] = Math.sqrt(std[j] / X.length); if (std[j] == 0) std[j] = 1; }
    }

    public double[] transform(double[] x) {
        double[] o = new double[x.length];
        for (int j = 0; j < x.length; j++) o[j] = (x[j] - mean[j]) / std[j];
        return o;
    }

    public double[][] transform(double[][] X) {
        double[][] o = new double[X.length][];
        for (int i = 0; i < X.length; i++) o[i] = transform(X[i]);
        return o;
    }
}
