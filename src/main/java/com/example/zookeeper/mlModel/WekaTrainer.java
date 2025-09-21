package com.example.zookeeper.mlModel;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.CSVLoader;

import java.io.File;

public class WekaTrainer {

    // Trenira model i čuva ga na disk
    public static void trainAndSave(String csvPath, String modelPath) throws Exception {
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(csvPath));
        Instances data = loader.getDataSet();
        data.setClassIndex(data.numAttributes() - 1); // labela je poslednja kolona

        RandomForest rf = new RandomForest();
        rf.buildClassifier(data);

        SerializationHelper.write(modelPath, rf);
        System.out.println("Model sačuvan na: " + modelPath);
    }

    // Učitava model sa diska
    public static Classifier loadModel(String modelPath) throws Exception {
        return (Classifier) SerializationHelper.read(modelPath);
    }
}
