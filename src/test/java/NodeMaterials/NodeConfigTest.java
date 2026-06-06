package NodeMaterials;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NodeConfigTest {

    @Test
    void shouldCreateValidConfiguration() {
        NodeConfig config = new NodeConfig("node-A", "localhost", 4444);

        assertEquals("node-A", config.getNodeID());
        assertEquals("localhost", config.getHost());
        assertEquals(4444, config.getPort());
    }

    @Test
    void identicalConfigurationsShouldBeEqual() {
        NodeConfig first =
                new NodeConfig("node-A", "localhost", 4444);

        NodeConfig second =
                new NodeConfig("node-A", "localhost", 4444);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void configurationsWithDifferentPortsShouldNotBeEqual() {
        NodeConfig first =
                new NodeConfig("node-A", "localhost", 4444);

        NodeConfig second =
                new NodeConfig("node-A", "localhost", 4445);

        assertNotEquals(first, second);
    }

    @Test
    void shouldRejectBlankNodeID() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NodeConfig(" ", "localhost", 4444)
        );
    }

    @Test
    void shouldRejectBlankHost() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NodeConfig("node-A", " ", 4444)
        );
    }

    @Test
    void shouldRejectPortBelowRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NodeConfig("node-A", "localhost", 0)
        );
    }

    @Test
    void shouldRejectPortAboveRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NodeConfig("node-A", "localhost", 65536)
        );
    }

}
