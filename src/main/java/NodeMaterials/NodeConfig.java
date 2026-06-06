package NodeMaterials;

import java.util.Objects;

public class NodeConfig {

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private final String nodeID;
    // node network address ( could be considered to work like localhost )
    private final String host;
    private final int port;

    public NodeConfig(String nodeID, String host, int port) {
        this.nodeID = nodeID;
        this.host = host;
        this.port = port;

        if (nodeID == null || nodeID.isBlank()) {
            throw new IllegalArgumentException("Node ID cannot be blank");
        }

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host cannot be blank");
        }

        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException(
                    "Port must be between " + MIN_PORT + " and " + MAX_PORT
            );
        }
    }

    public String getNodeID() {
        return nodeID;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object object) {
        if ( this == object ) {
            return true;
        }

        if (!(object instanceof NodeConfig other)) {
            return false;
        }

        return port == other.port
                && nodeID.equals(other.nodeID)
                && host.equals(other.host);
    }

    // as long as the objects are the same, the same  length/hash code will be produced for them
    @Override
    public int hashCode() {
        return Objects.hash(nodeID, host, port);
    }

    // basic debugigng and logging done around the encryption for just in case tbh
    @Override
    public String toString() {
        return "NodeConfig{" +
                "nodeID='" + nodeID + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
