package MessagePackage;

import CryptoUtil.AesEncryption;
import CryptoUtil.KeyRegister;
import CryptoUtil.RsaEncryption;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnionMessageBuilderTest {

    @Test
    void oneNodeOnionShouldContainDeliveryMessage() throws Exception {
        KeyPair destinationKeys = RsaEncryption.generateKeyPair();
        byte[] message = "Hello destination".getBytes(StandardCharsets.UTF_8);

        OnionPacket packet = OnionMessageBuilder.buildDelivery(
                message,
                destinationKeys.getPublic()
        );
        DecryptedLayer layer = decryptLayer(packet, destinationKeys);

        assertTrue(layer.isDeliver());
        assertArrayEquals(message, layer.getFinalMessage());
    }

    @Test
    void threeNodeOnionShouldRevealOneLayerAtATime() throws Exception {
        KeyPair node1Keys = RsaEncryption.generateKeyPair();
        KeyPair node2Keys = RsaEncryption.generateKeyPair();
        KeyPair node3Keys = RsaEncryption.generateKeyPair();
        KeyRegister register = new KeyRegister();
        register.register("node-1", node1Keys.getPublic());
        register.register("node-2", node2Keys.getPublic());
        register.register("node-3", node3Keys.getPublic());
        byte[] message = "Hidden message".getBytes(StandardCharsets.UTF_8);

        OnionPacket node1Packet = OnionMessageBuilder.buildOnion(
                message,
                List.of("node-1", "node-2", "node-3"),
                register
        );

        DecryptedLayer node1Layer = decryptLayer(node1Packet, node1Keys);
        assertTrue(node1Layer.isRelay());
        assertEquals("node-2", node1Layer.getNextNodeID());

        DecryptedLayer node2Layer =
                decryptLayer(node1Layer.getInnerPacket(), node2Keys);
        assertTrue(node2Layer.isRelay());
        assertEquals("node-3", node2Layer.getNextNodeID());

        DecryptedLayer node3Layer =
                decryptLayer(node2Layer.getInnerPacket(), node3Keys);
        assertTrue(node3Layer.isDeliver());
        assertArrayEquals(message, node3Layer.getFinalMessage());
    }

    @Test
    void buildOnionShouldRejectNullAndEmptyMessages() throws Exception {
        KeyRegister register = registerSingleNode();
        List<String> route = List.of("node-1");

        assertThrows(
                IllegalArgumentException.class,
                () -> OnionMessageBuilder.buildOnion(null, route, register)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> OnionMessageBuilder.buildOnion(new byte[0], route, register)
        );
    }

    @Test
    void buildOnionShouldRejectNullAndEmptyRoutes() {
        KeyRegister register = new KeyRegister();
        byte[] message = "Hello".getBytes(StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () -> OnionMessageBuilder.buildOnion(message, null, register)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> OnionMessageBuilder.buildOnion(message, List.of(), register)
        );
    }

    @Test
    void buildOnionShouldRejectNullKeyRegister() {
        byte[] message = "Hello".getBytes(StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () -> OnionMessageBuilder.buildOnion(
                        message,
                        List.of("node-1"),
                        null
                )
        );
    }

    @Test
    void buildOnionShouldRejectUnknownNode() {
        KeyRegister register = new KeyRegister();
        byte[] message = "Hello".getBytes(StandardCharsets.UTF_8);

        assertThrows(
                NoSuchElementException.class,
                () -> OnionMessageBuilder.buildOnion(
                        message,
                        List.of("unknown-node"),
                        register
                )
        );
    }

    private KeyRegister registerSingleNode() throws Exception {
        KeyRegister register = new KeyRegister();
        register.register(
                "node-1",
                RsaEncryption.generateKeyPair().getPublic()
        );
        return register;
    }

    private DecryptedLayer decryptLayer(
            OnionPacket packet,
            KeyPair nodeKeys
    ) throws Exception {
        byte[] rawAesKey = RsaEncryption.decrypt(
                packet.getEncryptedAesKey(),
                nodeKeys.getPrivate()
        );
        SecretKey aesKey = new SecretKeySpec(rawAesKey, "AES");

        AesEncryption.EncryptedData encryptedData =
                new AesEncryption.EncryptedData(
                        packet.getEncryptedPayload(),
                        packet.getIv()
                );
        byte[] encodedLayer =
                AesEncryption.decrypt(encryptedData, aesKey);

        return LayerCodec.decode(encodedLayer);
    }
}
