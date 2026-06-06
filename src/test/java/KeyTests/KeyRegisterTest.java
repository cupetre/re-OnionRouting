package KeyTests;

import CryptoUtil.KeyRegister;
import CryptoUtil.RsaEncryption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyRegisterTest {
    private static PublicKey firstPublicKey;
    private static PublicKey secondPublicKey;

    @BeforeAll
    static void generateTestKeys() throws Exception {
        firstPublicKey = RsaEncryption.generateKeyPair().getPublic();
        secondPublicKey = RsaEncryption.generateKeyPair().getPublic();
    }

    @Test
    void newRegisterShouldBeEmpty() {
        KeyRegister register = new KeyRegister();

        assertEquals(0, register.size());
    }

    @Test
    void registeringNodeShouldIncreaseSizeAndStoreKey() {
        KeyRegister register = new KeyRegister();

        register.register("node-1", firstPublicKey);

        assertEquals(1, register.size());
        assertEquals(firstPublicKey, register.getPublicKey("node-1"));
    }

    @Test
    void containsNodeShouldReportRegisteredAndUnknownNodes() {
        KeyRegister register = new KeyRegister();
        register.register("node-1", firstPublicKey);

        assertTrue(register.containsNode("node-1"));
        assertFalse(register.containsNode("node-2"));
    }

    @Test
    void duplicateNodeIDShouldBeRejected() {
        KeyRegister register = new KeyRegister();
        register.register("node-1", firstPublicKey);

        assertThrows(
                IllegalStateException.class,
                () -> register.register("node-1", secondPublicKey)
        );
    }

    @Test
    void nullNodeIDShouldBeRejected() {
        KeyRegister register = new KeyRegister();

        assertThrows(
                IllegalArgumentException.class,
                () -> register.register(null, firstPublicKey)
        );
    }

    @Test
    void blankNodeIDShouldBeRejected() {
        KeyRegister register = new KeyRegister();

        assertThrows(
                IllegalArgumentException.class,
                () -> register.register(" ", firstPublicKey)
        );
    }

    @Test
    void nullPublicKeyShouldBeRejected() {
        KeyRegister register = new KeyRegister();

        assertThrows(
                IllegalArgumentException.class,
                () -> register.register("node-1", null)
        );
    }

    @Test
    void unknownNodeLookupShouldBeRejected() {
        KeyRegister register = new KeyRegister();

        assertThrows(
                NoSuchElementException.class,
                () -> register.getPublicKey("unknown-node")
        );
    }

    @Test
    void removingNodeShouldMakeItUnavailable() {
        KeyRegister register = new KeyRegister();
        register.register("node-1", firstPublicKey);

        register.remove("node-1");

        assertEquals(0, register.size());
        assertFalse(register.containsNode("node-1"));
        assertThrows(
                NoSuchElementException.class,
                () -> register.getPublicKey("node-1")
        );
    }

    @Test
    void removingUnknownNodeShouldBeRejected() {
        KeyRegister register = new KeyRegister();

        assertThrows(
                NoSuchElementException.class,
                () -> register.remove("unknown-node")
        );
    }

    @Test
    void invalidIDsShouldBeRejectedByLookupAndContains() {
        KeyRegister register = new KeyRegister();

        assertThrows(IllegalArgumentException.class, () -> register.getPublicKey(null));
        assertThrows(IllegalArgumentException.class, () -> register.getPublicKey(" "));
        assertThrows(IllegalArgumentException.class, () -> register.containsNode(null));
        assertThrows(IllegalArgumentException.class, () -> register.containsNode(" "));
    }
}
