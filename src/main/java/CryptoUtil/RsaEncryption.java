package CryptoUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

public class RsaEncryption {
    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE_BITS = 2048;
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final OAEPParameterSpec OAEP_PARAMETERS = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(KEY_SIZE_BITS, SECURE_RANDOM);
        return keyPairGenerator.generateKeyPair();
    }

    public static byte[] encrypt(byte[] plainText, PublicKey publicKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        Objects.requireNonNull(plainText, "Plaintext cannot be null");
        Objects.requireNonNull(publicKey, "Public key cannot be null");

        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_PARAMETERS, SECURE_RANDOM);
        return cipher.doFinal(plainText);
    }

    public static byte[] decrypt(byte[] cipherText, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        Objects.requireNonNull(cipherText, "Ciphertext cannot be null");
        Objects.requireNonNull(privateKey, "Private key cannot be null");

        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_PARAMETERS);
        return cipher.doFinal(cipherText);
    }

    public static String publicKeyToBase64(PublicKey publicKey) {
        Objects.requireNonNull(publicKey, "Public key cannot be null");
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static PublicKey publicKeyFromBase64(String encodedPublicKey) throws Exception {
        Objects.requireNonNull(encodedPublicKey, "Encoded public key cannot be null");

        byte[] keyBytes = Base64.getDecoder().decode(encodedPublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);

        return keyFactory.generatePublic(keySpec);
    }

    public static PrivateKey privateKeyFromBase64(String encodedPrivateKey) throws Exception {
        Objects.requireNonNull(encodedPrivateKey, "Encoded private key cannot be null");

        byte[] keyBytes = Base64.getDecoder().decode(encodedPrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);

        return keyFactory.generatePrivate(keySpec);
    }
}
