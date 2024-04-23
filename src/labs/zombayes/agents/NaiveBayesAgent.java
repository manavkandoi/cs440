package src.labs.zombayes.agents;

// Import statements...
import edu.bu.labs.zombayes.agents.SurvivalAgent;
import edu.bu.labs.zombayes.features.Features.FeatureType;
import edu.bu.labs.zombayes.linalg.Matrix;
import edu.bu.labs.zombayes.linalg.Shape;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NaiveBayesAgent extends SurvivalAgent {

    public static class NaiveBayes {
        public static final FeatureType[] FEATURE_HEADER = {
            FeatureType.CONTINUOUS, FeatureType.CONTINUOUS, FeatureType.DISCRETE, FeatureType.DISCRETE
        };

        private Map<Integer, Double> classPriorProbabilities = new HashMap<>();
        private Map<Integer, Map<Integer, Double>> featureMeans = new HashMap<>();
        private Map<Integer, Map<Integer, Double>> featureVariances = new HashMap<>();
        private Map<Integer, Map<Integer, Map<Object, Double>>> featureCategoryProbabilities = new HashMap<>();
        private Set<Integer> classes = new HashSet<>();
        private int totalSamples;

        public NaiveBayes() {
        }

        public void fit(Matrix X, Matrix y_gt) {
            Shape shape = X.getShape(); // Using the getShape() method from Matrix class
            totalSamples = shape.getNumRows(); // Using the getNumRows() method from Shape class
            int nFeatures = FEATURE_HEADER.length;

            // Initializing statistics data structures
            for (int featureIndex = 0; featureIndex < nFeatures; featureIndex++) {
                featureMeans.put(featureIndex, new HashMap<>());
                featureVariances.put(featureIndex, new HashMap<>());
                featureCategoryProbabilities.put(featureIndex, new HashMap<>());
            }

            Map<Integer, Integer> classCounts = new HashMap<>();

            for (int rowIndex = 0; rowIndex < totalSamples; rowIndex++) {
                int classLabel = (int) y_gt.get(rowIndex, 0);
                classes.add(classLabel);
                classCounts.put(classLabel, classCounts.getOrDefault(classLabel, 0) + 1);

                for (int featureIndex = 0; featureIndex < nFeatures; featureIndex++) {
                    double featureValue = X.get(rowIndex, featureIndex);
                    FeatureType featureType = FEATURE_HEADER[featureIndex];

                    Map<Integer, Double> means = featureMeans.get(featureIndex);
                    Map<Integer, Double> variances = featureVariances.get(featureIndex);
                    featureCategoryProbabilities.putIfAbsent(featureIndex, new HashMap<>());

                    switch (featureType) {
                        case CONTINUOUS:
                            means.merge(classLabel, featureValue, Double::sum);
                            variances.merge(classLabel, featureValue * featureValue, Double::sum);
                            break;
                        case DISCRETE:
                            featureCategoryProbabilities.get(featureIndex).computeIfAbsent(classLabel, k -> new HashMap<>()).merge(featureValue, 1.0, Double::sum);
                            break;
                    }
                }
            }

            finalizeStatistics(classCounts);
        }

        private void finalizeStatistics(Map<Integer, Integer> classCounts) {
            for (Integer classLabel : classCounts.keySet()) {
                double classCount = classCounts.get(classLabel);
                classPriorProbabilities.put(classLabel, classCount / totalSamples);

                for (int featureIndex = 0; featureIndex < FEATURE_HEADER.length; featureIndex++) {
                    if (FEATURE_HEADER[featureIndex] == FeatureType.CONTINUOUS) {
                        double sum = featureMeans.get(featureIndex).get(classLabel);
                        double squaredSum = featureVariances.get(featureIndex).get(classLabel);
                        double mean = sum / classCount;
                        double variance = squaredSum / classCount - mean * mean;

                        featureMeans.get(featureIndex).put(classLabel, mean);
                        featureVariances.get(featureIndex).put(classLabel, variance);
                    } else {
                        Map<Object, Double> frequencies = featureCategoryProbabilities.get(featureIndex).get(classLabel);
                        for (Map.Entry<Object, Double> entry : frequencies.entrySet()) {
                            frequencies.put(entry.getKey(), entry.getValue() / classCount);
                        }
                    }
                }
            }
        }

        public int predict(Matrix x) {
            double maxLogProb = Double.NEGATIVE_INFINITY;
            int bestClass = -1;

            for (Integer classLabel : classes) {
                double logProb = Math.log(classPriorProbabilities.get(classLabel));
                for (int featureIndex = 0; featureIndex < FEATURE_HEADER.length; featureIndex++) {
                    double featureValue = x.get(0, featureIndex);
                    FeatureType featureType = FEATURE_HEADER[featureIndex];

                    switch (featureType) {
                        case CONTINUOUS:
                            double mean = featureMeans.get(featureIndex).get(classLabel);
                            double variance = featureVariances.get(featureIndex).get(classLabel);
                            logProb += logGaussianProbability(featureValue, mean, variance);
                            break;
                        case DISCRETE:
                            Map<Object, Double> probabilities = featureCategoryProbabilities.get(featureIndex).get(classLabel);
                            double categoryProb = probabilities.getOrDefault(featureValue, 1.0 / (totalSamples + probabilities.size())); // Laplace smoothing
                            logProb += Math.log(categoryProb);
                            break;
                    }
                }

                if (logProb > maxLogProb) {
                    maxLogProb = logProb;
                    bestClass = classLabel;
                }
            }

            return bestClass;
        }

        private double logGaussianProbability(double value, double mean, double variance) {
            return -0.5 * Math.log(2 * Math.PI * variance) - (Math.pow(value - mean, 2) / (2 * variance));
        }
    }
    
    private NaiveBayes model;

    public NaiveBayesAgent(int playerNum, String[] args) {
        super(playerNum, args);
        this.model = new NaiveBayes();
    }

    public NaiveBayes getModel() {
        return this.model;
    }

    @Override
    public void train(Matrix X, Matrix y_gt) {
        this.getModel().fit(X, y_gt);
    }

    @Override
    public int predict(Matrix featureRowVector) {
        return this.getModel().predict(featureRowVector);
    }
}
