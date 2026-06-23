package MessagePackage;

import Logs.LogLevel;
import Logs.Logger;
import NodeMaterials.MixnetNode;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class Router {
    private final Map<String, MixnetNode> nodes;

    public Router() {
        this.nodes = new HashMap<>();
    }

    public void registerNode(MixnetNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        if (nodes.containsKey(node.getNodeId())) {
            throw new IllegalStateException(
                    "Node already registered: " + node.getNodeId()
            );
        }

        nodes.put(node.getNodeId(), node);
        Logger.log("Router registered node " + node.getNodeId(), LogLevel.Info);
    }

    public boolean containsNode(String nodeID) {
        if ( nodeID == null || nodeID.isBlank() ) {
            throw new IllegalArgumentException("the node cant be null, something wrong with passed nodeID");
        }

        return nodes.containsKey(nodeID);
    }

    public int size() {
        return nodes.size();
    }

    private MixnetNode getNode(String nodeID) {
        if (nodeID == null || nodeID.isBlank()) {
            throw new IllegalArgumentException(
                    "Node ID cannot be null or blank"
            );
        }

        MixnetNode node = nodes.get(nodeID);

        if (node == null) {
            throw new NoSuchElementException(
                    "Unknown node: " + nodeID
            );
        }

        return node;
    }

    public byte[] route(
            String startingNodeID,
            OnionPacket packet
    ) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, InvalidKeyException {
        if ( startingNodeID == null || startingNodeID.isBlank() ) {
            throw new IllegalArgumentException("does not exist in hash");
        }

        if (packet == null) {
            throw new IllegalArgumentException("does not exist in hash");
        }

        String currentNodeID = startingNodeID;
        OnionPacket currentPacket = packet;
        int hopCount = 0;

        Logger.log("Router starting route at node " + startingNodeID, LogLevel.Status);

        while (true) {
            hopCount++;
            MixnetNode currentNode = getNode(currentNodeID);

            Logger.log("Router sending packet to " + currentNodeID + " at hop " + hopCount, LogLevel.Info);

            DecryptedLayer layer =
                    currentNode.decryptedLayer(currentPacket);

            if (layer.isDeliver()) {
                Logger.log("Router received delivery result from " + currentNodeID, LogLevel.Success);
                return layer.getFinalMessage();
            }

            Logger.log(currentNodeID + " requested forward to " + layer.getNextNodeID(), LogLevel.Status);
            currentNodeID = layer.getNextNodeID();
            currentPacket = layer.getInnerPacket();
        }
    }
}
