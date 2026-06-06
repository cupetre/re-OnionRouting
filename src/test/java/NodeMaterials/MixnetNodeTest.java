package NodeMaterials;

import CryptoUtil.RsaEncryption;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

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
}