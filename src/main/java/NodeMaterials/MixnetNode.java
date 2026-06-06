package NodeMaterials;

import java.security.KeyPair;

public class MixnetNode {

    NodeConfig config;
    KeyPair keyPair;

    public MixnetNode(NodeConfig config, KeyPair keyPair) {
        this.config = config;
        this.keyPair = keyPair;
    }

    public String getNodeId() {
        return config.getNodeID();
    }

    public NodeConfig getConfig() {
        return config;
    }

    public void getPublicKey() {

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
