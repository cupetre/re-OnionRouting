package NodeMaterials;

import CryptoUtil.AesEncryption;
import CryptoUtil.RsaEncryption;
import Logs.LogLevel;
import Logs.Logger;
import MessagePackage.DecryptedLayer;
import MessagePackage.LayerCodec;
import MessagePackage.OnionPacket;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.util.Objects;

public class MixnetNode {

    final private NodeConfig config;
    final private KeyPair keyPair;

    public MixnetNode(NodeConfig config, KeyPair keyPair) {
        this.config = Objects.requireNonNull(config, "Config cant be null, sorry");
        this.keyPair = Objects.requireNonNull(keyPair, "Cant be a null either, yikes");
        Logger.log("Initialized mixnet node " + config.getNodeID() + " at " + config.getHost() + ":" + config.getPort(), LogLevel.Info);
    }

    public String getNodeId() {
        return config.getNodeID();
    }

    public NodeConfig getConfig() {
        return config;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    @Override
    public String toString() {
        return "MixnetNode{" +
                "config=" + config +
                '}';
    }

    // main function for decryption of the whole layer
    public DecryptedLayer decryptedLayer(
            OnionPacket packet) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {

        // check if alls good
        if (packet == null) {
            throw new IllegalArgumentException("Packet is empty or null , somethings wrong in passing the packet");
        }

        Logger.log("Node " + getNodeId() + " started decrypting one onion layer", LogLevel.Status);

        // if yes, first unlock with RSA , so we get to teh AES key
        // that encrypts the whole package
        byte[] rawAesKey = RsaEncryption.decrypt(
                packet.getEncryptedAesKey(),
                keyPair.getPrivate()
        );

        // pick up the secret key for the AES
        SecretKey secretAesKey = new SecretKeySpec(rawAesKey, "AES");

        // split the package in sections ( since IV is constant, we get payload only )
        AesEncryption.EncryptedData encryptedData =
                new AesEncryption.EncryptedData(
                        packet.getEncryptedPayload(),
                        packet.getIv()
                );

        // decrypt it
        byte[] encodedLayer =
                AesEncryption.decrypt(
                        encryptedData,
                        secretAesKey
                );

        // send out the decoded layer as well ( strings instead of bytes[] ) so it knows nextnodeid
        DecryptedLayer layer = LayerCodec.decode(encodedLayer);
        Logger.log("Node " + getNodeId() + " decoded layer type " + layer.getType(), LogLevel.Success);
        return layer;
    }

    // we dont make a get private key since its only used internally and it shouldn't be accessible
    public void processRelay() {
        // processing relay shuffle and forwarding
    }

    public void delivery() {
        // last node accepting, last layer decrypt, show message
    }

    public void response() {
        // api response for potrdilo xdd
    }
}
