package MessagePackage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OnionPacketDtoTest {

    @Test
    void fromPacketShouldCreateBase64Fields() {
        OnionPacket packet = createPacket();

        OnionPacketDto dto = OnionPacketDto.fromPacket(packet);

        assertNotNull(dto.getEncryptedAesKey());
        assertFalse(dto.getEncryptedAesKey().isBlank());
        assertNotNull(dto.getIv());
        assertFalse(dto.getIv().isBlank());
        assertNotNull(dto.getEncryptedPayload());
        assertFalse(dto.getEncryptedPayload().isBlank());
    }

    @Test
    void toPacketShouldReconstructOriginalPacket() {
        OnionPacket original = createPacket();
        OnionPacketDto dto = OnionPacketDto.fromPacket(original);

        OnionPacket reconstructed = dto.toPacket();

        assertEquals(original, reconstructed);
    }

    @Test
    void fromPacketShouldRejectNullPacket() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OnionPacketDto.fromPacket(null)
        );
    }

    @Test
    void constructorShouldRejectInvalidFields() {
        assertThrows(IllegalArgumentException.class, () -> new OnionPacketDto(null, "abc", "abc"));
        assertThrows(IllegalArgumentException.class, () -> new OnionPacketDto(" ", "abc", "abc"));
        assertThrows(IllegalArgumentException.class, () -> new OnionPacketDto("abc", null, "abc"));
        assertThrows(IllegalArgumentException.class, () -> new OnionPacketDto("abc", " ", "abc"));
        assertThrows(IllegalArgumentException.class, () -> new OnionPacketDto("abc", "abc", null));
        assertThrows(IllegalArgumentException.class, () -> new OnionPacketDto("abc", "abc", " "));
    }

    @Test
    void toPacketShouldRejectInvalidBase64() {
        OnionPacketDto dto = new OnionPacketDto(
                "not base64!",
                "also bad!",
                "bad too!"
        );

        assertThrows(
                IllegalArgumentException.class,
                dto::toPacket
        );
    }

    @Test
    void toJsonShouldWriteAllFields() {
        OnionPacketDto dto = OnionPacketDto.fromPacket(createPacket());

        String json = dto.toJson();
        OnionPacketDto decoded = OnionPacketDto.fromJson(json);

        assertFalse(json.isBlank());
        assertEquals(dto.getEncryptedAesKey(), decoded.getEncryptedAesKey());
        assertEquals(dto.getIv(), decoded.getIv());
        assertEquals(dto.getEncryptedPayload(), decoded.getEncryptedPayload());
    }

    @Test
    void fromJsonShouldReconstructPacket() {
        OnionPacket original = createPacket();
        String json = OnionPacketDto.fromPacket(original).toJson();

        OnionPacket reconstructed = OnionPacketDto
                .fromJson(json)
                .toPacket();

        assertEquals(original, reconstructed);
    }

    @Test
    void fromJsonShouldRejectInvalidJson() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OnionPacketDto.fromJson(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> OnionPacketDto.fromJson(" ")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> OnionPacketDto.fromJson("{\"encryptedAesKey\":\"abc\"}")
        );
    }

    private OnionPacket createPacket() {
        return new OnionPacket(
                new byte[]{1, 2, 3},
                new byte[12],
                new byte[]{4, 5, 6}
        );
    }
}
