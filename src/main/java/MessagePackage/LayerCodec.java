package MessagePackage;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class LayerCodec {
    private static final byte RELAY_TYPE = 1;
    private static final byte DELIVERY_TYPE = 2;

    private static final int MAX_NODE_TO_BYTES = 256;
    private static final int MAX_ENCRYPTED_KEY_BYTES = 512;
    private static final int MAX_PAYLOAD_BYTES = 1_000_000;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    // here we convert the layer into bytes
    public static byte[] encode(DecryptedLayer layer) {
        if (layer == null)
            throw new IllegalArgumentException("Layter cannot be null");

        try {
            ByteArrayOutputStream byteOutput =
                    new ByteArrayOutputStream();

            DataOutputStream output =
                    new DataOutputStream(byteOutput);

            if (layer.isRelay()) {
                output.writeByte(RELAY_TYPE);
                encodeRelay(output, layer);
            } else {
                output.writeByte(DELIVERY_TYPE);
                encodeDelivery(output, layer);
            }

            output.flush();
            return byteOutput.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Could not encode decrypted layer", e);
        }
    }

    // and here we convert the bytes in to a layer
    // a little better said would be from byte[] -> DIS -> Java obj
    public static DecryptedLayer decode(byte[] encodedLayer) throws IOException {
        if (encodedLayer == null || encodedLayer.length == 0) {
            throw new IllegalArgumentException(
                    "Encoded layer cannot be null or empty"
            );
        }

        ByteArrayInputStream byteInput =
                new ByteArrayInputStream(encodedLayer);

        DataInputStream input =
                new DataInputStream(byteInput);

        byte type = input.readByte();

        if (input.available() != 0) {
            throw new IllegalArgumentException(
                    "Encoded layer contains unexpected extra bytes"
            );
        }

        DecryptedLayer layer;

        if ( type == RELAY_TYPE ) {
            layer = decodeRelay(input);
        }

         else if ( type == DELIVERY_TYPE ) {
            layer = decodeDelivery(input);
        }

          else {
            throw new IllegalArgumentException("Unknown Layer type: " + type);
        }

          return layer;
    }

    private static DecryptedLayer decodeDelivery(DataInputStream input) throws IOException {
        byte[] message = readByteArray(
                input,
                MAX_PAYLOAD_BYTES,
                "Final Message"
        );

        return DecryptedLayer.Deliver(message);
    }

    private static DecryptedLayer decodeRelay(DataInputStream input) throws IOException {
        // now since our bytearray has the LENGTH of the nextnodeID
        // and then the nextnodeID actual byte[] , we need to extract it
        // and see which it is , convert it to string, continue the process

        byte[] nodeIDBytes = readByteArray(
                input,
                MAX_NODE_TO_BYTES,
                "next node ID"
        );

        String nextNodeID = new String(nodeIDBytes, StandardCharsets.UTF_8);

        byte[] encryptedAesKey = readByteArray(
                input,
                MAX_ENCRYPTED_KEY_BYTES,
                "Encrypted Aes Key"
        );

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        input.readFully(iv);

        byte[] encryptedPayload = readByteArray(
                input,
                MAX_PAYLOAD_BYTES,
                "Encrypted Payload"
        );

        // store up everything together and send it away it gooooooooez
        OnionPacket innerPacket = new OnionPacket(
                encryptedAesKey,
                iv,
                encryptedPayload);

        return DecryptedLayer.relay(
                nextNodeID,
                innerPacket
        );
    }

    private static byte[] readByteArray(DataInputStream input, int maxNodeToBytes, String nextNodeId) throws IOException {
        //first length as we said
        // as we did in the encoding for output.writeInt(data.length), we now do the opposite
        int length = input.readInt();

        if ( length <= 0 ) {
            throw new IllegalArgumentException(nextNodeId + "cant be empty ");
        }

        if ( length > maxNodeToBytes ) {
            throw new IllegalArgumentException(nextNodeId + "over the top length yikes");
        }

        byte[] data = new byte[length];
        // as we did int he encoding for output.write(data), well, you get it
        input.readFully(data);

        return data;
    }

    private static void encodeDelivery(DataOutputStream output, DecryptedLayer layer) throws IOException {
        byte[] message = layer.getFinalMessage();

        if ( message.length > MAX_PAYLOAD_BYTES ) {
            throw new IllegalArgumentException(" Final message is too large");
        }

        writeByteArray(output, message);
    }

    private static void encodeRelay(DataOutputStream output, DecryptedLayer layer) throws IOException {

        // one of the things we transfer is the next ndoes ID , and we convert that to bytes
        byte[] nodeIDBytes = layer
                .getNextNodeID()
                .getBytes(StandardCharsets.UTF_8);

        // we check whether its within our justified range
        if (nodeIDBytes.length > MAX_NODE_TO_BYTES) {
            throw new IllegalArgumentException("Node ID Exceepds the maximum encoding size");
        }

        // here we form the packet we send
        OnionPacket packet = layer.getInnerPacket();

        // through the functions we have in our onion packet
        // , we form the encryption message in bytes
        byte[] encryptedAesKey = packet.getEncryptedAesKey();
        byte[] iv = packet.getIv();
        byte[] encryptedPayload = packet.getEncryptedPayload();

        // again check for length
        if ( encryptedAesKey.length > MAX_ENCRYPTED_KEY_BYTES ) {
            throw new IllegalArgumentException(
                    "Encrypted AES key exceeds maximum size"
            );
        }

        if (encryptedPayload.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException(
                    "Encrypted payload exceeds maximum size"
            );
        }

        // if all good, send it out
        writeByteArray(output, nodeIDBytes);
        writeByteArray(output, encryptedAesKey);

        // we use this differently since we dont need to set errorChecks for the IV
        // it'll always be 12 bytes, hence we just add it manually to the whole array
        // for the other ones we actually check through the functions whether the length
        // of the encoded bytes is okey, if its fits, and tells us info about
        // where it starts from to where it goes to (ends to)
        output.write(iv);

        writeByteArray(output, encryptedPayload);
    }

    private static void writeByteArray(DataOutputStream output, byte[] data) throws IOException {
        output.writeInt(data.length);
        output.write(data);
    }

}
