package com.heartpredict.ml;

public class Metrics {
    public int tp, tn, fp, fn;
    public double accuracy, precision, recall, f1;

    public static Metrics of(int[] truth, int[] pred) {
        Metrics m = new Metrics();
        for (int i = 0; i < truth.length; i++) {
            if (truth[i] == 1 && pred[i] == 1) m.tp++;
            else if (truth[i] == 0 && pred[i] == 0) m.tn++;
            else if (truth[i] == 0 && pred[i] == 1) m.fp++;
            else m.fn++;
        }
        m.accuracy = (double) (m.tp + m.tn) / truth.length;
        m.precision = m.tp + m.fp == 0 ? 0 : (double) m.tp / (m.tp + m.fp);
        m.recall = m.tp + m.fn == 0 ? 0 : (double) m.tp / (m.tp + m.fn);
        m.f1 = m.precision + m.recall == 0 ? 0 : 2 * m.precision * m.recall / (m.precision + m.recall);
        return m;
    }
}
