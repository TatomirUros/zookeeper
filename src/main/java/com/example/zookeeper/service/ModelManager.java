package com.example.zookeeper.service;

import com.example.zookeeper.DTO.PredictionRequestDTO;
import com.example.zookeeper.mlModel.WekaTrainer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ModelManager {
    private final ZkService zkService;
    private final AtomicReference<Classifier> activeModel = new AtomicReference<>();
    private final Path modelFile;
    private final Path csvFile;

    public ModelManager(ZkService zkService,
                        @Value("${model.local.filename:iris.model}") String modelFilename,
                        @Value("${model.local.csvname:iris.csv}") String csvFilename) {
        this.zkService = zkService;
        this.modelFile = Paths.get(System.getProperty("java.io.tmpdir"), modelFilename);
        this.csvFile = Paths.get(System.getProperty("java.io.tmpdir"), csvFilename);
    }

    @PostConstruct
    public void init() {
        // register listener for ZK model changes
        zkService.registerModelListener(this::onZkModelChange);

        // try to load local model first (if exists)
        try {
            if (Files.exists(modelFile)) {
                Classifier c = WekaTrainer.loadModel(modelFile.toString());
                activeModel.set(c);
                System.out.println("ModelManager: Loaded local model from " + modelFile.toString());
            } else {
                // if ZK already has model, zkService start() will notify and onZkModelChange will fire
                System.out.println("ModelManager: no local model at startup");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @PreDestroy
    public void shutdown() {
        // nothing special (ZkService closes)
    }

    // called when this instance receives a new model via ZK (modelBytes written to ZK)
    private void onZkModelChange(byte[] modelBytes) {
        try {
            System.out.println("ModelManager: received model bytes from ZK, writing to local file...");
            Files.write(modelFile, modelBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // also try to fetch CSV bytes (so we can create Instances)
            try {
                byte[] csvBytes = zkService.getCsvBytes();
                if (csvBytes != null) {
                    Files.write(csvFile, csvBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    System.out.println("ModelManager: wrote training CSV from ZK to " + csvFile);
                }
            } catch (Exception e) {
                System.out.println("ModelManager: could not fetch CSV from ZK: " + e.getMessage());
            }

            // load model object
            Classifier c = WekaTrainer.loadModel(modelFile.toString());
            activeModel.set(c);
            System.out.println("ModelManager: active model updated from ZK.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // called by controller on PUT (we get MultipartFile)
    public void updateModelFromCsv(MultipartFile uploaded) throws Exception {
        // 1) save CSV locally (tmp)
        Files.createDirectories(csvFile.getParent());
        try (FileOutputStream fos = new FileOutputStream(csvFile.toFile())) {
            fos.write(uploaded.getBytes());
        }
        System.out.println("ModelManager: CSV saved to " + csvFile.toString());

        // 2) train and save model locally
        WekaTrainer.trainAndSave(csvFile.toString(), modelFile.toString());
        System.out.println("ModelManager: trained model saved to " + modelFile.toString());

        // 3) load model into memory (atomic swap)
        Classifier c = WekaTrainer.loadModel(modelFile.toString());
        activeModel.set(c);

        // 4) publish to ZooKeeper (model bytes + csv bytes)
        byte[] modelBytes = Files.readAllBytes(modelFile);
        byte[] csvBytes = Files.readAllBytes(csvFile);
        zkService.publishModel(modelBytes, csvBytes);
        System.out.println("ModelManager: published model + csv to ZooKeeper.");
    }

    public String predict(PredictionRequestDTO req) throws Exception {
        Classifier model = activeModel.get();
        if (model == null) {
            throw new IllegalStateException("No model loaded on this instance");
        }

        // Build Instances template from csvFile (must exist)
        if (!Files.exists(csvFile)) {
            throw new IllegalStateException("Training CSV not found on this instance: " + csvFile.toString());
        }

        CSVLoader loader = new CSVLoader();
        loader.setSource(csvFile.toFile());
        Instances dataset = loader.getDataSet();
        dataset.setClassIndex(dataset.numAttributes() - 1);

        Instance inst = new DenseInstance(dataset.numAttributes());
        inst.setDataset(dataset);
        inst.setValue(0, req.sepalLength);
        inst.setValue(1, req.sepalWidth);
        inst.setValue(2, req.petalLength);
        inst.setValue(3, req.petalWidth);

        double index = model.classifyInstance(inst);
        return dataset.classAttribute().value((int) index);
    }
}
