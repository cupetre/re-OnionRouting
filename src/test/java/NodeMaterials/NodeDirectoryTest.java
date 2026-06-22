package NodeMaterials;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeDirectoryTest {

    @Test
    void newDirectoryShouldBeEmpty() {
        NodeDirectory directory = new NodeDirectory();

        assertEquals(0, directory.size());
    }

    @Test
    void registeringConfigShouldIncreaseSizeAndMakeNodeAvailable() {
        NodeDirectory directory = new NodeDirectory();
        NodeConfig config = new NodeConfig("node-1", "localhost", 4444);

        directory.register(config);

        assertEquals(1, directory.size());
        assertTrue(directory.containsNode("node-1"));
        assertFalse(directory.containsNode("node-2"));
    }

    @Test
    void getConfigShouldReturnRegisteredConfig() {
        NodeDirectory directory = new NodeDirectory();
        NodeConfig config = new NodeConfig("node-1", "localhost", 4444);
        directory.register(config);

        assertEquals(config, directory.getConfig("node-1"));
    }

    @Test
    void registerShouldRejectNullConfig() {
        NodeDirectory directory = new NodeDirectory();

        assertThrows(
                IllegalArgumentException.class,
                () -> directory.register(null)
        );
    }

    @Test
    void registerShouldRejectDuplicateNodeID() {
        NodeDirectory directory = new NodeDirectory();
        directory.register(new NodeConfig("node-1", "localhost", 4444));

        assertThrows(
                IllegalStateException.class,
                () -> directory.register(
                        new NodeConfig("node-1", "localhost", 4445)
                )
        );
    }

    @Test
    void lookupsShouldRejectNullAndBlankNodeIDs() {
        NodeDirectory directory = new NodeDirectory();

        assertThrows(IllegalArgumentException.class, () -> directory.getConfig(null));
        assertThrows(IllegalArgumentException.class, () -> directory.getConfig(" "));
        assertThrows(IllegalArgumentException.class, () -> directory.containsNode(null));
        assertThrows(IllegalArgumentException.class, () -> directory.containsNode(" "));
    }

    @Test
    void getConfigShouldRejectUnknownNode() {
        NodeDirectory directory = new NodeDirectory();

        assertThrows(
                NoSuchElementException.class,
                () -> directory.getConfig("unknown-node")
        );
    }
}
