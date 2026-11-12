package com.heartpredict.ml;

/** Binary logistic regression trained with batch gradient descent and L2 regularisation. */
public class LogisticRegression {
    private double[] w;
    private double b;

    public void fit(double[][] X, int[] y, double lr, int epochs, double lambda) {
        int n = X.length, d = X[0].length;
        w = new double[d]; b = 0;
        for (int e = 0; e < epochs; e++) {
            double[] gw = new double[d]; double gb = 0;
            for (int i = 0; i < n; i++) {
                double err = proba(X[i]) - y[i];
                for (int j = 0; j < d; j++) gw[j] += err * X[i][j];
                gb += err;
            }
            for (int j = 0; j < d; j++) w[j] -= lr * (gw[j] / n + lambda * w[j]);
            b -= lr * gb / n;
        }
    }

    public double proba(double[] x) {
        double z = b;
        for (int j = 0; j < x.length; j++) z += w[j] * x[j];
        z = Math.max(-30, Math.min(30, z));
        return 1.0 / (1.0 + Math.exp(-z));
    }

    public int predict(double[] x) { return proba(x) >= 0.5 ? 1 : 0; }
}
