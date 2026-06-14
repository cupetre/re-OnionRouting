package MessagePackage;

import CryptoUtil.AesEncryption;
import CryptoUtil.KeyRegister;
import CryptoUtil.RsaEncryption;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.security.*;
import java.util.List;

public class OnionMessageBuilder {

    // provide structure
    private OnionMessageBuilder() {

    }

    private static OnionPacket encryptLayer (
            DecryptedLayer layer,
            PublicKey nodePublicKey
    ) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

        // first we do a checkup
        if ( layer == null ) {
            throw new IllegalArgumentException("Layer cannot be null");
        }

        // check if pubkey validity as well
        if ( nodePublicKey == null ) {
            throw new IllegalArgumentException("public key does not exist/error pubkey");
        }

        // encode the exisitng message + route ?
        byte[] encodedLayer = LayerCodec.encode(layer);
        // take whole package
        // gen pairs and both encryption
        SecretKey aesKey = AesEncryption.generateAesKey();

        AesEncryption.EncryptedData encryptedData =
                AesEncryption.encrypt(encodedLayer, aesKey);

        byte[] encryptedAesKey = RsaEncryption.encrypt(
                aesKey.getEncoded(),
                nodePublicKey
        );

        return new OnionPacket(encryptedAesKey,
                encryptedData.getIv(),
                encryptedData.getCiphertext());
    }

    public static OnionPacket buildDelivery(
            byte[] message,
            PublicKey destinationPublicKey
    ) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        DecryptedLayer deliveryLayer =
                DecryptedLayer.Deliver(message);

        return encryptLayer(
                deliveryLayer,
                destinationPublicKey
        );
    }
}
