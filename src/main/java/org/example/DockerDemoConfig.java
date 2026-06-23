package org.example;

import CryptoUtil.KeyRegister;
import CryptoUtil.RsaEncryption;
import NodeMaterials.NodeConfig;
import NodeMaterials.NodeDirectory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class DockerDemoConfig {
    private static final String CONFIG_FILE = "docker-nodes.properties";
    private static final String SERVER_BIND_HOST = "0.0.0.0";

    private final Properties properties;
    private final List<String> route;

    public DockerDemoConfig() throws IOException {
        this.properties = loadProperties();
        this.route = Arrays.stream(requiredProperty("route").split(","))
                .map(String::trim)
                .filter(nodeID -> !nodeID.isBlank())
                .toList();
    }

    public List<String> getRoute() {
        return route;
    }

    public NodeDirectory createNodeDirectory() {
        NodeDirectory directory = new NodeDirectory();

        for (String nodeID : route) {
            directory.register(createNetworkConfig(nodeID));
        }

        return directory;
    }

    // generate the keys for eeach node in a map
    public KeyRegister createKeyRegister() throws Exception {
        KeyRegister keyRegister = new KeyRegister();

        for (String nodeID : route) {
            keyRegister.register(nodeID, publicKeyFor(nodeID));
        }

        return keyRegister;
    }

    // creating the network on disc
    public NodeConfig createNetworkConfig(String nodeID) {
        return new NodeConfig(
                nodeID,
                requiredProperty("node." + nodeID + ".host"),
                portFor(nodeID)
        );
    }

    // server starter
    public NodeConfig createServerConfig(String nodeID) {
        return new NodeConfig(
                nodeID,
                SERVER_BIND_HOST,
                portFor(nodeID)
        );
    }

    // assigning generated keys
    public KeyPair keyPairFor(String nodeID) throws Exception {
        PublicKey publicKey = publicKeyFor(nodeID);
        PrivateKey privateKey = RsaEncryption.privateKeyFromBase64(
                requiredProperty("node." + nodeID + ".privateKey")
        );

        return new KeyPair(publicKey, privateKey);
    }

    private PublicKey publicKeyFor(String nodeID) throws Exception {
        return RsaEncryption.publicKeyFromBase64(
                requiredProperty("node." + nodeID + ".publicKey")
        );
    }

    private int portFor(String nodeID) {
        return Integer.parseInt(requiredProperty("node." + nodeID + ".port"));
    }

    private String requiredProperty(String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing docker demo config value: " + key);
        }

        return value;
    }

    private Properties loadProperties() throws IOException {
        Properties loadedProperties = new Properties();

        try (InputStream inputStream = DockerDemoConfig.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IOException("Missing config file: " + CONFIG_FILE);
            }

            loadedProperties.load(inputStream);
        }

        return loadedProperties;
    }
}
