package com.example.zookeeper.mlModel;

import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class WekaTest {
    public static void main(String[] args) throws Exception {
        String csvPath = "iris.csv";
        String modelPath = "iris.model";

        // 1. Trenira i sačuvaj model
        WekaTrainer.trainAndSave(csvPath, modelPath);

        // 2. Učitaj dataset (da bi napravio "template" Instances)
        Instances dataset = new weka.core.converters.CSVLoader() {{
            setSource(new java.io.File(csvPath));
        }}.getDataSet();
        dataset.setClassIndex(dataset.numAttributes() - 1);

        // 3. Učitaj model
        var model = WekaTrainer.loadModel(modelPath);

        // 4. Napravi novi primer (isti redosled kolona kao dataset bez klase!)
        Instance novi = new DenseInstance(dataset.numAttributes());
        novi.setDataset(dataset);
        novi.setValue(0, 5.1); // sepallength
        novi.setValue(1, 3.5); // sepalwidth
        novi.setValue(2, 1.4); // petallength
        novi.setValue(3, 0.2); // petalwidth

        double predikcija = model.classifyInstance(novi);
        String predKlasa = dataset.classAttribute().value((int) predikcija);

        System.out.println("Predikcija klase: " + predKlasa);
    }
}
