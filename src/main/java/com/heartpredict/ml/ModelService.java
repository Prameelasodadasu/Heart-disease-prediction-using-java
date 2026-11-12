package com.heartpredict.ml;

import java.util.*;

/** Full ML pipeline: split -> feature selection -> normalisation -> tune K -> train KNN + LR -> evaluate. */
public class ModelService {
    public final String[] allFeatures;
    public String[] selectedNames;
    public double[] corr;
    public int[] selected;
    public int bestK, trainSize, testSize;
    public Metrics knnMetrics, lrMetrics, ensembleMetrics;

    private final Scaler scaler = new Scaler();
    private KNN knn;
    private LogisticRegression lr;

    public ModelService(Dataset ds) {
        allFeatures = ds.features;
        int n = ds.X.length;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) order.add(i);
        Collections.shuffle(order, new Random(42));
        int nTrain = (int) (n * 0.8);
        trainSize = nTrain; testSize = n - nTrain;

        double[][] Xtr = new double[nTrain][], Xte = new double[n - nTrain][];
        int[] ytr = new int[nTrain], yte = new int[n - nTrain];
        for (int i = 0; i < n; i++) {
            int src = order.get(i);
            if (i < nTrain) { Xtr[i] = ds.X[src]; ytr[i] = ds.y[src]; }
            else { Xte[i - nTrain] = ds.X[src]; yte[i - nTrain] = ds.y[src]; }
        }

        // feature selection (on training data only)
        corr = FeatureSelector.correlations(Xtr, ytr);
        selected = FeatureSelector.select(corr, 0.10, 5);
        selectedNames = new String[selected.length];
        for (int i = 0; i < selected.length; i++) selectedNames[i] = allFeatures[selected[i]];
        Xtr = FeatureSelector.cols(Xtr, selected);
        Xte = FeatureSelector.cols(Xte, selected);

        // normalisation
        scaler.fit(Xtr);
        Xtr = scaler.transform(Xtr);
        Xte = scaler.transform(Xte);

        // tune K with 5-fold cross-validation
        bestK = tuneK(Xtr, ytr);
        knn = new KNN(bestK); knn.fit(Xtr, ytr);
        lr = new LogisticRegression(); lr.fit(Xtr, ytr, 0.1, 1500, 0.01);

        int[] pk = new int[yte.length], pl = new int[yte.length], pe = new int[yte.length];
        for (int i = 0; i < yte.length; i++) {
            double a = knn.proba(Xte[i]), b = lr.proba(Xte[i]);
            pk[i] = a >= 0.5 ? 1 : 0; pl[i] = b >= 0.5 ? 1 : 0; pe[i] = (a + b) / 2 >= 0.5 ? 1 : 0;
        }
        knnMetrics = Metrics.of(yte, pk); lrMetrics = Metrics.of(yte, pl); ensembleMetrics = Metrics.of(yte, pe);
    }

    private int tuneK(double[][] X, int[] y) {
        int folds = 5, best = 5; double bestAcc = -1;
        for (int k = 3; k <= 21; k += 2) {
            int ok = 0;
            for (int f = 0; f < folds; f++) {
                List<double[]> tr = new ArrayList<>(); List<Integer> ty = new ArrayList<>();
                for (int i = 0; i < X.length; i++) if (i % folds != f) { tr.add(X[i]); ty.add(y[i]); }
                KNN m = new KNN(k);
                m.fit(tr.toArray(new double[0][]), ty.stream().mapToInt(Integer::intValue).toArray());
                for (int i = f; i < X.length; i += folds) if (m.predict(X[i]) == y[i]) ok++;
            }
            double acc = (double) ok / X.length;
            if (acc > bestAcc) { bestAcc = acc; best = k; }
        }
        return best;
    }

    /** @return {knnProbability, logisticProbability, ensembleProbability} */
    public double[] predict(Map<String, Double> input) {
        double[] raw = new double[allFeatures.length];
        for (int i = 0; i < raw.length; i++) {
            Double v = input.get(allFeatures[i]);
            if (v == null) throw new IllegalArgumentException("Missing value for " + allFeatures[i]);
            raw[i] = v;
        }
        double[] x = scaler.transform(FeatureSelector.cols(raw, selected));
        double a = knn.proba(x), b = lr.proba(x);
        return new double[]{a, b, (a + b) / 2};
    }

    public static String riskLevel(double p) { return p < 0.35 ? "Low" : p < 0.6 ? "Moderate" : "High"; }
}
