package MessagePackage;

import CryptoUtil.AesEncryption;
import CryptoUtil.KeyRegister;
import CryptoUtil.RsaEncryption;
import Logs.LogLevel;
import Logs.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.security.*;
import java.util.List;

public class OnionMessageBuilder {

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
        Logger.log("Encoding " + layer.getType() + " layer", LogLevel.Debug);

        byte[] encodedLayer = LayerCodec.encode(layer);

        // take whole package
        // gen pairs and both encryption
        SecretKey aesKey = AesEncryption.generateAesKey();
        Logger.log("Generated AES key for one onion layer", LogLevel.Debug);

        AesEncryption.EncryptedData encryptedData =
                AesEncryption.encrypt(encodedLayer, aesKey);
        Logger.log("Encrypted one encoded onion layer with AES-GCM", LogLevel.Debug);

        byte[] encryptedAesKey = RsaEncryption.encrypt(
                aesKey.getEncoded(),
                nodePublicKey
        );
        Logger.log("Wrapped AES key with receiving RSA node public key", LogLevel.Debug);

        return new OnionPacket(encryptedAesKey,
                encryptedData.getIv(),
                encryptedData.getCiphertext());
    }

    // buildup for last node, aka the fisrt one in the sequence of ecndoing and encryption
    public static OnionPacket buildDelivery(
            byte[] message,
            PublicKey destinationPublicKey
    ) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Logger.log("Building final delivery layer", LogLevel.Status);

        DecryptedLayer deliveryLayer =
                DecryptedLayer.deliver(message);

        return encryptLayer(
                deliveryLayer,
                destinationPublicKey
        );
    }

    // after first/last node gets finished, the rest of the node gets wrapped
    public static OnionPacket wrapRelay(
            String nextNodeID, // which node's next
            OnionPacket innerPacket, // the packet for wrapping
            PublicKey relayPublicKey // the key to encrypt
    ) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Logger.log("Wrapping relay layer toward next node " + nextNodeID, LogLevel.Status);

        DecryptedLayer relayLayer =
                DecryptedLayer.relay(
                        nextNodeID,
                        innerPacket
                );

        return encryptLayer(
                relayLayer,
                relayPublicKey
                );
    }

    // now lets automate the whole process of wrapping and sending
    public static OnionPacket buildOnion(
            byte[] message,
            List<String> route,
            KeyRegister keyRegister
    ) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

        //first we start with validations and error checkups
        if ( message == null || message.length == 0 ) {
            throw new IllegalArgumentException(
                    "Message cannot be null or empty"
            );
        }

        if ( route == null || route.isEmpty() ) {
            throw new IllegalArgumentException(
                    "Route must contain at least one node"
            );
        }

        if (keyRegister == null) {
            throw new IllegalArgumentException(
                    "Key register cannot be null"
            );
        }

        Logger.log("Building onion route with " + route.size() + " node(s)", LogLevel.Status);

        // extract index and ID of last node
        int finalNode = route.size() - 1;
        String finalNodeID = route.get(finalNode);
        Logger.log("Preparing innermost delivery layer for " + finalNodeID, LogLevel.Info);

        // assign the public key for it as the first one
        PublicKey finalNodePublicKey =
                keyRegister.getPublicKey(finalNodeID);

        // package it and now start with wrapping relay
        OnionPacket packet = buildDelivery(
                message,
                finalNodePublicKey
        );

        // wrapping form back to front
        for ( int index = finalNode - 1; index >= 0 ; index-- ) {

            String currentNodeID = route.get(index);
            String nextNodeID = route.get(index + 1);

            Logger.log("Preparing relay wrapper for " + currentNodeID + " -> " + nextNodeID, LogLevel.Info);

            PublicKey currentNodePublicKey =
                    keyRegister.getPublicKey(currentNodeID);

            packet = wrapRelay(
                    nextNodeID,
                    packet,
                    currentNodePublicKey
            );
        }

        Logger.log("Finished building onion packet for entry node " + route.get(0), LogLevel.Success);
        return packet;
    }
}
