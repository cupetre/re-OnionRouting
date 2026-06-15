package MessagePackage;

import CryptoUtil.KeyRegister;
import CryptoUtil.RsaEncryption;
import NodeMaterials.MixnetNode;
import NodeMaterials.NodeConfig;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {

    @Test
    void newRouterShouldBeEmpty() {
        Router router = new Router();

        assertEquals(0, router.size());
    }

    @Test
    void registeringNodeShouldIncreaseSizeAndMakeItAvailable() throws Exception {
        Router router = new Router();
        MixnetNode node = createNode(
                "node-1",
                4444,
                RsaEncryption.generateKeyPair()
        );

        router.registerNode(node);

        assertEquals(1, router.size());
        assertTrue(router.containsNode("node-1"));
        assertFalse(router.containsNode("node-2"));
    }

    @Test
    void registeringNullNodeShouldBeRejected() {
        Router router = new Router();

        assertThrows(
                IllegalArgumentException.class,
                () -> router.registerNode(null)
        );
    }

    @Test
    void duplicateNodeIDShouldBeRejected() throws Exception {
        Router router = new Router();
        router.registerNode(createNode(
                "node-1",
                4444,
                RsaEncryption.generateKeyPair()
        ));

        assertThrows(
                IllegalStateException.class,
                () -> router.registerNode(createNode(
                        "node-1",
                        4445,
                        RsaEncryption.generateKeyPair()
                ))
        );
    }

    @Test
    void invalidStartingNodeIDShouldBeRejected() {
        Router router = new Router();
        OnionPacket packet = createDummyPacket();

        assertThrows(
                IllegalArgumentException.class,
                () -> router.route(null, packet)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> router.route(" ", packet)
        );
    }

    @Test
    void unknownStartingNodeShouldBeRejected() {
        Router router = new Router();

        assertThrows(
                NoSuchElementException.class,
                () -> router.route("unknown-node", createDummyPacket())
        );
    }

    @Test
    void nullPacketShouldBeRejected() {
        Router router = new Router();

        assertThrows(
                IllegalArgumentException.class,
                () -> router.route("node-1", null)
        );
    }

    @Test
    void oneNodeRouteShouldReturnFinalMessage() throws Exception {
        KeyPair nodeKeys = RsaEncryption.generateKeyPair();
        MixnetNode node = createNode("node-1", 4444, nodeKeys);
        Router router = new Router();
        router.registerNode(node);
        byte[] message = "One node".getBytes(StandardCharsets.UTF_8);
        OnionPacket packet = OnionMessageBuilder.buildDelivery(
                message,
                nodeKeys.getPublic()
        );

        byte[] deliveredMessage = router.route("node-1", packet);

        assertArrayEquals(message, deliveredMessage);
    }

    @Test
    void threeNodeRouteShouldReturnFinalMessage() throws Exception {
        KeyPair node1Keys = RsaEncryption.generateKeyPair();
        KeyPair node2Keys = RsaEncryption.generateKeyPair();
        KeyPair node3Keys = RsaEncryption.generateKeyPair();
        Router router = new Router();
        router.registerNode(createNode("node-1", 4444, node1Keys));
        router.registerNode(createNode("node-2", 4445, node2Keys));
        router.registerNode(createNode("node-3", 4446, node3Keys));
        KeyRegister register = createKeyRegister(
                node1Keys,
                node2Keys,
                node3Keys
        );
        byte[] message = "Three nodes".getBytes(StandardCharsets.UTF_8);
        OnionPacket packet = OnionMessageBuilder.buildOnion(
                message,
                List.of("node-1", "node-2", "node-3"),
                register
        );

        byte[] deliveredMessage = router.route("node-1", packet);

        assertArrayEquals(message, deliveredMessage);
    }

    @Test
    void missingIntermediateNodeShouldBeRejected() throws Exception {
        KeyPair node1Keys = RsaEncryption.generateKeyPair();
        KeyPair node2Keys = RsaEncryption.generateKeyPair();
        KeyPair node3Keys = RsaEncryption.generateKeyPair();
        Router router = new Router();
        router.registerNode(createNode("node-1", 4444, node1Keys));
        router.registerNode(createNode("node-3", 4446, node3Keys));
        KeyRegister register = createKeyRegister(
                node1Keys,
                node2Keys,
                node3Keys
        );
        OnionPacket packet = OnionMessageBuilder.buildOnion(
                "Hello".getBytes(StandardCharsets.UTF_8),
                List.of("node-1", "node-2", "node-3"),
                register
        );

        assertThrows(
                NoSuchElementException.class,
                () -> router.route("node-1", packet)
        );
    }

    @Test
    void startingAtWrongNodeShouldFailDecryption() throws Exception {
        KeyPair node1Keys = RsaEncryption.generateKeyPair();
        KeyPair node2Keys = RsaEncryption.generateKeyPair();
        Router router = new Router();
        router.registerNode(createNode("node-1", 4444, node1Keys));
        router.registerNode(createNode("node-2", 4445, node2Keys));
        KeyRegister register = new KeyRegister();
        register.register("node-1", node1Keys.getPublic());
        register.register("node-2", node2Keys.getPublic());
        OnionPacket packet = OnionMessageBuilder.buildOnion(
                "Hello".getBytes(StandardCharsets.UTF_8),
                List.of("node-1", "node-2"),
                register
        );

        assertThrows(
                BadPaddingException.class,
                () -> router.route("node-2", packet)
        );
    }

    @Test
    void containsNodeShouldRejectInvalidIDs() {
        Router router = new Router();

        assertThrows(
                IllegalArgumentException.class,
                () -> router.containsNode(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> router.containsNode(" ")
        );
    }

    private MixnetNode createNode(
            String nodeID,
            int port,
            KeyPair keyPair
    ) {
        return new MixnetNode(
                new NodeConfig(nodeID, "localhost", port),
                keyPair
        );
    }

    private KeyRegister createKeyRegister(
            KeyPair node1Keys,
            KeyPair node2Keys,
            KeyPair node3Keys
    ) {
        KeyRegister register = new KeyRegister();
        register.register("node-1", node1Keys.getPublic());
        register.register("node-2", node2Keys.getPublic());
        register.register("node-3", node3Keys.getPublic());
        return register;
    }

    private OnionPacket createDummyPacket() {
        return new OnionPacket(
                new byte[]{1},
                new byte[12],
                new byte[]{2}
        );
    }
}
