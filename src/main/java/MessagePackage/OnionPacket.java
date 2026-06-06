package MessagePackage;

import java.util.Arrays;

public class OnionPacket {

    private static final int GCM_IV_LENGTH = 12;

    // this is the AES KEY that we wille encrypt in an RSA WRAP
    private final byte[] encryptedAesKey;
    // aditiion on the encryption ( crypto 101 )
    private final byte[] iv;
    // the message thats wrapped in encryption
    private final byte[] encryptedPayload;

    public OnionPacket(byte[] encryptedAesKey, byte[] iv, byte[] encryptedPayload) {
        if (encryptedAesKey == null || iv == null || encryptedPayload == null) {
            throw new IllegalArgumentException("Packet fields cannot be null");
        }

        if (encryptedAesKey.length == 0 || encryptedPayload.length == 0) {
            throw new IllegalArgumentException("Packet fields cannot be empty");
        }

        if (iv.length != GCM_IV_LENGTH) {
            throw new IllegalArgumentException("IV must be exactly 12 bytes");
        }

        this.encryptedAesKey = encryptedAesKey.clone();
        this.iv = iv;
        this.encryptedPayload = encryptedPayload;
    }

    public byte[] getEncryptedPayload() {
        return encryptedPayload.clone();
    }

    public byte[] getIv() {
        return iv;
    }

    public byte[] getEncryptedAesKey() {
        return encryptedAesKey.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }

        if ( !(obj instanceof OnionPacket other) ) {
            return false;
        }

        return Arrays.equals(encryptedAesKey, other.encryptedAesKey)
                && Arrays.equals(iv, other.iv)
                && Arrays.equals(encryptedPayload, other.encryptedPayload);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(encryptedAesKey);
        result = 31 * result + Arrays.hashCode(iv);
        result = 31 * result + Arrays.hashCode(encryptedPayload);
        return result;
    }

    @Override
    public String toString() {
        return "OnionPacket{" +
                "encryptedAesKeyLength=" + encryptedAesKey.length +
                ", ivLength=" + iv.length +
                ", encryptedPayloadLength=" + encryptedPayload.length +
                '}';
    }
}
