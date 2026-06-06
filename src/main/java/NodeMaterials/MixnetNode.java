package NodeMaterials;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Objects;

public class MixnetNode {

    final private NodeConfig config;
    final private KeyPair keyPair;

    public MixnetNode(NodeConfig config, KeyPair keyPair) {
        this.config = Objects.requireNonNull(config, "Config cant be null, sorry");
        this.keyPair = Objects.requireNonNull(keyPair, "Cant be a null either, yikes");
    }

    public String getNodeId() {
        return config.getNodeID();
    }

    public NodeConfig getConfig() {
        return config;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    // we dont make a get private key since its only used internally and it shouldn't be accessible

    public void processRelay() {
        // processing relay shuffle and forwarding
    }

    public void delivery() {
        // last node accepting, last layer decrypt, show message
    }

    public void response() {
        // api response for potrdilo xdd
    }
}
