package MessagePackage;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnionPacketDto {
    private final String encryptedAesKey;
    private final String iv;
    private final String encryptedPayload;


    public OnionPacketDto(String encryptedAesKey, String iv, String encryptedPayload) {

        if (encryptedAesKey == null || encryptedAesKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Encrypted AES key cannot be null or blank"
            );
        }

        if (iv == null || iv.isBlank()) {
            throw new IllegalArgumentException(
                    "IV cannot be null or blank"
            );
        }

        if (encryptedPayload == null || encryptedPayload.isBlank()) {
            throw new IllegalArgumentException(
                    "Encrypted payload cannot be null or blank"
            );
        }

        this.encryptedAesKey = encryptedAesKey;
        this.iv = iv;
        this.encryptedPayload = encryptedPayload;
    }

    public static OnionPacketDto fromPacket(OnionPacket packet) {
        if ( packet == null ) {
            throw new IllegalArgumentException("onion packet should not be a null");
        }

        String encryptedAesKey =
                Base64.getEncoder().encodeToString(
                        packet.getEncryptedAesKey()
                );

        String iv =
                Base64.getEncoder().encodeToString(
                        packet.getIv()
                );

        String encryptedPayload =
                Base64.getEncoder().encodeToString(
                        packet.getEncryptedPayload()
                );

        return new OnionPacketDto(
                encryptedAesKey,
                iv,
                encryptedPayload
        );
    }

    // this is where we restructure the message to actually take/send
    //ones for sending it so it gets decoded ( topacket )
    // the other one is taking it from there and encoding it ( frompacket )

    // this one actually turns them in to a JSOn-safe string
    // so when the data gets received, we got base64 string type -> byte[] -> onionpacket
    public OnionPacket toPacket() {
        byte[] encryptedAesKeyByes =
                Base64.getDecoder().decode(encryptedAesKey);

        byte[] ivBytes =
                Base64.getDecoder().decode(iv);

        byte[] encryptedPayloadBytes =
                Base64.getDecoder().decode(encryptedPayload);

        return new OnionPacket(
                encryptedAesKeyByes,
                ivBytes,
                encryptedPayloadBytes
        );
    }

    public String toJson() {
        return "{" +
                "\"encryptedAesKey\":\"" + encryptedAesKey + "\"," +
                "\"iv\":\"" + iv + "\"," +
                "\"encryptedPayload\":\"" + encryptedPayload + "\"" +
                "}";
    }

    public static OnionPacketDto fromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON cannot be null or blank");
        }

        return new OnionPacketDto(
                readJsonStringField(json, "encryptedAesKey"),
                readJsonStringField(json, "iv"),
                readJsonStringField(json, "encryptedPayload")
        );
    }

    private static String readJsonStringField(String json, String fieldName) {
        Pattern pattern = Pattern.compile(
                "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\""
        );
        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing JSON field: " + fieldName);
        }

        return matcher.group(1);
    }

    public String getEncryptedAesKey() {
        return encryptedAesKey;
    }

    public String getIv() {
        return iv;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

}
