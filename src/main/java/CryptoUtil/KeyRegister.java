package CryptoUtil;

import Logs.LogLevel;
import Logs.Logger;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class KeyRegister {
    private final Map<String, PublicKey> publicKeys;

    public KeyRegister() {
        this.publicKeys = new HashMap<>();
    }

    public void register(String nodeID, PublicKey publicKey) {
        validateNodeID(nodeID);

        if (publicKey == null) {
            throw new IllegalArgumentException("Public key cannot be null");
        }

        if (publicKeys.containsKey(nodeID)) {
            throw new IllegalStateException("Node is already registered: " + nodeID);
        }

        publicKeys.put(nodeID, publicKey);
        Logger.log("Registered public key for node " + nodeID, LogLevel.Info);
    }

    public PublicKey getPublicKey(String nodeID) {
        validateNodeID(nodeID);

        PublicKey publicKey = publicKeys.get(nodeID);

        if (publicKey == null) {
            throw new NoSuchElementException("No public key registered for node: " + nodeID);
        }

        return publicKey;
    }

    public boolean containsNode(String nodeID) {
        validateNodeID(nodeID);
        return publicKeys.containsKey(nodeID);
    }

    public int size() {
        return publicKeys.size();
    }

    public void remove(String nodeID) {
        validateNodeID(nodeID);

        if (!publicKeys.containsKey(nodeID)) {
            throw new NoSuchElementException("Node is not registered: " + nodeID);
        }

        publicKeys.remove(nodeID);
        Logger.log("Removed public key for node " + nodeID, LogLevel.Info);
    }

    private void validateNodeID(String nodeID) {
        if (nodeID == null || nodeID.isBlank()) {
            throw new IllegalArgumentException("Node ID cannot be null or blank");
        }
    }
}
