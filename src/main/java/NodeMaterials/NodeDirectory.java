package NodeMaterials;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class NodeDirectory {
    private final Map<String, NodeConfig> nodes;

    public NodeDirectory() {
        this.nodes = new HashMap<>();
    }

    private void validateNodeID(String nodeID) {
        if ( nodeID == null || nodeID.isBlank() ) {
            throw new IllegalArgumentException("Node id cant be neither n ull or blank bruh, logic");
        }
    }

    public void register(NodeConfig config) {

        if ( config == null ) {
            throw new IllegalArgumentException("not existant check if tis actually an boject/node");
        }

        String newID = config.getNodeID();

        if (nodes.containsKey(newID)) {
            throw new IllegalStateException(
                    "Node already registered: " + newID
            );
        }

        nodes.put(newID, config);
    }

    public NodeConfig getConfig(String nodeID) {
        validateNodeID(nodeID);

        NodeConfig newConfig = nodes.get(nodeID);

        if ( newConfig == null ) {
            throw new NoSuchElementException(
                    "Unknown node: " + nodeID
            );        }

        return newConfig;

    }

    public boolean containsNode(String nodeID) {
        validateNodeID(nodeID);

        return nodes.containsKey(nodeID);
    }

    public int size() {
        return nodes.size();
    }
}
