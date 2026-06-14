package MessagePackage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayerCodecTest {

    @Test
    void deliveryLayerShouldSurviveEncodeDecodeRoundTrip() throws IOException {
        byte[] message = "Hello onion routing".getBytes();
        DecryptedLayer original = DecryptedLayer.Deliver(message);

        byte[] encoded = LayerCodec.encode(original);
        DecryptedLayer decoded = LayerCodec.decode(encoded);

        assertTrue(decoded.isDeliver());
        assertArrayEquals(message, decoded.getFinalMessage());
    }

    @Test
    void relayLayerShouldSurviveEncodeDecodeRoundTrip() throws IOException {
        OnionPacket innerPacket = createInnerPacket();
        DecryptedLayer original =
                DecryptedLayer.relay("node-2", innerPacket);

        byte[] encoded = LayerCodec.encode(original);
        DecryptedLayer decoded = LayerCodec.decode(encoded);

        assertTrue(decoded.isRelay());
        assertEquals("node-2", decoded.getNextNodeID());
        assertEquals(innerPacket, decoded.getInnerPacket());
    }

    @Test
    void encodeShouldRejectNullLayer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LayerCodec.encode(null)
        );
    }

    @Test
    void decodeShouldRejectNullAndEmptyInput() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LayerCodec.decode(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> LayerCodec.decode(new byte[0])
        );
    }

    @Test
    void decodeShouldRejectUnknownLayerType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LayerCodec.decode(new byte[]{99})
        );
    }

    @Test
    void decodeShouldRejectUnexpectedExtraBytes() {
        byte[] encoded = LayerCodec.encode(
                DecryptedLayer.Deliver("Hello".getBytes())
        );
        byte[] withExtraByte =
                Arrays.copyOf(encoded, encoded.length + 1);
        withExtraByte[withExtraByte.length - 1] = 99;

        assertThrows(
                IllegalArgumentException.class,
                () -> LayerCodec.decode(withExtraByte)
        );
    }

    @Test
    void decodeShouldRejectTruncatedData() {
        byte[] encoded = LayerCodec.encode(
                DecryptedLayer.Deliver("Hello".getBytes())
        );
        byte[] truncated =
                Arrays.copyOf(encoded, encoded.length - 1);

        assertThrows(
                IOException.class,
                () -> LayerCodec.decode(truncated)
        );
    }

    private OnionPacket createInnerPacket() {
        return new OnionPacket(
                new byte[]{1, 2, 3},
                new byte[12],
                new byte[]{4, 5, 6}
        );
    }
}
