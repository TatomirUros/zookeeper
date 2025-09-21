package com.example.zookeeper.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Service
public class ZkService {
    private CuratorFramework client;
    private NodeCache modelCache;

    private final List<Consumer<byte[]>> modelListeners = new CopyOnWriteArrayList<>();

    @Value("${zk.connect-string}")
    private String zkConnectString;

    private static final String MODEL_ZNODE = "/ml/model";
    private static final String CSV_ZNODE = "/ml/model_csv";

    @PostConstruct
    public void start() throws Exception {
        client = CuratorFrameworkFactory.newClient(
                zkConnectString,
                new ExponentialBackoffRetry(1000, 3)
        );
        client.start();

        modelCache = new NodeCache(client, MODEL_ZNODE);
        modelCache.getListenable().addListener(() -> {
            ChildData current = modelCache.getCurrentData();
            if (current != null) {
                byte[] modelBytes = current.getData();
                // optionally fetch csv bytes (if present)
                try {
                    byte[] csvBytes = null;
                    if (client.checkExists().forPath(CSV_ZNODE) != null) {
                        csvBytes = client.getData().forPath(CSV_ZNODE);
                    }
                    // SAVE CSV LOCALLY is responsibility of ModelManager; we pass modelBytes and ModelManager will fetch csv via getCsvBytes()
                } catch (Exception ex) {
                    // ignore here; ModelManager can try to fetch csv if needed
                }

                // notify listeners
                for (Consumer<byte[]> l : modelListeners) {
                    try { l.accept(modelBytes); } catch (Exception ignore) {}
                }
            }
        });
        modelCache.start(true);

        // If there's already a model in ZK at startup, fire an initial notify
        if (client.checkExists().forPath(MODEL_ZNODE) != null) {
            byte[] modelBytes = client.getData().forPath(MODEL_ZNODE);
            for (Consumer<byte[]> l : modelListeners) {
                try { l.accept(modelBytes); } catch (Exception ignore) {}
            }
        }
    }

    @PreDestroy
    public void stop() {
        try { if (modelCache != null) modelCache.close(); } catch (Exception ignored) {}
        try { if (client != null) client.close(); } catch (Exception ignored) {}
    }

    public void registerModelListener(Consumer<byte[]> listener) {
        modelListeners.add(listener);
    }

    public byte[] getCsvBytes() throws Exception {
        if (client.checkExists().forPath(CSV_ZNODE) != null) {
            return client.getData().forPath(CSV_ZNODE);
        }
        return null;
    }

    public void publishModel(byte[] modelBytes, byte[] csvBytes) throws Exception {
        if (client.checkExists().forPath(MODEL_ZNODE) == null) {
            client.create().creatingParentsIfNeeded().forPath(MODEL_ZNODE, modelBytes);
        } else {
            client.setData().forPath(MODEL_ZNODE, modelBytes);
        }

        if (csvBytes != null) {
            if (client.checkExists().forPath(CSV_ZNODE) == null) {
                client.create().creatingParentsIfNeeded().forPath(CSV_ZNODE, csvBytes);
            } else {
                client.setData().forPath(CSV_ZNODE, csvBytes);
            }
        }
    }

    // helper to read raw model node (if necessary)
    public byte[] getModelBytes() throws Exception {
        if (client.checkExists().forPath(MODEL_ZNODE) != null) {
            return client.getData().forPath(MODEL_ZNODE);
        }
        return null;
    }

}
