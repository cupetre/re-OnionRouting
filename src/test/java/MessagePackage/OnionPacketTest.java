package MessagePackage;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OnionPacketTest {

    @Test
    void packetsWithIdenticalArrayContentsShouldBeEqual() {
        OnionPacket first = createPacket();
        OnionPacket second = createPacket();

        assertEquals(first, second);
    }

    @Test
    void equalPacketsShouldHaveEqualHashCodes() {
        OnionPacket first = createPacket();
        OnionPacket second = createPacket();

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void modifyingConstructorArraysShouldNotModifyPacket() {
        byte[] encryptedAesKey = {1, 2, 3};
        byte[] iv = new byte[12];
        byte[] encryptedPayload = {4, 5, 6};
        OnionPacket packet = new OnionPacket(encryptedAesKey, iv, encryptedPayload);

        encryptedAesKey[0] = 99;
        iv[0] = 99;
        encryptedPayload[0] = 99;

        assertArrayEquals(new byte[]{1, 2, 3}, packet.getEncryptedAesKey());
        assertArrayEquals(new byte[12], packet.getIv());
        assertArrayEquals(new byte[]{4, 5, 6}, packet.getEncryptedPayload());
    }

    @Test
    void modifyingGetterResultsShouldNotModifyPacket() {
        OnionPacket packet = createPacket();

        byte[] returnedAesKey = packet.getEncryptedAesKey();
        byte[] returnedIv = packet.getIv();
        byte[] returnedPayload = packet.getEncryptedPayload();
        returnedAesKey[0] = 99;
        returnedIv[0] = 99;
        returnedPayload[0] = 99;

        assertArrayEquals(new byte[]{1, 2, 3}, packet.getEncryptedAesKey());
        assertArrayEquals(new byte[12], packet.getIv());
        assertArrayEquals(new byte[]{4, 5, 6}, packet.getEncryptedPayload());
    }

    @Test
    void toStringShouldNotExposeArrayContents() {
        OnionPacket packet = createPacket();
        String result = packet.toString();

        assertFalse(result.contains(Arrays.toString(packet.getEncryptedAesKey())));
        assertFalse(result.contains(Arrays.toString(packet.getIv())));
        assertFalse(result.contains(Arrays.toString(packet.getEncryptedPayload())));
    }

    private OnionPacket createPacket() {
        return new OnionPacket(
                new byte[]{1, 2, 3},
                new byte[12],
                new byte[]{4, 5, 6}
        );
    }
}
