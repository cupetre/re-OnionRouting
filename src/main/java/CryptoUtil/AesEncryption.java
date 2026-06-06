package CryptoUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

public class AesEncryption {
    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE_BITS = 256;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static SecretKey generateAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE_BITS, SECURE_RANDOM);
        return keyGenerator.generateKey();
    }

    public static EncryptedData encrypt(byte[] plainText, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Objects.requireNonNull(plainText, "Plaintext cannot be null");
        Objects.requireNonNull(secretKey, "Secret key cannot be null");

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec gcmParSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParSpec);

        byte[] cipherTextWithTag = cipher.doFinal(plainText);
        return new EncryptedData(cipherTextWithTag, iv);
    }

    public static byte[] decrypt(EncryptedData encryptedData, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Objects.requireNonNull(encryptedData, "Encrypted data cannot be null");
        Objects.requireNonNull(secretKey, "Secret key cannot be null");

        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec gcmParaSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, encryptedData.getIv());
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParaSpec);
        return cipher.doFinal(encryptedData.getCiphertext());
    }

    public static class EncryptedData {
        private final byte[] ciphertext;
        private final byte[] iv;

        public EncryptedData(byte[] ciphertext, byte[] iv) {
            Objects.requireNonNull(ciphertext, "Ciphertext cannot be null");
            Objects.requireNonNull(iv, "IV cannot be null");

            if (iv.length != GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("IV must be exactly " + GCM_IV_LENGTH_BYTES + " bytes");
            }

            this.ciphertext = ciphertext.clone();
            this.iv = iv.clone();
        }

        public byte[] getCiphertext() {
            return ciphertext.clone();
        }

        public byte[] getIv() {
            return iv.clone();
        }
    }
}
