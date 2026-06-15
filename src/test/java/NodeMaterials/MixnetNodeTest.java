package NodeMaterials;

import CryptoUtil.KeyRegister;
import CryptoUtil.RsaEncryption;
import MessagePackage.DecryptedLayer;
import MessagePackage.OnionMessageBuilder;
import MessagePackage.OnionPacket;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MixnetNodeTest {

    @Test
    void shouldExposeIdentityAndPublicKey() throws Exception {
        NodeConfig config =
                new NodeConfig("node-A", "localhost", 4444);

        KeyPair keyPair = RsaEncryption.generateKeyPair();
        MixnetNode node = new MixnetNode(config, keyPair);

        assertEquals("node-A", node.getNodeId());
        assertEquals(config, node.getConfig());
        assertEquals(keyPair.getPublic(), node.getPublicKey());
    }

    @Test
    void shouldRejectNullConfiguration() throws Exception {
        KeyPair keyPair = RsaEncryption.generateKeyPair();

        assertThrows(
                NullPointerException.class,
                () -> new MixnetNode(null, keyPair)
        );
    }

    @Test
    void shouldRejectNullKeyPair() {
        NodeConfig config =
                new NodeConfig("node-A", "localhost", 4444);

        assertThrows(
                NullPointerException.class,
                () -> new MixnetNode(config, null)
        );
    }

    @Test
    void toStringShouldNotExposePrivateKey() throws Exception {
        NodeConfig config =
                new NodeConfig("node-A", "localhost", 4444);

        KeyPair keyPair = RsaEncryption.generateKeyPair();
        MixnetNode node = new MixnetNode(config, keyPair);

        String result = node.toString();

        assertTrue(result.contains("node-A"));
        assertFalse(result.contains(
                keyPair.getPrivate().toString()
        ));
    }

    @Test
    void destinationNodeShouldDecryptFinalMessage() throws Exception {
        KeyPair keyPair = RsaEncryption.generateKeyPair();
        MixnetNode node = createNode("node-1", 4444, keyPair);
        byte[] message = "Hello destination".getBytes(StandardCharsets.UTF_8);
        OnionPacket packet = OnionMessageBuilder.buildDelivery(
                message,
                keyPair.getPublic()
        );

        DecryptedLayer layer = node.decryptedLayer(packet);

        assertTrue(layer.isDeliver());
        assertArrayEquals(message, layer.getFinalMessage());
    }

    @Test
    void relayNodeShouldDecryptNextHopAndInnerPacket() throws Exception {
        KeyPair node1Keys = RsaEncryption.generateKeyPair();
        KeyPair node2Keys = RsaEncryption.generateKeyPair();
        MixnetNode node1 = createNode("node-1", 4444, node1Keys);
        KeyRegister register = new KeyRegister();
        register.register("node-1", node1Keys.getPublic());
        register.register("node-2", node2Keys.getPublic());
        OnionPacket packet = OnionMessageBuilder.buildOnion(
                "Hello".getBytes(StandardCharsets.UTF_8),
                List.of("node-1", "node-2"),
                register
        );

        DecryptedLayer layer = node1.decryptedLayer(packet);

        assertTrue(layer.isRelay());
        assertEquals("node-2", layer.getNextNodeID());
        assertNotNull(layer.getInnerPacket());
    }

    @Test
    void nodeShouldRejectPacketEncryptedForAnotherNode() throws Exception {
        KeyPair intendedNodeKeys = RsaEncryption.generateKeyPair();
        KeyPair wrongNodeKeys = RsaEncryption.generateKeyPair();
        MixnetNode wrongNode =
                createNode("wrong-node", 4445, wrongNodeKeys);
        OnionPacket packet = OnionMessageBuilder.buildDelivery(
                "Hello".getBytes(StandardCharsets.UTF_8),
                intendedNodeKeys.getPublic()
        );

        assertThrows(
                BadPaddingException.class,
                () -> wrongNode.decryptedLayer(packet)
        );
    }

    @Test
    void nodeShouldRejectTamperedEncryptedPayload() throws Exception {
        KeyPair keyPair = RsaEncryption.generateKeyPair();
        MixnetNode node = createNode("node-1", 4444, keyPair);
        OnionPacket original = OnionMessageBuilder.buildDelivery(
                "Hello".getBytes(StandardCharsets.UTF_8),
                keyPair.getPublic()
        );
        byte[] tamperedPayload = original.getEncryptedPayload();
        tamperedPayload[0] ^= 1;
        OnionPacket tamperedPacket = new OnionPacket(
                original.getEncryptedAesKey(),
                original.getIv(),
                tamperedPayload
        );

        assertThrows(
                AEADBadTagException.class,
                () -> node.decryptedLayer(tamperedPacket)
        );
    }

    @Test
    void nodeShouldRejectNullPacket() throws Exception {
        MixnetNode node = createNode(
                "node-1",
                4444,
                RsaEncryption.generateKeyPair()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> node.decryptedLayer(null)
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
}
