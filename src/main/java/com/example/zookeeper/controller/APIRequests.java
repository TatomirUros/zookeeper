package com.example.zookeeper.controller;

import com.example.zookeeper.DTO.PredictionRequestDTO;
import com.example.zookeeper.service.ModelManager;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/model")
public class APIRequests {

    private final ModelManager modelManager;

    public APIRequests(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    /**
     * PUT endpoint za ažuriranje modela
     * - prima CSV fajl
     * - trenira model i čuva ga u ZooKeeper-u
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateModel(@RequestParam("file") MultipartFile file) {
        try {
            modelManager.updateModelFromCsv(file);
            return ResponseEntity.ok("Model uspešno ažuriran i objavljen preko ZooKeeper-a.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Greška pri ažuriranju modela: " + e.getMessage());
        }
    }

    /**
     * POST endpoint za predikciju
     * - prima JSON telo
     * - vraća predikciju koristeći model iz ZooKeeper-a
     */
    @PostMapping(value = "/predict", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> predict(@RequestBody PredictionRequestDTO request) {
        try {
            String result = modelManager.predict(request);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(503).body("Model nije dostupan: " + ise.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Greška pri predikciji: " + e.getMessage());
        }
    }
}
