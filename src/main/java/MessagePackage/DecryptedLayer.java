package MessagePackage;

import Logs.LogLevel;
import Logs.Logger;

public class DecryptedLayer {
    private final LayerType type;
    private final String nextNodeID;
    private final OnionPacket innerPacket;
    private final byte[] finalMessage;

    //we keep it private so invalid field combinations cant be easily created
    private DecryptedLayer(LayerType type, String nextNodeID, OnionPacket innerPacket, byte[] finalMessage) {
        this.type = type;
        this.nextNodeID = nextNodeID;
        this.innerPacket = innerPacket;
        this.finalMessage = finalMessage;
    }

    // every node except the delivery works as a relay since it gets, wraps, and sends
    // the message the last node should get, hence how we keep anonimity
    // between the nodes that resend the message, only the next step is known to every node, nothing else
    public static DecryptedLayer relay(
            String nextNodeID,
            OnionPacket innerPacket
    ) {
        if ( nextNodeID == null || nextNodeID.isBlank() ) {
            throw new IllegalArgumentException("null/blank");
        }

        if ( innerPacket == null ) {
            throw new IllegalArgumentException("the inner packet is null");
        }

        Logger.log("Created relay instruction for next node " + nextNodeID, LogLevel.Debug);

        return new DecryptedLayer(
                LayerType.RELAY,
                nextNodeID,
                innerPacket,
                null
        );
    }

    // instructions for the last node, aka , the first step of the proces
    // we tell the last node that its last, and it only waits for a delivery
    // aka it only gets the fnal message and thats it
    public static DecryptedLayer deliver(byte[] finalMessage) {
        if ( finalMessage == null || finalMessage.length == 0 ) {
            throw new IllegalArgumentException(" the final message is empty as shit");
        }
        Logger.log("Created delivery instruction", LogLevel.Debug);

        return new DecryptedLayer(
                LayerType.DELIVER,
                null,
                null,
                finalMessage
        );
    }

    public byte[] getFinalMessage() {
        if ( !isDeliver() ) {
            throw new IllegalArgumentException(" relay does not have this function");
        }

        return finalMessage.clone();
    }

    public OnionPacket getInnerPacket() {
        if ( !isRelay() ) {
            throw new IllegalArgumentException(" delivery does not have this function");
        }

        return innerPacket;
    }

    public String getNextNodeID() {
        if ( !isRelay() ) {
            throw new IllegalArgumentException(" delivery does not have this function");
        }

        return nextNodeID;
    }

    public LayerType getType() {
        return type;
    }

    public boolean isRelay() {
        return type == LayerType.RELAY;
    }

    public boolean isDeliver() {
        return type == LayerType.DELIVER;
    }
}
